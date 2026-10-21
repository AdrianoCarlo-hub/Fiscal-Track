package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.acompte.AcomptesObligatoiresService;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.domain.model.DeficitReportable;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.domain.model.SaisieDeclaration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validation et construction des parametres de calcul d'une declaration,
 * selon le code impot de l'obligation.
 *
 * Aucune regle fiscale n'est reimplementee ici : la classe se contente de
 *   1. verifier la presence et la validite des champs obligatoires du type
 *      (messages en francais),
 *   2. construire la map de parametres attendue par
 *      {@code CalculerFiscalDispatcherUseCase} (inchange),
 *   3. calculer le chiffre d'affaires total a enregistrer avec la declaration
 *      (penalites et amende de defaut de depot).
 *
 * L'impot lui-meme reste calcule exclusivement par les calculateurs existants.
 */
@Service
public class PreparerDonneesDeclarationUseCase {

    /** Types d'impots pour lesquels un formulaire de declaration existe. */
    public static final Set<String> TYPES_FORMULAIRE = Set.of("IS", "TVA", "IR", "IRSA", "ACOMPTE_IR", "ACOMPTE_IS");

    public static final int MAX_SALARIES = 50;

    private final ContribuableRepositoryPort contribuableRepositoryPort;
    private final ParametreFiscalService parametreFiscalService;
    private final AcomptesObligatoiresService acomptesObligatoiresService;

    /** Constructeur historique (tests et anciens appels, sans liquidation serveur). */
    public PreparerDonneesDeclarationUseCase(ContribuableRepositoryPort contribuableRepositoryPort,
                                             ParametreFiscalService parametreFiscalService) {
        this(contribuableRepositoryPort, parametreFiscalService, null);
    }

    @Autowired
    public PreparerDonneesDeclarationUseCase(ContribuableRepositoryPort contribuableRepositoryPort,
                                             ParametreFiscalService parametreFiscalService,
                                             AcomptesObligatoiresService acomptesObligatoiresService) {
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.parametreFiscalService = parametreFiscalService;
        this.acomptesObligatoiresService = acomptesObligatoiresService;
    }

    /**
     * Donnees pretes a etre transmises au moteur de calcul.
     *
     * @param params          parametres du dispatcher (le moteur reste inchange)
     * @param chiffreAffaires chiffre d'affaires total a enregistrer
     * @param donneesSaisies  copie lisible des champs saisis (tracabilite)
     */
    public record DonneesDeclaration(Map<String, Object> params,
                                     BigDecimal chiffreAffaires,
                                     Map<String, Object> donneesSaisies,
                                     Map<String, Object> detailAcomptes,
                                     BigDecimal creditAnterieur) {
        public DonneesDeclaration(Map<String, Object> params,
                                  BigDecimal chiffreAffaires,
                                  Map<String, Object> donneesSaisies) {
            this(params, chiffreAffaires, donneesSaisies, Map.of(), BigDecimal.ZERO);
        }
    }

    /**
     * Valide la saisie pour le code impot donne et construit les parametres.
     *
     * @param nif NIF du dossier concerne (lecture de la fiche : autorisation
     *            d'ouverture, option sante, categorie d'activite)
     * @throws CalculFiscalInvalideException avec un message en francais si un
     *         champ obligatoire manque ou si une valeur est invalide
     */
    public DonneesDeclaration execute(SaisieDeclaration saisie, String codeImpot, String nif) {
        return executeInterne(saisie, codeImpot, nif, null);
    }

    /**
     * Variante utilisee par l'apercu et le depot : l'obligation permet de
     * connaitre l'exercice et de liquider les acomptes reellement payes.
     */
    public DonneesDeclaration execute(SaisieDeclaration saisie, ObligationFiscale obligation) {
        if (obligation == null) {
            return execute(saisie, null, null);
        }
        return executeInterne(saisie, obligation.getCodeImpot(), obligation.getNif(), obligation);
    }

    private DonneesDeclaration executeInterne(SaisieDeclaration saisie, String codeImpot,
                                              String nif, ObligationFiscale obligation) {
        if (codeImpot == null || codeImpot.isBlank()) {
            throw new CalculFiscalInvalideException("Type d'impot inconnu pour cette obligation");
        }
        String type = codeImpot.toUpperCase();
        if (!TYPES_FORMULAIRE.contains(type)) {
            throw new CalculFiscalInvalideException(
                    "Formulaire de declaration non disponible pour le type d'impot " + type
                            + " : aucun champ declare n'est pris en charge. Contactez votre agent fiscal.");
        }

        Contribuable contribuable = contribuableRepositoryPort.findByNif(nif).orElse(null);

        return switch (type) {
            case "IS" -> preparerIs(saisie, contribuable, obligation);
            case "TVA" -> preparerTva(saisie);
            case "IR" -> preparerIr(saisie, contribuable, obligation);
            case "IRSA" -> preparerIrsa(saisie);
            default -> preparerAcompte(obligation);
        };
    }

    // ------------------------------------------------------------------
    // IS
    // ------------------------------------------------------------------

    private DonneesDeclaration preparerIs(SaisieDeclaration s, Contribuable contribuable,
                                          ObligationFiscale obligation) {
        BigDecimal caBrut = premierNonNul(s.getCaBrut(), s.getChiffreAffairesDeclare());
        exigerPresent(caBrut, "Le chiffre d'affaires brut encaisse (Ar)");
        exigerPositif(caBrut, "Le chiffre d'affaires brut encaisse (Ar)");

        String categorie = s.getCategorieActiviteIs();
        if (estVide(categorie) && contribuable != null) {
            categorie = contribuable.getCategorieActivite();
        }

        exigerPositif(s.getAcomptesDouane(), "Les acomptes de douane (Ar)");
        exigerPositif(s.getAcomptesMarchesPublics(), "Les acomptes marches publics (Ar)");

        Map<String, Object> params = new HashMap<>();
        params.put("caBrut", caBrut);
        params.put("adherentCga", Boolean.TRUE.equals(s.getAdherentCga()));
        params.put("achatsConformes", nvl(s.getAchatsConformes()));
        params.put("chargesSalariales", nvl(s.getChargesSalariales()));
        params.put("categorieActivite", categorie);

        Map<String, Object> saisi = new LinkedHashMap<>();
        saisi.put("caBrut", caBrut);
        saisi.put("adherentCga", Boolean.TRUE.equals(s.getAdherentCga()));
        saisi.put("achatsConformes", nvl(s.getAchatsConformes()));
        saisi.put("chargesSalariales", nvl(s.getChargesSalariales()));
        saisi.put("categorieActiviteIs", categorie);

        AcomptesObligatoiresService.Liquidation liquidation =
                appliquerLiquidation("IS", s, obligation, params, saisi);
        if (liquidation == null) {
            params.put("acomptes", nvl(s.getAcomptes()));
            saisi.put("acomptes", nvl(s.getAcomptes()));
        }

        return new DonneesDeclaration(params, caBrut, saisi,
                liquidation == null ? Map.of() : liquidation.detail(),
                liquidation == null ? BigDecimal.ZERO : liquidation.creditAnterieur());
    }

    // ------------------------------------------------------------------
    // TVA
    // ------------------------------------------------------------------

    /**
     * La TVA collectee est calculee par le serveur a partir du chiffre
     * d'affaires LOCAUX (taux normal, ou taux reduit gaz butane) : les
     * exportations sont taxees a 0 % et ne produisent donc aucune TVA
     * collectee. Le calculateur existant recoit la TVA collectee, la TVA
     * deductible (biens + immobilisations) et le credit anterieur.
     */
    private DonneesDeclaration preparerTva(SaisieDeclaration s) {
        BigDecimal caLocal = premierNonNul(s.getCaLocal(), s.getChiffreAffairesDeclare());
        BigDecimal caExport = s.getCaExportation() == null ? BigDecimal.ZERO : s.getCaExportation();
        exigerPresent(caLocal, "Le chiffre d'affaires imposable localement (Ar)");
        exigerPositif(caLocal, "Le chiffre d'affaires imposable localement (Ar)");
        exigerPositif(caExport, "Le chiffre d'affaires a l'exportation (Ar)");
        exigerPositif(s.getTvaDeductibleBiens(), "La TVA deductible sur les biens et services (Ar)");
        exigerPositif(s.getTvaDeductibleImmobilisations(), "La TVA deductible sur les immobilisations (Ar)");
        exigerPositif(s.getCreditTvaAnterieur(), "Le credit de TVA anterieur (Ar)");

        boolean gazButane = Boolean.TRUE.equals(s.getGazButane());
        boolean uniquementExport = caLocal.signum() == 0 && caExport.signum() > 0;

        BigDecimal taux = uniquementExport
                ? parametreFiscalService.getValeur("TVA_TAUX_EXPORT")
                : (gazButane
                    ? parametreFiscalService.getValeur("TVA_TAUX_GAZ_BUTANE")
                    : parametreFiscalService.getValeur("TVA_TAUX_NORMAL"));

        BigDecimal tvaCollectee = caLocal.multiply(taux).setScale(2, RoundingMode.HALF_UP);
        BigDecimal biens = nvl(s.getTvaDeductibleBiens());
        BigDecimal immobilisations = nvl(s.getTvaDeductibleImmobilisations());
        BigDecimal creditAnterieur = nvl(s.getCreditTvaAnterieur());
        BigDecimal tvaDeductible = biens.add(immobilisations);

        Map<String, Object> params = new HashMap<>();
        params.put("tvaCollectee", tvaCollectee);
        params.put("tvaDeductible", tvaDeductible);
        params.put("creditTvaAnterieur", creditAnterieur);
        params.put("exportation", uniquementExport);
        params.put("gazButane", gazButane);
        params.put("chiffreAffaires", caLocal.add(caExport));

        Map<String, Object> saisi = new LinkedHashMap<>();
        saisi.put("caLocal", caLocal);
        saisi.put("caExportation", caExport);
        saisi.put("gazButane", gazButane);
        saisi.put("tvaCollectee", tvaCollectee);
        saisi.put("tvaDeductibleBiens", biens);
        saisi.put("tvaDeductibleImmobilisations", immobilisations);
        saisi.put("creditTvaAnterieur", creditAnterieur);

        return new DonneesDeclaration(params, caLocal.add(caExport), saisi);
    }

    // ------------------------------------------------------------------
    // IR
    // ------------------------------------------------------------------

    private DonneesDeclaration preparerIr(SaisieDeclaration s, Contribuable contribuable,
                                          ObligationFiscale obligation) {
        BigDecimal caHT = premierNonNul(s.getCaHT(), s.getChiffreAffairesDeclare());
        exigerPresent(caHT, "Le chiffre d'affaires HT (Ar)");
        exigerPositif(caHT, "Le chiffre d'affaires HT (Ar)");
        exigerPositif(s.getChargesDeductibles(), "Les charges deductibles (Ar)");
        exigerPositif(s.getReintegrations(), "Les reintegrations extra-comptables (Ar)");
        exigerPositif(s.getDeductions(), "Les deductions extra-comptables (Ar)");
        exigerPositif(s.getAcomptes(), "Les acomptes verses (Ar)");
        exigerPositif(s.getAcomptesDouane(), "Les acomptes de douane (Ar)");
        exigerPositif(s.getAcomptesMarchesPublics(), "Les acomptes marches publics (Ar)");

        // L'autorisation d'ouverture et l'option sante ne se saisissent PAS
        // dans la declaration : elles sont lues dans la fiche du contribuable.
        boolean autorisationOuverture = contribuable != null
                && Boolean.TRUE.equals(contribuable.getAutorisationOuverture());
        boolean optionSante = contribuable != null
                && Boolean.TRUE.equals(contribuable.getOptionSante());

        String categorie = s.getCategorieActivite();
        if (estVide(categorie) && contribuable != null) {
            categorie = contribuable.getCategorieActivite();
        }

        int moisActifs = s.getMoisActifs() == null ? 12 : s.getMoisActifs();
        if (moisActifs < 1 || moisActifs > 12) {
            throw new CalculFiscalInvalideException(
                    "Le mois d'activite dans l'exercice doit etre compris entre 1 et 12");
        }

        List<Map<String, Object>> deficits = new ArrayList<>();
        if (s.getDeficits() != null) {
            for (DeficitReportable d : s.getDeficits()) {
                if (d == null) continue;
                exigerPositif(d.montant(), "Le montant du deficit reportable (Ar)");
                Map<String, Object> ligne = new LinkedHashMap<>();
                ligne.put("anneeOrigine", d.anneeOrigine());
                ligne.put("montant", d.montant());
                deficits.add(ligne);
            }
        }

        Map<String, Object> params = new HashMap<>();
        params.put("caHT", caHT);
        params.put("chargesDeductibles", nvl(s.getChargesDeductibles()));
        params.put("reintegrations", nvl(s.getReintegrations()));
        params.put("deductions", nvl(s.getDeductions()));
        params.put("deficitsReportables", deficits);
        params.put("categorieActivite", categorie);
        params.put("moisActifs", moisActifs);
        params.put("autorisationOuverture", autorisationOuverture);
        params.put("optionSante", optionSante);

        Map<String, Object> saisi = new LinkedHashMap<>();
        saisi.put("caHT", caHT);
        saisi.put("chargesDeductibles", nvl(s.getChargesDeductibles()));
        saisi.put("reintegrations", nvl(s.getReintegrations()));
        saisi.put("deductions", nvl(s.getDeductions()));
        saisi.put("deficitsReportables", deficits);
        saisi.put("categorieActivite", categorie);
        saisi.put("moisActifs", moisActifs);
        Map<String, Object> dossier = new LinkedHashMap<>();
        dossier.put("autorisationOuverture", autorisationOuverture);
        dossier.put("optionSante", optionSante);
        saisi.put("sourceDossier", dossier);

        AcomptesObligatoiresService.Liquidation liquidation =
                appliquerLiquidation("IR", s, obligation, params, saisi);
        if (liquidation == null) {
            params.put("acomptes", nvl(s.getAcomptes()));
            saisi.put("acomptes", nvl(s.getAcomptes()));
        }

        return new DonneesDeclaration(params, caHT, saisi,
                liquidation == null ? Map.of() : liquidation.detail(),
                liquidation == null ? BigDecimal.ZERO : liquidation.creditAnterieur());
    }

    // ------------------------------------------------------------------
    // IRSA
    // ------------------------------------------------------------------

    /**
     * Chaque ligne salarie est calculee par le calculateur IRSA existant ;
     * l'impot principal de la declaration est le total des lignes.
     *
     * Les trois avantages en nature (logement, vehicule, telephone) sont
     * transmis un a un aux trois parametres correspondants du calculateur.
     */
    private DonneesDeclaration preparerIrsa(SaisieDeclaration s) {
        List<SaisieDeclaration.LigneSalarie> lignes = s.getSalaries();
        if (lignes == null || lignes.isEmpty()) {
            throw new CalculFiscalInvalideException(
                    "Au moins un salarie est obligatoire pour declarer l'IRSA");
        }
        if (lignes.size() > MAX_SALARIES) {
            throw new CalculFiscalInvalideException(
                    "Au plus " + MAX_SALARIES + " salaries par declaration IRSA");
        }

        List<Map<String, Object>> salaries = new ArrayList<>();
        BigDecimal totalBruts = BigDecimal.ZERO;
        for (int i = 0; i < lignes.size(); i++) {
            SaisieDeclaration.LigneSalarie ligne = lignes.get(i);
            if (ligne == null || ligne.salaireBrut() == null) {
                throw new CalculFiscalInvalideException(
                        "Salaire brut mensuel obligatoire pour la ligne " + (i + 1));
            }
            exigerPositif(ligne.salaireBrut(), "Le salaire brut mensuel de la ligne " + (i + 1) + " (Ar)");
            exigerPositif(ligne.cotisationsSociales(), "Les cotisations sociales de la ligne " + (i + 1) + " (Ar)");
            exigerPositif(ligne.avantageLogement(), "L'avantage logement de la ligne " + (i + 1) + " (Ar)");
            exigerPositif(ligne.avantageVehicule(), "L'avantage vehicule de la ligne " + (i + 1) + " (Ar)");
            exigerPositif(ligne.avantageTelephone(), "L'avantage telephone de la ligne " + (i + 1) + " (Ar)");
            int personnes = ligne.personnesCharge() == null ? 0 : ligne.personnesCharge();
            if (personnes < 0) {
                throw new CalculFiscalInvalideException(
                        "Le nombre de personnes a charge de la ligne " + (i + 1)
                                + " ne peut pas etre negatif");
            }

            Map<String, Object> paramsLigne = new HashMap<>();
            paramsLigne.put("salaireBrut", ligne.salaireBrut());
            paramsLigne.put("cotisationsSociales", nvl(ligne.cotisationsSociales()));
            paramsLigne.put("avantageLogement", nvl(ligne.avantageLogement()));
            paramsLigne.put("avantageVehicule", nvl(ligne.avantageVehicule()));
            paramsLigne.put("avantageTelephone", nvl(ligne.avantageTelephone()));
            paramsLigne.put("nombrePersonnesCharge", personnes);

            Map<String, Object> saisi = new LinkedHashMap<>();
            saisi.put("nom", ligne.nom() == null || ligne.nom().isBlank()
                    ? "Salarie " + (i + 1) : ligne.nom().trim());
            saisi.put("salaireBrut", ligne.salaireBrut());
            saisi.put("cotisationsSociales", nvl(ligne.cotisationsSociales()));
            saisi.put("avantageLogement", nvl(ligne.avantageLogement()));
            saisi.put("avantageVehicule", nvl(ligne.avantageVehicule()));
            saisi.put("avantageTelephone", nvl(ligne.avantageTelephone()));
            saisi.put("personnesCharge", personnes);
            saisi.put("parametres", paramsLigne);

            salaries.add(saisi);
            totalBruts = totalBruts.add(ligne.salaireBrut());
        }

        Map<String, Object> params = new HashMap<>();
        params.put("salaries", salaries);

        Map<String, Object> saisi = new LinkedHashMap<>();
        saisi.put("salaries", salaries);
        saisi.put("totalSalairesBruts", totalBruts);

        return new DonneesDeclaration(params, totalBruts, saisi);
    }

    // ------------------------------------------------------------------
    // Acompte provisionnel IR
    // ------------------------------------------------------------------

    /**
     * Acompte : aucun montant libre. Avec une obligation, le serveur calcule
     * le montant depuis la declaration validee anterieure (ou le forfait de
     * debut d'activite) et trace l'exercice de base.
     */
    private DonneesDeclaration preparerAcompte(ObligationFiscale obligation) {
        if (obligation == null || acomptesObligatoiresService == null) {
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> saisi = new LinkedHashMap<>();
            saisi.put("source", "Derniere declaration IR du dossier (IR N-1)");
            return new DonneesDeclaration(params, BigDecimal.ZERO, saisi);
        }

        AcomptesObligatoiresService.CalculAcompte calcul =
                acomptesObligatoiresService.calculerPourObligation(obligation);
        if (!calcul.disponible()) {
            throw new CalculFiscalInvalideException(calcul.message());
        }

        Map<String, Object> params = new HashMap<>();
        params.put("acompteMontant", calcul.montant());
        params.put("baseAcompte", calcul.base());
        params.put("tauxAcompte", calcul.taux());
        params.put("exerciceBase", calcul.exerciceBase());
        params.put("codeImpot", calcul.codeImpot());

        Map<String, Object> saisi = new LinkedHashMap<>();
        saisi.put("source", "Calcul serveur : " + calcul.codeImpot());
        saisi.put("montant", calcul.montant());
        saisi.put("base", calcul.base());
        saisi.put("exerciceBase", calcul.exerciceBase());
        saisi.put("taux", calcul.taux());
        saisi.put("detailAcomptes", calcul.detail());
        saisi.put("avertissements", calcul.avertissements());

        return new DonneesDeclaration(params, BigDecimal.ZERO, saisi,
                calcul.detail(), BigDecimal.ZERO);
    }

    /**
     * Liquide les acomptes reellement payes, les autres acomptes saisis et le
     * credit anterieur. Retourne null si le service n'est pas disponible
     * (ancien constructeur, tests historiques).
     */
    private AcomptesObligatoiresService.Liquidation appliquerLiquidation(
            String codeImpotParent, SaisieDeclaration s, ObligationFiscale obligation,
            Map<String, Object> params, Map<String, Object> saisi) {
        if (acomptesObligatoiresService == null || obligation == null) {
            return null;
        }
        int exercice = exerciceDe(obligation.getPeriodeFiscale());
        if (exercice <= 0) {
            return null;
        }
        AcomptesObligatoiresService.Liquidation liquidation =
                acomptesObligatoiresService.liquider(
                        obligation.getNif(), codeImpotParent, exercice,
                        s.getAcomptesDouane(), s.getAcomptesMarchesPublics());

        params.put("acomptes", liquidation.totalDeduit());
        saisi.put("acomptes", liquidation.totalDeduit());
        saisi.put("acomptesPayes", liquidation.acomptesPayes());
        saisi.put("acomptesDouane", nvl(s.getAcomptesDouane()));
        saisi.put("acomptesMarchesPublics", nvl(s.getAcomptesMarchesPublics()));
        saisi.put("creditAnterieur", liquidation.creditAnterieur());
        saisi.put("detailAcomptes", liquidation.detail());
        return liquidation;
    }

    private static int exerciceDe(String periode) {
        if (periode == null || periode.length() < 4) {
            return 0;
        }
        try {
            return Integer.parseInt(periode.substring(0, 4));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void exigerPresent(BigDecimal valeur, String libelle) {
        if (valeur == null) {
            throw new CalculFiscalInvalideException(libelle + " est obligatoire");
        }
    }

    private static void exigerPositif(BigDecimal valeur, String libelle) {
        if (valeur != null && valeur.signum() < 0) {
            throw new CalculFiscalInvalideException(libelle + " ne peut pas etre negatif");
        }
    }

    private static BigDecimal premierNonNul(BigDecimal prioritaire, BigDecimal secondaire) {
        return prioritaire != null ? prioritaire : secondaire;
    }

    private static BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }

    private static boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}

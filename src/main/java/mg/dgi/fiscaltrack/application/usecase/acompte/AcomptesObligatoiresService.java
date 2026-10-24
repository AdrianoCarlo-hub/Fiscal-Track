package mg.dgi.fiscaltrack.application.usecase.acompte;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Regles communes des acomptes obligatoires IR et IS.
 *
 * Base de calcul : derniere declaration VALIDEE de l'impot dont l'exercice est
 * strictement anterieur a l'exercice des acomptes (R1 corrigee). Le montant
 * n'est jamais repris du formulaire : il est recalcule a chaque ouverture et
 * au depot, le serveur faisant foi.
 */
@Service
public class AcomptesObligatoiresService {

    private static final Set<String> CATEGORIES_IS_16K = Set.of("AGRICOLE", "ELEVEUR");
    private static final Set<String> CATEGORIES_IS_50K = Set.of(
            "ARTISAN", "ARTISANAL", "GARGOTIER", "PETIT_COMMERCANT");
    private static final Set<String> CATEGORIES_IS_100K = Set.of(
            "COMMERCANT", "COMMERCE", "HOTELIER", "PRESTATAIRE");
    private static final Set<String> CATEGORIES_IS_150K = Set.of(
            "PROFESSION_LIBERALE", "ACTIVITES_MULTIPLES");

    private static final Set<String> CATEGORIES_IR_500K = Set.of(
            "INDUSTRIE", "ARTISANAT", "AGRICULTURE", "ELEVAGE", "TOURISME",
            "MINES", "HOTELLERIE");

    private static final Set<String> CATEGORIES_IR_AUTRES = Set.of(
            "COMMERCE", "SERVICES", "AUTRES");

    private final DeclarationRepositoryPort declarationRepositoryPort;
    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private final ContribuableRepositoryPort contribuableRepositoryPort;
    private final ParametreFiscalService parametreFiscalService;

    public AcomptesObligatoiresService(DeclarationRepositoryPort declarationRepositoryPort,
                                       ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                       CompteCourantFiscalRepositoryPort compteRepositoryPort,
                                       ContribuableRepositoryPort contribuableRepositoryPort,
                                       ParametreFiscalService parametreFiscalService) {
        this.declarationRepositoryPort = declarationRepositoryPort;
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.compteRepositoryPort = compteRepositoryPort;
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.parametreFiscalService = parametreFiscalService;
    }

    /** Reference validee utilisee comme base d'un acompte. */
    public record Reference(int exercice, BigDecimal impotRetenu, String codeImpot) {
    }

    /** Resultat d'un calcul d'acompte (periodique ou debut d'activite). */
    public record CalculAcompte(boolean disponible,
                                BigDecimal montant,
                                BigDecimal base,
                                BigDecimal taux,
                                Integer exerciceBase,
                                String codeImpot,
                                String message,
                                List<String> avertissements,
                                Map<String, Object> detail) {
    }

    /** Cumul deductile pour la liquidation d'un impot. */
    public record Liquidation(BigDecimal acomptesPayes,
                              BigDecimal autresAcomptes,
                              BigDecimal creditAnterieur,
                              BigDecimal totalDeduit,
                              List<String> avertissements,
                              Map<String, Object> detail) {
    }

    public Optional<Reference> trouverReference(String nif, String codeImpotParent, int exercice) {
        return declarationRepositoryPort
                .findDerniereDeclarationValideeAvantExercice(nif, codeImpotParent, exercice)
                .flatMap(declaration -> obligationRepositoryPort
                        .findById(declaration.getIdObligationFiscale())
                        .map(obligation -> new Reference(
                                exerciceDe(obligation.getPeriodeFiscale()),
                                valeurOuZero(declaration.getImpotPrincipalCalcule()),
                                codeImpotParent)));
    }

    public CalculAcompte calculerPeriodique(String nif, String codeImpotParent, int exercice) {
        String code = codeImpotParent.toUpperCase();
        if (!"IR".equals(code) && !"IS".equals(code)) {
            throw new CalculFiscalInvalideException(
                    "Aucun acompte periodique n'est prevu pour l'impot " + codeImpotParent);
        }

        Optional<Reference> reference = trouverReference(nif, code, exercice);
        if (reference.isEmpty()) {
            return indisponible(code, null, null,
                    "Aucune declaration " + code + " validee anterieure a l'exercice "
                            + exercice + " : aucun acompte periodique.");
        }

        Reference ref = reference.get();
        BigDecimal taux = parametreFiscalService.getValeur(
                "ACOMPTE_" + code + "_TAUX_PAR_ACOMPTE",
                parametreFiscalService.getValeur("ACOMPTE_TAUX_PROVISIONNEL", new BigDecimal("0.50"))
                        .divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP));
        BigDecimal montant = ref.impotRetenu().multiply(taux).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> detail = detailBase(code, ref, montant, taux, false);
        return new CalculAcompte(true, montant, ref.impotRetenu(), taux, ref.exercice(),
                code, null, List.of(), detail);
    }

    public CalculAcompte calculerDebutActivite(String codeImpotParent, String categorie) {
        String code = codeImpotParent == null ? "" : codeImpotParent.toUpperCase();
        String cat = normaliser(categorie);
        List<String> avertissements = new ArrayList<>();

        BigDecimal montant;
        String libelle;
        boolean inconnue = false;

        if ("IR".equals(code)) {
            if (CATEGORIES_IR_500K.contains(cat) || "AGRICOLE".equals(cat)) {
                montant = parametreFiscalService.getValeur("ACOMPTE_IR_INDUSTRIE_ETC");
                libelle = "IR - industrie, artisanat, agriculture, elevage, tourisme, mines, hotellerie";
            } else if ("EXPORTATEUR_AGRICOLE".equals(cat)) {
                montant = parametreFiscalService.getValeur("ACOMPTE_IR_EXPORTATEUR_AGRICOLE");
                libelle = "IR - exportateur agricole";
            } else if ("EDUCATION".equals(cat) || "SANTE".equals(cat)) {
                montant = parametreFiscalService.getValeur("ACOMPTE_IR_EDUCATION_SANTE");
                libelle = "IR - education, sante";
            } else {
                montant = parametreFiscalService.getValeur("ACOMPTE_IR_AUTRES");
                libelle = "IR - autres categories";
                if (!CATEGORIES_IR_AUTRES.contains(cat)) {
                    inconnue = true;
                    avertissements.add("Catégorie d'activité inconnue : forfait « autres » "
                            + "appliqué (1 000 000 Ar). Vérifiez la fiche du contribuable.");
                }
            }
        } else if ("IS".equals(code)) {
            if (CATEGORIES_IS_16K.contains(cat)) {
                montant = parametreFiscalService.getValeur("ACOMPTE_IS_AGRICOLE_ELEVEUR");
                libelle = "IS - agricole, eleveur";
            } else if (CATEGORIES_IS_50K.contains(cat)) {
                montant = parametreFiscalService.getValeur("ACOMPTE_IS_ARTISAN");
                libelle = "IS - artisan, gargotier, petit commercant";
            } else if (CATEGORIES_IS_100K.contains(cat)) {
                montant = parametreFiscalService.getValeur("ACOMPTE_IS_COMMERCANT");
                libelle = "IS - commercant, hotelier, prestataire";
            } else if (CATEGORIES_IS_150K.contains(cat) || cat.isEmpty()) {
                montant = parametreFiscalService.getValeur("ACOMPTE_IS_PROFESSION_LIBERALE");
                libelle = "IS - profession liberale, activites multiples, autres";
                if (cat.isEmpty()) {
                    inconnue = true;
                    avertissements.add("Catégorie d'activité inconnue : forfait le plus élevé "
                            + "appliqué (150 000 Ar, catégorie « autres »). "
                            + "Vérifiez la fiche du contribuable.");
                }
            } else {
                montant = parametreFiscalService.getValeur("ACOMPTE_IS_PROFESSION_LIBERALE");
                libelle = "IS - autres categories";
                inconnue = true;
                avertissements.add("Catégorie d'activité inconnue : forfait le plus élevé "
                        + "appliqué (150 000 Ar, catégorie « autres »). "
                        + "Vérifiez la fiche du contribuable.");
            }
        } else {
            throw new CalculFiscalInvalideException(
                    "Acompte de debut d'activite non prevu pour l'impot " + codeImpotParent);
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("codeImpot", code);
        detail.put("type", "DEBUT_ACTIVITE");
        detail.put("categorie", categorie);
        detail.put("libelle", libelle);
        detail.put("montant", montant);
        detail.put("base", null);
        detail.put("exerciceBase", null);
        detail.put("taux", null);
        detail.put("categorieInconnue", inconnue);
        detail.put("avertissements", avertissements);

        return new CalculAcompte(true, montant, null, null, null, code,
                null, avertissements, detail);
    }

    public CalculAcompte calculerPourObligation(ObligationFiscale obligation) {
        String code = obligation.getCodeImpot().toUpperCase();
        int exercice = exerciceDe(obligation.getPeriodeFiscale());
        if ("ACOMPTE_IR".equals(code) || "ACOMPTE_IS".equals(code)) {
            String parent = code.substring("ACOMPTE_".length());
            if (obligation.getPeriodeFiscale() != null
                    && obligation.getPeriodeFiscale().toUpperCase().contains("DEBUT")) {
                String categorie = contribuableRepositoryPort.findByNif(obligation.getNif())
                        .map(Contribuable::getCategorieActivite)
                        .orElse(null);
                return calculerDebutActivite(parent, categorie);
            }
            return calculerPeriodique(obligation.getNif(), parent, exercice);
        }
        throw new CalculFiscalInvalideException(
                "Cette obligation n'est pas un acompte : " + obligation.getCodeImpot());
    }

    public Liquidation liquider(String nif, String codeImpotParent, int exercice,
                                BigDecimal douane, BigDecimal marchesPublics) {
        String parent = codeImpotParent.toUpperCase();
        String codeAcompte = "ACOMPTE_" + parent;
        BigDecimal autres = valeurOuZero(douane).add(valeurOuZero(marchesPublics));

        BigDecimal acomptesPayes = compteRepositoryPort
                .sommeMontantPayeAcomptes(nif, codeAcompte, exercice);

        List<String> avertissements = new ArrayList<>();
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (ObligationFiscale obligation : obligationRepositoryPort.findByNif(nif)) {
            if (!codeAcompte.equalsIgnoreCase(obligation.getCodeImpot())
                    || exerciceDe(obligation.getPeriodeFiscale()) != exercice) {
                continue;
            }
            Optional<Declaration> declaration = declarationRepositoryPort
                    .findByIdObligationFiscale(obligation.getIdObligationFiscale());
            BigDecimal du = declaration.map(Declaration::getImpotPrincipalCalcule)
                    .orElse(obligation.getMontantAcompte());
            BigDecimal paye = declaration.flatMap(d -> compteRepositoryPort
                            .findByIdDeclaration(d.getIdDeclaration()))
                    .map(CompteCourantFiscal::getMontantPaye)
                    .orElse(BigDecimal.ZERO);

            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("periode", obligation.getPeriodeFiscale());
            ligne.put("statut", obligation.getStatutDeclaration() == null
                    ? null : obligation.getStatutDeclaration().name());
            ligne.put("montantDu", du);
            ligne.put("montantPaye", paye);

            if (declaration.isEmpty()) {
                avertissements.add("Acompte " + obligation.getPeriodeFiscale()
                        + " non déposé : aucun montant déduit.");
            } else if (valeurOuZero(paye).compareTo(valeurOuZero(du)) < 0) {
                avertissements.add("Acompte " + obligation.getPeriodeFiscale()
                        + " partiellement payé : seul le montant payé ("
                        + valeurOuZero(paye) + " Ar) est déduit.");
            }
            lignes.add(ligne);
        }

        BigDecimal creditAnterieur = declarationRepositoryPort
                .findDerniereDeclarationValideeAvantExercice(nif, parent, exercice)
                .map(Declaration::getCreditFiscal)
                .map(v -> valeurOuZero(v))
                .orElse(BigDecimal.ZERO);

        BigDecimal total = acomptesPayes.add(autres).add(creditAnterieur);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("codeImpot", parent);
        detail.put("exercice", exercice);
        detail.put("acomptesPayes", acomptesPayes);
        detail.put("autresAcomptes", autres);
        detail.put("acomptesDouane", valeurOuZero(douane));
        detail.put("acomptesMarchesPublics", valeurOuZero(marchesPublics));
        detail.put("creditAnterieur", creditAnterieur);
        detail.put("totalDeduit", total);
        detail.put("lignes", lignes);
        detail.put("avertissements", avertissements);

        return new Liquidation(acomptesPayes, autres, creditAnterieur, total,
                avertissements, detail);
    }

    /** Met a jour les colonnes d'apercu (jamais source de verite). */
    public void mettreAJourApercu(ObligationFiscale obligation, CalculAcompte calcul) {
        if (obligation == null || calcul == null || !calcul.disponible()) {
            return;
        }
        obligation.setMontantAcompte(calcul.montant());
        obligation.setBaseAcompte(calcul.base());
        obligation.setTauxAcompte(calcul.taux());
        obligation.setExerciceBaseAcompte(calcul.exerciceBase());
    }

    public Map<String, Object> detailLiquidation(Liquidation liquidation) {
        return liquidation == null ? Map.of() : liquidation.detail();
    }

    private CalculAcompte indisponible(String code, BigDecimal base, Integer exercice, String message) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("codeImpot", code);
        detail.put("type", "PERIODIQUE");
        detail.put("disponible", false);
        detail.put("message", message);
        detail.put("base", base);
        detail.put("exerciceBase", exercice);
        detail.put("avertissements", List.of(message));
        return new CalculAcompte(false, BigDecimal.ZERO, base, null, exercice,
                code, message, List.of(message), detail);
    }

    private Map<String, Object> detailBase(String code, Reference ref, BigDecimal montant,
                                           BigDecimal taux, boolean inconnue) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("codeImpot", code);
        detail.put("type", "PERIODIQUE");
        detail.put("disponible", true);
        detail.put("base", ref.impotRetenu());
        detail.put("baseLibelle", "Base : " + code + " " + ref.exercice()
                + " = " + ref.impotRetenu() + " Ar");
        detail.put("exerciceBase", ref.exercice());
        detail.put("taux", taux);
        detail.put("montant", montant);
        detail.put("categorieInconnue", inconnue);
        detail.put("avertissements", List.of());
        return detail;
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

    private static String normaliser(String categorie) {
        return categorie == null ? "" : categorie.trim().toUpperCase();
    }

    private static BigDecimal valeurOuZero(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}

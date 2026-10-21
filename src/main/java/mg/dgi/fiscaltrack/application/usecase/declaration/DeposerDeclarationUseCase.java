package mg.dgi.fiscaltrack.application.usecase.declaration;



import mg.dgi.fiscaltrack.application.port.out.AmendeDefautDepotRepositoryPort;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;

import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;

import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;

import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;

import mg.dgi.fiscaltrack.domain.enums.StatutDeclaration;

import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;

import mg.dgi.fiscaltrack.domain.enums.StatutValidation;

import mg.dgi.fiscaltrack.domain.exception.AccesRefuseException;

import mg.dgi.fiscaltrack.domain.exception.DeclarationDejaExistanteException;

import mg.dgi.fiscaltrack.domain.exception.ObligationIntrouvableException;

import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;

import mg.dgi.fiscaltrack.domain.model.Declaration;

import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;

import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;

import mg.dgi.fiscaltrack.domain.model.SaisieDeclaration;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.math.BigDecimal;

import java.time.LocalDate;

import java.time.OffsetDateTime;

import java.util.Map;

import java.util.Optional;



@Service

public class DeposerDeclarationUseCase {



    private static final Logger logger = LoggerFactory.getLogger(DeposerDeclarationUseCase.class);



    /** Seuil de tolerance (Ar) en dessous duquel un ecart est ignore. */

    private static final BigDecimal SEUIL_ECART = new BigDecimal("0.01");



    private final DeclarationRepositoryPort declarationRepositoryPort;

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;

    private final AmendeDefautDepotRepositoryPort amendeDefautDepotRepositoryPort;

    private final EnregistrerActionUseCase enregistrerActionUseCase;

    private final CalculerImpotDeclarationUseCase calculerImpotDeclarationUseCase;

    private final PreparerDonneesDeclarationUseCase preparerDonneesDeclarationUseCase;



    public DeposerDeclarationUseCase(DeclarationRepositoryPort declarationRepositoryPort,

                                      ObligationFiscaleRepositoryPort obligationRepositoryPort,

                                      CompteCourantFiscalRepositoryPort compteRepositoryPort,

                                      AmendeDefautDepotRepositoryPort amendeDefautDepotRepositoryPort,

                                      EnregistrerActionUseCase enregistrerActionUseCase,

                                      CalculerImpotDeclarationUseCase calculerImpotDeclarationUseCase,

                                      PreparerDonneesDeclarationUseCase preparerDonneesDeclarationUseCase) {

        this.declarationRepositoryPort = declarationRepositoryPort;

        this.obligationRepositoryPort = obligationRepositoryPort;

        this.compteRepositoryPort = compteRepositoryPort;

        this.amendeDefautDepotRepositoryPort = amendeDefautDepotRepositoryPort;

        this.enregistrerActionUseCase = enregistrerActionUseCase;

        this.calculerImpotDeclarationUseCase = calculerImpotDeclarationUseCase;

        this.preparerDonneesDeclarationUseCase = preparerDonneesDeclarationUseCase;

    }



    @Transactional

    public Declaration execute(Long idObligation, BigDecimal chiffreAffaires, BigDecimal impotCalcule) {

        ObligationFiscale obligation = obligationRepositoryPort.findById(idObligation)

                .orElseThrow(() -> new ObligationIntrouvableException(idObligation));



        verifierDoublon(idObligation);



        if (chiffreAffaires == null || chiffreAffaires.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException("Le chiffre d'affaires declare ne peut pas etre negatif");

        }

        if (impotCalcule == null || impotCalcule.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException("L'impot principal calcule ne peut pas etre negatif");

        }



        Optional<ResultatCalculFiscal> recalcul = calculerImpotDeclarationUseCase.execute(

                obligation.getCodeImpot(), chiffreAffaires, obligation.getNif());



        return enregistrer(obligation, chiffreAffaires, impotCalcule, recalcul, null,

                BigDecimal.ZERO, Map.of());

    }



    /**

     * Depot d'une declaration a partir du formulaire adapte a l'impot.

     * Le serveur recalcule avec les parametres du formulaire ; si le montant

     * affiche a l'ecran differe du recalcul, c'est le montant serveur qui est

     * retenu et l'ecart est signale dans le detail du calcul.

     *

     * @param donneesSaisiesJson copie JSON des champs saisis (tracabilite)

     * @param nifAppelant        NIF du contribuable connecte (null = agent)

     */

    @Transactional

    public Declaration deposer(SaisieDeclaration saisie,

                               String donneesSaisiesJson,

                               String nifAppelant) {

        ObligationFiscale obligation = obligationRepositoryPort.findById(saisie.getIdObligationFiscale())

                .orElseThrow(() -> new ObligationIntrouvableException(saisie.getIdObligationFiscale()));



        verifierPropriete(obligation, nifAppelant);

        verifierDoublon(obligation.getIdObligationFiscale());



        PreparerDonneesDeclarationUseCase.DonneesDeclaration donnees =

                preparerDonneesDeclarationUseCase.execute(saisie, obligation);



        Optional<ResultatCalculFiscal> recalcul = calculerImpotDeclarationUseCase.executeAvecParametres(

                obligation.getCodeImpot(), donnees.params(), obligation.getNif());



        return enregistrer(obligation, donnees.chiffreAffaires(),

                saisie.getImpotPrincipalCalcule(), recalcul, donneesSaisiesJson,

                donnees.creditAnterieur(), donnees.detailAcomptes());

    }



    private void verifierDoublon(Long idObligation) {

        if (declarationRepositoryPort.existsByIdObligationFiscale(idObligation)) {

            throw new DeclarationDejaExistanteException(idObligation);

        }

    }



    /** Un contribuable ne peut deposer que pour son propre NIF. */

    private void verifierPropriete(ObligationFiscale obligation, String nifAppelant) {

        if (nifAppelant != null && !nifAppelant.equals(obligation.getNif())) {

            throw new AccesRefuseException(

                    "Acces refuse : cette obligation fiscale appartient a un autre contribuable.");

        }

    }



    private Declaration enregistrer(ObligationFiscale obligation,

                                    BigDecimal chiffreAffaires,

                                    BigDecimal impotCalcule,

                                    Optional<ResultatCalculFiscal> recalcul,

                                    String donneesSaisiesJson,

                                    BigDecimal creditAnterieur,

                                    Map<String, Object> detailAcomptes) {

        Long idObligation = obligation.getIdObligationFiscale();



        if (impotCalcule == null || impotCalcule.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException("L'impot principal calcule ne peut pas etre negatif");

        }



        OffsetDateTime maintenant = OffsetDateTime.now();



        BigDecimal impotRetenu = impotCalcule;

        String detailCalcul = null;

        BigDecimal baseImposable = null;

        BigDecimal tauxApplique = null;

        BigDecimal montantBrut = null;

        BigDecimal montantMinimum = null;



        if (recalcul.isPresent()) {

            ResultatCalculFiscal resultat = recalcul.get();

            BigDecimal montantServeur = resultat.getSolde() == null

                    ? BigDecimal.ZERO

                    : resultat.getSolde();



            BigDecimal ecart = impotCalcule.subtract(montantServeur).abs();

            if (ecart.compareTo(SEUIL_ECART) > 0) {

                detailCalcul = "Ecart de recalcul : montant soumis=" + impotCalcule

                        + ", montant recalcule par le serveur=" + montantServeur

                        + ". Le montant du serveur est retenu.";

                logger.warn("[DECLARATION] Ecart pour l'obligation {} ({}): soumis={}, serveur={}",

                        idObligation, obligation.getCodeImpot(), impotCalcule, montantServeur);

            } else {

                detailCalcul = "Montant confirme par le recalcul serveur.";

            }



            impotRetenu = montantServeur;

            baseImposable = resultat.getBaseImposable();

            tauxApplique = resultat.getTauxApplique();

            montantBrut = resultat.getMontantBrut();

            montantMinimum = resultat.getMontantMinimum();



            if (resultat.getEtapesCalcul() != null && !resultat.getEtapesCalcul().isEmpty()) {

                detailCalcul = detailCalcul + System.lineSeparator()

                        + String.join(System.lineSeparator(), resultat.getEtapesCalcul());

            }

            if (resultat.getExplication() != null && !resultat.getExplication().isBlank()) {

                detailCalcul = detailCalcul + System.lineSeparator() + resultat.getExplication();

            }

        }



        BigDecimal creditUtilise = calculerCreditUtilise(creditAnterieur, recalcul, detailAcomptes);

        if (detailAcomptes != null && !detailAcomptes.isEmpty()) {

            String resume = resumerAcomptes(detailAcomptes);

            detailCalcul = detailCalcul == null ? resume : detailCalcul + System.lineSeparator() + resume;

        }



        Declaration declaration = Declaration.builder()

                .idObligationFiscale(idObligation)

                .chiffreAffairesDeclare(chiffreAffaires)

                .impotPrincipalCalcule(impotRetenu)

                .dateSoumission(maintenant)

                .statutValidation(StatutValidation.EN_ATTENTE_VALIDATION)

                .baseImposable(baseImposable)

                .tauxApplique(tauxApplique)

                .montantBrut(montantBrut)

                .montantMinimum(montantMinimum)

                .creditUtilise(creditUtilise)

                .detailCalcul(detailCalcul)

                .donneesSaisies(donneesSaisiesJson)

                .build();

        Declaration declarationSauvee = declarationRepositoryPort.save(declaration);



        obligation.setStatutDeclaration(StatutDeclaration.DEPOSE);

        obligation.setDateDepotEffectif(LocalDate.now());

        obligationRepositoryPort.save(obligation);



        BigDecimal montantAmende = amendeDefautDepotRepositoryPort

                .findByIdObligationFiscale(idObligation)

                .map(a -> a.getMontant() == null ? BigDecimal.ZERO : a.getMontant())

                .orElse(BigDecimal.ZERO);



        CompteCourantFiscal compte = CompteCourantFiscal.builder()

                .idDeclaration(declarationSauvee.getIdDeclaration())

                .nif(obligation.getNif())

                .dateCreationLigne(maintenant)

                .montantPrincipal(impotRetenu)

                .montantPenalites(BigDecimal.ZERO)

                .montantAmende(montantAmende)

                .montantPaye(BigDecimal.ZERO)

                .statutRecouvrement(StatutRecouvrement.NON_SOLDE)

                .build();

        compteRepositoryPort.save(compte);



        enregistrerActionUseCase.execute(

                obligation.getNif(),

                "DEPOT_DECLARATION",

                "Declaration #" + declarationSauvee.getIdDeclaration() + " pour obligation #" + idObligation);



        return declarationSauvee;

    }



    /**

     * Part du credit anterieur effectivement imputee : le credit n'est jamais

     * rembourse, il est reporte sur la declaration suivante du meme impot.

     */

    private BigDecimal calculerCreditUtilise(BigDecimal creditAnterieur,

                                             Optional<ResultatCalculFiscal> recalcul,

                                             Map<String, Object> detailAcomptes) {

        if (creditAnterieur == null || creditAnterieur.signum() <= 0) {

            return BigDecimal.ZERO;

        }

        if (detailAcomptes == null || detailAcomptes.isEmpty()) {

            return BigDecimal.ZERO;

        }

        BigDecimal impotAvantAcomptes = recalcul
                .map(resultat -> impotDuAvantAcomptes(resultat, detailAcomptes))
                .orElse(null);
        if (impotAvantAcomptes == null) {
            BigDecimal solde = recalcul.map(ResultatCalculFiscal::getSolde)
                    .filter(java.util.Objects::nonNull)
                    .orElse(BigDecimal.ZERO);
            return solde.add(creditAnterieur).min(creditAnterieur);
        }
        BigDecimal dejaDeduitHorsCredit = valeurOuZero(detailAcomptes.get("acomptesPayes"))
                .add(valeurOuZero(detailAcomptes.get("autresAcomptes")));
        BigDecimal disponible = impotAvantAcomptes.subtract(dejaDeduitHorsCredit);
        if (disponible.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return disponible.min(creditAnterieur);

    }



    /**
     * Impot du avant deduction des acomptes et du credit, reconstruit a partir
     * du resultat du moteur (le solde seul ne permet pas de savoir quelle part
     * du credit anterieur a ete reellement consommee).
     */
    private BigDecimal impotDuAvantAcomptes(ResultatCalculFiscal resultat,
                                            Map<String, Object> detailAcomptes) {
        if (resultat == null) {
            return null;
        }
        Object code = detailAcomptes.get("codeImpot");
        boolean is = code != null && "IS".equalsIgnoreCase(code.toString());
        BigDecimal brut = resultat.getMontantBrut();
        BigDecimal minimum = resultat.getMontantMinimum();
        if (brut == null && minimum == null) {
            return null;
        }
        BigDecimal impot = valeurOuZero(brut);
        if (is) {
            impot = impot.subtract(valeurOuZero(resultat.getReductions()));
        }
        if (impot.signum() < 0) {
            impot = BigDecimal.ZERO;
        }
        if (minimum != null && minimum.compareTo(impot) > 0) {
            impot = minimum;
        }
        return impot;
    }

    private static BigDecimal valeurOuZero(Object valeur) {
        if (valeur instanceof BigDecimal montant) {
            return montant;
        }
        if (valeur == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(valeur.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String resumerAcomptes(Map<String, Object> detail) {

        Object payes = detail.get("acomptesPayes");

        Object autres = detail.get("autresAcomptes");

        Object credit = detail.get("creditAnterieur");

        Object total = detail.get("totalDeduit");

        StringBuilder sb = new StringBuilder("Acomptes déduits : ")

                .append(total).append(" Ar")

                .append(" (payés ").append(payes)

                .append(" Ar, autres ").append(autres)

                .append(" Ar, crédit antérieur ").append(credit).append(" Ar)");

        Object avertissements = detail.get("avertissements");

        if (avertissements instanceof java.util.List<?> liste) {

            for (Object a : liste) {

                sb.append(System.lineSeparator()).append("AVERTISSEMENT : ").append(a);

            }

        }

        return sb.toString();

    }

}


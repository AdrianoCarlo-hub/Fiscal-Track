package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.AmendeDefautDepotRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.StatutDeclaration;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.enums.StatutValidation;
import mg.dgi.fiscaltrack.domain.exception.DeclarationDejaExistanteException;
import mg.dgi.fiscaltrack.domain.exception.ObligationIntrouvableException;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    public DeposerDeclarationUseCase(DeclarationRepositoryPort declarationRepositoryPort,
                                      ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                      CompteCourantFiscalRepositoryPort compteRepositoryPort,
                                      AmendeDefautDepotRepositoryPort amendeDefautDepotRepositoryPort,
                                      EnregistrerActionUseCase enregistrerActionUseCase,
                                      CalculerImpotDeclarationUseCase calculerImpotDeclarationUseCase) {
        this.declarationRepositoryPort = declarationRepositoryPort;
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.compteRepositoryPort = compteRepositoryPort;
        this.amendeDefautDepotRepositoryPort = amendeDefautDepotRepositoryPort;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
        this.calculerImpotDeclarationUseCase = calculerImpotDeclarationUseCase;
    }

    @Transactional
    public Declaration execute(Long idObligation, BigDecimal chiffreAffaires, BigDecimal impotCalcule) {
        ObligationFiscale obligation = obligationRepositoryPort.findById(idObligation)
                .orElseThrow(() -> new ObligationIntrouvableException(idObligation));

        if (declarationRepositoryPort.existsByIdObligationFiscale(idObligation)) {
            throw new DeclarationDejaExistanteException(idObligation);
        }

        if (chiffreAffaires == null || chiffreAffaires.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le chiffre d'affaires declare ne peut pas etre negatif");
        }
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

        Optional<ResultatCalculFiscal> recalcul = calculerImpotDeclarationUseCase.execute(
                obligation.getCodeImpot(), chiffreAffaires, obligation.getNif());
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
                .detailCalcul(detailCalcul)
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
}

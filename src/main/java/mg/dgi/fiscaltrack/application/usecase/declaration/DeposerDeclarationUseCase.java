package mg.dgi.fiscaltrack.application.usecase.declaration;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class DeposerDeclarationUseCase {

    private final DeclarationRepositoryPort declarationRepositoryPort;
    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public DeposerDeclarationUseCase(DeclarationRepositoryPort declarationRepositoryPort,
                                      ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                      CompteCourantFiscalRepositoryPort compteRepositoryPort,
                                      EnregistrerActionUseCase enregistrerActionUseCase) {
        this.declarationRepositoryPort = declarationRepositoryPort;
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.compteRepositoryPort = compteRepositoryPort;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
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

        Declaration declaration = Declaration.builder()
                .idObligationFiscale(idObligation)
                .chiffreAffairesDeclare(chiffreAffaires)
                .impotPrincipalCalcule(impotCalcule)
                .dateSoumission(maintenant)
                .statutValidation(StatutValidation.EN_ATTENTE_VALIDATION)
                .build();
        Declaration declarationSauvee = declarationRepositoryPort.save(declaration);

        obligation.setStatutDeclaration(StatutDeclaration.DEPOSE);
        obligation.setDateDepotEffectif(LocalDate.now());
        obligationRepositoryPort.save(obligation);

        CompteCourantFiscal compte = CompteCourantFiscal.builder()
                .idDeclaration(declarationSauvee.getIdDeclaration())
                .nif(obligation.getNif())
                .dateCreationLigne(maintenant)
                .montantPrincipal(impotCalcule)
                .montantPenalites(BigDecimal.ZERO)
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

package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.TypeImpotRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Assemblage du contexte de notification a partir d'une obligation fiscale
 * (impot, periode, echeance, montant du quand un compte existe).
 */
@Service
public class ContexteObligationService {

    private final TypeImpotRepositoryPort typeImpotRepositoryPort;
    private final DeclarationRepositoryPort declarationRepositoryPort;
    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;

    public ContexteObligationService(TypeImpotRepositoryPort typeImpotRepositoryPort,
                                     DeclarationRepositoryPort declarationRepositoryPort,
                                     CompteCourantFiscalRepositoryPort compteRepositoryPort) {
        this.typeImpotRepositoryPort = typeImpotRepositoryPort;
        this.declarationRepositoryPort = declarationRepositoryPort;
        this.compteRepositoryPort = compteRepositoryPort;
    }

    public ContexteNotification depuis(ObligationFiscale obligation) {
        Optional<Declaration> declaration = declarationRepositoryPort
                .findByIdObligationFiscale(obligation.getIdObligationFiscale());
        Optional<CompteCourantFiscal> compte = declaration
                .flatMap(d -> compteRepositoryPort.findByIdDeclaration(d.getIdDeclaration()));

        return ContexteNotification.builder()
                .nif(obligation.getNif())
                .codeImpot(obligation.getCodeImpot())
                .nomImpot(nomImpot(obligation.getCodeImpot()))
                .periode(obligation.getPeriodeFiscale())
                .echeance(obligation.getDateLimiteReelle())
                .montantDu(compte.map(CompteCourantFiscal::getResteARecouvrer).orElse(null))
                .montantPaye(compte.map(CompteCourantFiscal::getMontantPaye).orElse(null))
                .build();
    }

    public String nomImpot(String codeImpot) {
        return typeImpotRepositoryPort.findByCodeImpot(codeImpot)
                .map(t -> t.getNomImpot())
                .orElse(codeImpot);
    }

    public BigDecimal resteARecouvrer(CompteCourantFiscal compte) {
        if (compte.getResteARecouvrer() != null) {
            return compte.getResteARecouvrer();
        }
        BigDecimal total = nullSafe(compte.getMontantPrincipal())
                .add(nullSafe(compte.getMontantPenalites()))
                .add(nullSafe(compte.getMontantAmende()))
                .subtract(nullSafe(compte.getMontantPaye()));
        return total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total;
    }

    private BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

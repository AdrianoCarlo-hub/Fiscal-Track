package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.AgentFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.domain.exception.ObligationIntrouvableException;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.stereotype.Service;

@Service
public class AffecterObligationAgentUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final AgentFiscalRepositoryPort agentRepositoryPort;

    public AffecterObligationAgentUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                           AgentFiscalRepositoryPort agentRepositoryPort) {
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.agentRepositoryPort = agentRepositoryPort;
    }

    public ObligationFiscale execute(Long idObligation, String idAgent) {
        ObligationFiscale obligation = obligationRepositoryPort.findById(idObligation)
                .orElseThrow(() -> new ObligationIntrouvableException(idObligation));

        if (!agentRepositoryPort.existsById(idAgent)) {
            throw new IllegalArgumentException("Agent fiscal introuvable : " + idAgent);
        }

        obligation.setIdAgent(idAgent);
        return obligationRepositoryPort.save(obligation);
    }
}

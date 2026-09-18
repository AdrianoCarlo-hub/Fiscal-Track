package mg.dgi.fiscaltrack.application.usecase.admin;

import mg.dgi.fiscaltrack.application.port.out.AgentFiscalRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.AgentFiscal;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GererUtilisateursUseCase {

    private final AgentFiscalRepositoryPort agentRepositoryPort;

    public GererUtilisateursUseCase(AgentFiscalRepositoryPort agentRepositoryPort) {
        this.agentRepositoryPort = agentRepositoryPort;
    }

    public AgentFiscal ajouter(AgentFiscal agent) {
        if (agent.getIdAgent() == null || !agent.getIdAgent().matches("^[A-Z0-9]{6}$")) {
            throw new IllegalArgumentException(
                    "L'identifiant agent doit contenir 6 caracteres alphanumeriques en majuscules");
        }
        if (agentRepositoryPort.existsById(agent.getIdAgent())) {
            throw new IllegalArgumentException("Un agent avec cet identifiant existe deja");
        }
        if (agentRepositoryPort.existsByEmail(agent.getEmailAgent())) {
            throw new IllegalArgumentException("Un agent avec cet email existe deja");
        }
        return agentRepositoryPort.save(agent);
    }

    public AgentFiscal modifier(String idAgent, AgentFiscal donneesModifiees) {
        AgentFiscal existant = agentRepositoryPort.findById(idAgent)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Agent introuvable : " + idAgent));

        if (donneesModifiees.getNomAgent() != null) {
            existant.setNomAgent(donneesModifiees.getNomAgent());
        }
        if (donneesModifiees.getPrenomAgent() != null) {
            existant.setPrenomAgent(donneesModifiees.getPrenomAgent());
        }
        if (donneesModifiees.getTelephoneAgent() != null) {
            existant.setTelephoneAgent(donneesModifiees.getTelephoneAgent());
        }
        if (donneesModifiees.getDivision() != null) {
            existant.setDivision(donneesModifiees.getDivision());
        }
        if (donneesModifiees.getRoleSecurite() != null) {
            existant.setRoleSecurite(donneesModifiees.getRoleSecurite());
        }
        return agentRepositoryPort.save(existant);
    }

    public void supprimer(String idAgent) {
        if (!agentRepositoryPort.existsById(idAgent)) {
            throw new IllegalArgumentException("Agent introuvable : " + idAgent);
        }
        agentRepositoryPort.deleteById(idAgent);
    }

    public List<AgentFiscal> tous() {
        return agentRepositoryPort.findAll();
    }

    public AgentFiscal parId(String idAgent) {
        return agentRepositoryPort.findById(idAgent)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Agent introuvable : " + idAgent));
    }
}

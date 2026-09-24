package mg.dgi.fiscaltrack.application.usecase.auth;

import mg.dgi.fiscaltrack.application.port.out.AgentFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.exception.AccesNonAutoriseException;
import mg.dgi.fiscaltrack.domain.model.AgentFiscal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthentifierAgentUseCase {

    private final AgentFiscalRepositoryPort agentRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public AuthentifierAgentUseCase(AgentFiscalRepositoryPort agentRepositoryPort,
                                     PasswordEncoder passwordEncoder,
                                     EnregistrerActionUseCase enregistrerActionUseCase) {
        this.agentRepositoryPort = agentRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    public AgentFiscal execute(String emailAgent, String motDePasse) {
        if (emailAgent == null || emailAgent.isBlank()) {
            throw new AccesNonAutoriseException("L'email de l'agent est obligatoire");
        }
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new AccesNonAutoriseException("Le mot de passe est obligatoire");
        }

        AgentFiscal agent = agentRepositoryPort.findByEmail(emailAgent)
                .orElseThrow(() -> new AccesNonAutoriseException("Aucun agent avec cet email"));

        if (!passwordEncoder.matches(motDePasse, agent.getMotDePasseHashAgent())) {
            enregistrerActionUseCase.execute(emailAgent, "CONNEXION_ECHEC", "Mot de passe incorrect");
            throw new AccesNonAutoriseException("Mot de passe incorrect");
        }

        enregistrerActionUseCase.execute(agent.getIdAgent(), "CONNEXION", "Email=" + emailAgent);
        return agent;
    }
}

package mg.dgi.fiscaltrack.application.usecase.admin;

import mg.dgi.fiscaltrack.application.port.out.AgentFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.model.AgentFiscal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GererUtilisateursUseCase {

    private final AgentFiscalRepositoryPort agentRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public GererUtilisateursUseCase(AgentFiscalRepositoryPort agentRepositoryPort,
                                    PasswordEncoder passwordEncoder,
                                    EnregistrerActionUseCase enregistrerActionUseCase) {
        this.agentRepositoryPort = agentRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    public AgentFiscal ajouter(AgentFiscal agent) {
        return ajouter(agent, null);
    }

    /**
     * Cree un agent. Le mot de passe fourni est encode (BCrypt) avant
     * persistance ; il n'est jamais renvoye par l'API.
     */
    public AgentFiscal ajouter(AgentFiscal agent, String auteur) {
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
        if (agent.getMotDePasseHashAgent() != null && !agent.getMotDePasseHashAgent().isBlank()) {
            agent.setMotDePasseHashAgent(passwordEncoder.encode(agent.getMotDePasseHashAgent()));
        }
        AgentFiscal saved = agentRepositoryPort.save(agent);

        enregistrerActionUseCase.execute(
                auteur == null ? "SYSTEME" : auteur,
                "CREATION_UTILISATEUR",
                "Creation de l'utilisateur " + saved.getIdAgent()
                        + " (" + saved.getEmailAgent() + ", role " + saved.getRoleSecurite() + ")");
        return saved;
    }

    public AgentFiscal modifier(String idAgent, AgentFiscal donneesModifiees) {
        return modifier(idAgent, donneesModifiees, null);
    }

    /**
     * Modifie un agent (patch strict). Le mot de passe n'est re-encode que
     * si un nouveau mot de passe est fourni.
     */
    public AgentFiscal modifier(String idAgent, AgentFiscal donneesModifiees, String auteur) {
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
        if (donneesModifiees.getEmailAgent() != null) {
            existant.setEmailAgent(donneesModifiees.getEmailAgent());
        }
        if (donneesModifiees.getDivision() != null) {
            existant.setDivision(donneesModifiees.getDivision());
        }
        if (donneesModifiees.getRoleSecurite() != null) {
            existant.setRoleSecurite(donneesModifiees.getRoleSecurite());
        }
        if (donneesModifiees.getMotDePasseHashAgent() != null
                && !donneesModifiees.getMotDePasseHashAgent().isBlank()) {
            existant.setMotDePasseHashAgent(
                    passwordEncoder.encode(donneesModifiees.getMotDePasseHashAgent()));
        }
        AgentFiscal saved = agentRepositoryPort.save(existant);

        enregistrerActionUseCase.execute(
                auteur == null ? "SYSTEME" : auteur,
                "MODIFICATION_UTILISATEUR",
                "Modification de l'utilisateur " + saved.getIdAgent());
        return saved;
    }

    public void supprimer(String idAgent) {
        supprimer(idAgent, null);
    }

    public void supprimer(String idAgent, String auteur) {
        if (!agentRepositoryPort.existsById(idAgent)) {
            throw new IllegalArgumentException("Agent introuvable : " + idAgent);
        }
        agentRepositoryPort.deleteById(idAgent);

        enregistrerActionUseCase.execute(
                auteur == null ? "SYSTEME" : auteur,
                "SUPPRESSION_UTILISATEUR",
                "Suppression de l'utilisateur " + idAgent);
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

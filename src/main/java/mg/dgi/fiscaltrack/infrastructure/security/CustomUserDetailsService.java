package mg.dgi.fiscaltrack.infrastructure.security;

import mg.dgi.fiscaltrack.application.port.out.AgentFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.AgentFiscal;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final ContribuableRepositoryPort contribuableRepositoryPort;
    private final AgentFiscalRepositoryPort agentRepositoryPort;

    public CustomUserDetailsService(ContribuableRepositoryPort contribuableRepositoryPort,
                                     AgentFiscalRepositoryPort agentRepositoryPort) {
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.agentRepositoryPort = agentRepositoryPort;
    }

    /**
     * Recherche un utilisateur par son identifiant. On regarde d'abord
     * cote agent (email), puis cote contribuable (NIF).
     */
    @Override
    public UserDetails loadUserByUsername(String identifiant) throws UsernameNotFoundException {
        var agentOpt = agentRepositoryPort.findByEmail(identifiant);
        if (agentOpt.isPresent()) {
            AgentFiscal agent = agentOpt.get();
            return User.builder()
                    .username(agent.getIdAgent())
                    .password(agent.getMotDePasseHashAgent())
                    .authorities(List.of(new SimpleGrantedAuthority(
                            "ROLE_" + agent.getRoleSecurite().name())))
                    .build();
        }

        var contribuableOpt = contribuableRepositoryPort.findByNif(identifiant);
        if (contribuableOpt.isPresent()) {
            Contribuable contribuable = contribuableOpt.get();
            return User.builder()
                    .username(contribuable.getNif())
                    .password(contribuable.getMotDePasseHashContribuable())
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_CONTRIBUABLE")))
                    .build();
        }

        throw new UsernameNotFoundException("Utilisateur introuvable : " + identifiant);
    }
}

package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.AgentFiscal;

import java.util.List;
import java.util.Optional;

public interface AgentFiscalRepositoryPort {

    AgentFiscal save(AgentFiscal agentFiscal);

    Optional<AgentFiscal> findById(String idAgent);

    Optional<AgentFiscal> findByEmail(String emailAgent);

    List<AgentFiscal> findAll();

    List<AgentFiscal> findByDivision(String division);

    void deleteById(String idAgent);

    boolean existsById(String idAgent);

    boolean existsByEmail(String emailAgent);
}

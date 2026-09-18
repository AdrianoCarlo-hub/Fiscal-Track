package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.AgentFiscalRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.AgentFiscal;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.AgentFiscalMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.AgentFiscalJpaRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AgentFiscalRepositoryAdapter implements AgentFiscalRepositoryPort {

    private final AgentFiscalJpaRepository jpaRepository;
    private final AgentFiscalMapper mapper;

    public AgentFiscalRepositoryAdapter(AgentFiscalJpaRepository jpaRepository,
                                         AgentFiscalMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public AgentFiscal save(AgentFiscal agentFiscal) {
        if (agentFiscal.getCreatedAt() == null) {
            agentFiscal.setCreatedAt(OffsetDateTime.now());
        }
        agentFiscal.setUpdatedAt(OffsetDateTime.now());
        var entity = mapper.toEntity(agentFiscal);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<AgentFiscal> findById(String idAgent) {
        return jpaRepository.findById(idAgent).map(mapper::toDomain);
    }

    @Override
    public Optional<AgentFiscal> findByEmail(String emailAgent) {
        return jpaRepository.findByEmailAgent(emailAgent).map(mapper::toDomain);
    }

    @Override
    public List<AgentFiscal> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgentFiscal> findByDivision(String division) {
        return jpaRepository.findByDivision(division).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String idAgent) {
        jpaRepository.deleteById(idAgent);
    }

    @Override
    public boolean existsById(String idAgent) {
        return jpaRepository.existsById(idAgent);
    }

    @Override
    public boolean existsByEmail(String emailAgent) {
        return jpaRepository.existsByEmailAgent(emailAgent);
    }
}

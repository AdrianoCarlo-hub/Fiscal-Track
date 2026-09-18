package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.AgentFiscalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentFiscalJpaRepository extends JpaRepository<AgentFiscalEntity, String> {

    Optional<AgentFiscalEntity> findByEmailAgent(String emailAgent);

    boolean existsByEmailAgent(String emailAgent);

    List<AgentFiscalEntity> findByDivision(String division);
}

package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.ContribuableEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContribuableJpaRepository extends JpaRepository<ContribuableEntity, String> {

    @Query("SELECT c FROM ContribuableEntity c WHERE " +
           "LOWER(c.nif) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.raisonSociale) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.emailContribuable) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<ContribuableEntity> search(@Param("q") String query);

    @Query("SELECT c FROM ContribuableEntity c WHERE " +
           "LOWER(c.nif) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.raisonSociale) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.emailContribuable) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<ContribuableEntity> searchPaginated(@Param("q") String query, Pageable pageable);
}

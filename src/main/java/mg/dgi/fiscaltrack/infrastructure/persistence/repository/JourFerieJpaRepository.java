package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.JourFerieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JourFerieJpaRepository extends JpaRepository<JourFerieEntity, Long> {

    Optional<JourFerieEntity> findByDateFerie(LocalDate dateFerie);

    boolean existsByDateFerie(LocalDate dateFerie);

    @Query("SELECT j FROM JourFerieEntity j WHERE YEAR(j.dateFerie) = :annee ORDER BY j.dateFerie")
    List<JourFerieEntity> findByAnnee(@Param("annee") int annee);
}

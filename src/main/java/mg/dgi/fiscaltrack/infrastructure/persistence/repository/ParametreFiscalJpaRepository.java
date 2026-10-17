package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.ParametreFiscalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Year;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour les parametres fiscaux.
 */
@Repository
public interface ParametreFiscalJpaRepository extends JpaRepository<ParametreFiscalEntity, String> {

    /**
     * Trouve un parametre par son code.
     */
    Optional<ParametreFiscalEntity> findByCode(String code);

    /**
     * Retourne tous les parametres tries par code.
     */
    List<ParametreFiscalEntity> findAllByOrderByCodeAsc();

    /**
     * Trouve tous les parametres applicables pour une annee donnee.
     * annee_fin peut etre NULL (parametre sans date de fin) : dans ce cas le
     * parametre reste applicable. Une requete derivee du nom de methode
     * (AnneeFinGreaterThanEqual) exclurait a tort ces lignes ; on utilise donc
     * une requete explicite.
     */
    @Query("SELECT p FROM ParametreFiscalEntity p " +
            "WHERE p.anneeDebut <= :annee " +
            "AND (p.anneeFin IS NULL OR p.anneeFin >= :annee)")
    List<ParametreFiscalEntity> findApplicablesPourAnnee(@Param("annee") Year annee);

    /**
     * Trouve tous les parametres par secteur.
     */
    List<ParametreFiscalEntity> findBySecteur(String secteur);

    /**
     * Trouve tous les parametres par base legale.
     */
    List<ParametreFiscalEntity> findByBaseLegale(String baseLegale);

    /**
     * Trouve tous les parametres par type (via code prefix).
     */
    List<ParametreFiscalEntity> findByCodeStartingWith(String prefix);
}

package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByNif(String nif);

    List<NotificationEntity> findByStatutEnvoi(String statutEnvoi);

    List<NotificationEntity> findByStatutEnvoiAndDateEnvoiPrevueBefore(String statutEnvoi,
                                                                      OffsetDateTime date);

    List<NotificationEntity> findByStatutEnvoiAndNbTentativesLessThan(String statutEnvoi,
                                                                      Integer nbTentativesMax);

    long countByNifAndCanalEnvoiAndLuFalse(String nif, String canalEnvoi);

    List<NotificationEntity> findByNifAndCanalEnvoiOrderByCreatedAtDesc(String nif, String canalEnvoi);

    @Query("""
            SELECT n FROM NotificationEntity n
            WHERE (:nif IS NULL OR n.nif = :nif)
              AND (:statut IS NULL OR n.statutEnvoi = :statut)
              AND (:canal IS NULL OR n.canalEnvoi = :canal)
            """)
    Page<NotificationEntity> rechercher(@Param("nif") String nif,
                                        @Param("statut") String statut,
                                        @Param("canal") String canal,
                                        Pageable pageable);

    boolean existsByIdObligationFiscaleAndTypeRelanceAndCanalEnvoi(Long idObligationFiscale,
                                                                   String typeRelance,
                                                                   String canalEnvoi);

    boolean existsByIdCompteAndTypeRelanceAndCanalEnvoi(Long idCompte,
                                                        String typeRelance,
                                                        String canalEnvoi);
}

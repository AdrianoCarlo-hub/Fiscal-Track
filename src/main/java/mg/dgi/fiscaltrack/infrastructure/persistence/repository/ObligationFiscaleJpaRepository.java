package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.ObligationFiscaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ObligationFiscaleJpaRepository extends JpaRepository<ObligationFiscaleEntity, Long> {

    List<ObligationFiscaleEntity> findByNif(String nif);

    List<ObligationFiscaleEntity> findByStatutDeclaration(String statutDeclaration);

    List<ObligationFiscaleEntity> findByIdAgent(String idAgent);

    List<ObligationFiscaleEntity> findByNifAndPeriodeFiscale(String nif, String periodeFiscale);

    @Query("SELECT o FROM ObligationFiscaleEntity o WHERE o.dateLimiteReelle < :date " +
           "AND o.statutDeclaration <> 'DEPOSE'")
    List<ObligationFiscaleEntity> findEnRetard(@Param("date") LocalDate date);

    @Query("SELECT o FROM ObligationFiscaleEntity o WHERE o.dateLimiteReelle BETWEEN :date AND :datePlus " +
           "AND o.statutDeclaration = 'ATTENTE'")
    List<ObligationFiscaleEntity> findEcheancesProches(@Param("date") LocalDate date,
                                                       @Param("datePlus") LocalDate datePlus);
}

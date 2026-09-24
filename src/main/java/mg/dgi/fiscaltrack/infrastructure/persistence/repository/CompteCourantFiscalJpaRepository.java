package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.CompteCourantFiscalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompteCourantFiscalJpaRepository extends JpaRepository<CompteCourantFiscalEntity, Long> {

    Optional<CompteCourantFiscalEntity> findByIdDeclaration(Long idDeclaration);

    List<CompteCourantFiscalEntity> findByNif(String nif);

    List<CompteCourantFiscalEntity> findByStatutRecouvrement(String statutRecouvrement);

    @Query("SELECT c FROM CompteCourantFiscalEntity c WHERE c.statutRecouvrement <> 'SOLDE'")
    List<CompteCourantFiscalEntity> findNonSoldes();

    /**
     * Jointure compte -> declaration -> obligation pour identifier les
     * comptes dont l'echeance est depassee. Utilise pour l'emission
     * automatique des titres de perception.
     */
    @Query("SELECT c FROM CompteCourantFiscalEntity c " +
           "WHERE c.statutRecouvrement IN ('NON_SOLDE','PARTIEL') " +
           "AND c.idDeclaration IN (" +
           "   SELECT d.idDeclaration FROM DeclarationEntity d " +
           "   WHERE d.idObligationFiscale IN (" +
           "       SELECT o.idObligationFiscale FROM ObligationFiscaleEntity o " +
           "       WHERE o.dateLimiteReelle < :seuil" +
           "   )" +
           ")")
    List<CompteCourantFiscalEntity> findEligiblesTitrePerception(@Param("seuil") LocalDate seuil);

    /**
     * Comptes en TITRE_EMIS dont updated_at est anterieur au seuil,
     * candidats au lancement d'un ATD.
     */
    @Query("SELECT c FROM CompteCourantFiscalEntity c " +
           "WHERE c.statutRecouvrement = 'TITRE_EMIS' " +
           "AND c.updatedAt < :seuil")
    List<CompteCourantFiscalEntity> findEligiblesAtd(@Param("seuil") java.time.OffsetDateTime seuil);
}

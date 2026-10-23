package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.DeclarationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeclarationJpaRepository extends JpaRepository<DeclarationEntity, Long> {

    Optional<DeclarationEntity> findByIdObligationFiscale(Long idObligationFiscale);

    boolean existsByIdObligationFiscale(Long idObligationFiscale);

    List<DeclarationEntity> findByStatutValidation(String statutValidation);

    /** Toutes les declarations d'un contribuable, plus recentes d'abord. */
    @Query("""
            SELECT d FROM DeclarationEntity d
            WHERE d.idObligationFiscale IN (
                SELECT o.idObligationFiscale FROM ObligationFiscaleEntity o WHERE o.nif = :nif
            )
            ORDER BY d.dateSoumission DESC
            """)
    List<DeclarationEntity> findByNif(@Param("nif") String nif);

    /**
     * Derniere declaration deposee pour un NIF (via ses obligations).
     * Utilisee pour determiner le CA de reference de l'amende de defaut
     * de depot (CDI 20.01.52). Le resultat est ordonne par date de
     * soumission decroissante ; le caller limite a la premiere ligne.
     */
    @Query("""
            SELECT d FROM DeclarationEntity d
            WHERE d.idObligationFiscale IN (
                SELECT o.idObligationFiscale FROM ObligationFiscaleEntity o WHERE o.nif = :nif
            )
            ORDER BY d.dateSoumission DESC
            """)
    List<DeclarationEntity> findLatestByNif(@Param("nif") String nif, Pageable pageable);

    /**
     * Derniere declaration d'un impot donne pour un NIF (via ses obligations).
     * Utilisee pour le calcul des acomptes (25 % de l'IR N-1).
     */
    @Query("""
            SELECT d FROM DeclarationEntity d
            WHERE d.idObligationFiscale IN (
                SELECT o.idObligationFiscale FROM ObligationFiscaleEntity o
                WHERE o.nif = :nif AND o.codeImpot = :codeImpot
            )
            ORDER BY d.dateSoumission DESC
            """)
    List<DeclarationEntity> findLatestByNifAndCodeImpot(@Param("nif") String nif,
                                                        @Param("codeImpot") String codeImpot,
                                                        Pageable pageable);

    /**
     * Derniere declaration VALIDEE d'un impot dont l'exercice est strictement
     * anterieur a {@code periodeLimite} (format AAAA). Sert de base aux
     * acomptes obligatoires et au report du credit fiscal.
     */
    @Query("""
            SELECT d FROM DeclarationEntity d
            WHERE d.statutValidation = 'VALIDEE'
            AND d.idObligationFiscale IN (
                SELECT o.idObligationFiscale FROM ObligationFiscaleEntity o
                WHERE o.nif = :nif AND o.codeImpot = :codeImpot
                AND o.periodeFiscale < :periodeLimite
            )
            ORDER BY d.dateSoumission DESC
            """)
    List<DeclarationEntity> findValideeByNifEtImpotAvantPeriode(@Param("nif") String nif,
                                                                @Param("codeImpot") String codeImpot,
                                                                @Param("periodeLimite") String periodeLimite,
                                                                Pageable pageable);
}

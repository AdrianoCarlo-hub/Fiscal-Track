package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.CompteCourantFiscalMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.CompteCourantFiscalJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CompteCourantFiscalRepositoryAdapter implements CompteCourantFiscalRepositoryPort {

    private final CompteCourantFiscalJpaRepository jpaRepository;
    private final CompteCourantFiscalMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    public CompteCourantFiscalRepositoryAdapter(CompteCourantFiscalJpaRepository jpaRepository,
                                                 CompteCourantFiscalMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CompteCourantFiscal save(CompteCourantFiscal compteCourantFiscal) {
        if (compteCourantFiscal.getCreatedAt() == null) {
            compteCourantFiscal.setCreatedAt(OffsetDateTime.now());
        }
        compteCourantFiscal.setUpdatedAt(OffsetDateTime.now());
        var entity = mapper.toEntity(compteCourantFiscal);
        // saveAndFlush + refresh : force le flush puis relit la ligne pour peupler
        // les colonnes generees cote PostgreSQL (montant_total_du, reste_a_recouvrer).
        // Sans refresh, une entite deja presente dans le contexte de persistance
        // conserve des valeurs nulles pour ces colonnes (@Generated).
        var saved = jpaRepository.saveAndFlush(entity);
        entityManager.refresh(saved);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CompteCourantFiscal> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CompteCourantFiscal> findByIdDeclaration(Long idDeclaration) {
        return jpaRepository.findByIdDeclaration(idDeclaration).map(mapper::toDomain);
    }

    @Override
    public List<CompteCourantFiscal> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CompteCourantFiscal> findByNif(String nif) {
        return jpaRepository.findByNif(nif).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CompteCourantFiscal> findByStatut(String statutRecouvrement) {
        return jpaRepository.findByStatutRecouvrement(statutRecouvrement).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CompteCourantFiscal> findNonSoldes() {
        return jpaRepository.findNonSoldes().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<CompteCourantFiscal> findEligiblesTitrePerception(LocalDate seuilEcheance) {
        return jpaRepository.findEligiblesTitrePerception(seuilEcheance).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CompteCourantFiscal> findEligiblesAtd(LocalDate seuilUpdatedAt) {
        OffsetDateTime seuil = seuilUpdatedAt.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        return jpaRepository.findEligiblesAtd(seuil).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

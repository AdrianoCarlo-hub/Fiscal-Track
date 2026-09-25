package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.ContribuableMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.ContribuableJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ContribuableRepositoryAdapter implements ContribuableRepositoryPort {

    private final ContribuableJpaRepository jpaRepository;
    private final ContribuableMapper mapper;

    public ContribuableRepositoryAdapter(ContribuableJpaRepository jpaRepository,
                                          ContribuableMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Contribuable save(Contribuable contribuable) {
        if (contribuable.getCreatedAt() == null) {
            contribuable.setCreatedAt(OffsetDateTime.now());
        }
        contribuable.setUpdatedAt(OffsetDateTime.now());
        var entity = mapper.toEntity(contribuable);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Contribuable> findByNif(String nif) {
        return jpaRepository.findById(nif).map(mapper::toDomain);
    }

    @Override
    public List<Contribuable> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Contribuable> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteByNif(String nif) {
        jpaRepository.deleteById(nif);
    }

    @Override
    public boolean existsByNif(String nif) {
        return jpaRepository.existsById(nif);
    }

    @Override
    public List<Contribuable> search(String query) {
        return jpaRepository.search(query).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Contribuable> search(String query, Pageable pageable) {
        return jpaRepository.searchPaginated(query, pageable).map(mapper::toDomain);
    }
}

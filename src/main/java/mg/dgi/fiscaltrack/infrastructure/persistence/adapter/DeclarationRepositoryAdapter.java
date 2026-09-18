package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.DeclarationMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.DeclarationJpaRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DeclarationRepositoryAdapter implements DeclarationRepositoryPort {

    private final DeclarationJpaRepository jpaRepository;
    private final DeclarationMapper mapper;

    public DeclarationRepositoryAdapter(DeclarationJpaRepository jpaRepository,
                                         DeclarationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Declaration save(Declaration declaration) {
        if (declaration.getCreatedAt() == null) {
            declaration.setCreatedAt(OffsetDateTime.now());
        }
        declaration.setUpdatedAt(OffsetDateTime.now());
        var entity = mapper.toEntity(declaration);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Declaration> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Declaration> findByIdObligationFiscale(Long idObligationFiscale) {
        return jpaRepository.findByIdObligationFiscale(idObligationFiscale).map(mapper::toDomain);
    }

    @Override
    public List<Declaration> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Declaration> findByStatutValidation(String statutValidation) {
        return jpaRepository.findByStatutValidation(statutValidation).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByIdObligationFiscale(Long idObligationFiscale) {
        return jpaRepository.existsByIdObligationFiscale(idObligationFiscale);
    }
}

package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.TypeImpotRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.TypeImpot;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.TypeImpotMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.TypeImpotJpaRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TypeImpotRepositoryAdapter implements TypeImpotRepositoryPort {

    private final TypeImpotJpaRepository jpaRepository;
    private final TypeImpotMapper mapper;

    public TypeImpotRepositoryAdapter(TypeImpotJpaRepository jpaRepository,
                                       TypeImpotMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public TypeImpot save(TypeImpot typeImpot) {
        if (typeImpot.getCreatedAt() == null) {
            typeImpot.setCreatedAt(OffsetDateTime.now());
        }
        typeImpot.setUpdatedAt(OffsetDateTime.now());
        var entity = mapper.toEntity(typeImpot);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<TypeImpot> findByCodeImpot(String codeImpot) {
        return jpaRepository.findById(codeImpot).map(mapper::toDomain);
    }

    @Override
    public List<TypeImpot> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByCodeImpot(String codeImpot) {
        jpaRepository.deleteById(codeImpot);
    }

    @Override
    public boolean existsByCodeImpot(String codeImpot) {
        return jpaRepository.existsById(codeImpot);
    }
}

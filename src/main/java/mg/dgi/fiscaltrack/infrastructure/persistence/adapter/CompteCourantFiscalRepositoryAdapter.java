package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.CompteCourantFiscalMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.CompteCourantFiscalJpaRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CompteCourantFiscalRepositoryAdapter implements CompteCourantFiscalRepositoryPort {

    private final CompteCourantFiscalJpaRepository jpaRepository;
    private final CompteCourantFiscalMapper mapper;

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
        return mapper.toDomain(jpaRepository.save(entity));
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
}

package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.AmendeDefautDepotRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.AmendeDefautDepot;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.AmendeDefautDepotMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.AmendeDefautDepotJpaRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AmendeDefautDepotRepositoryAdapter implements AmendeDefautDepotRepositoryPort {

    private final AmendeDefautDepotJpaRepository jpaRepository;
    private final AmendeDefautDepotMapper mapper;

    public AmendeDefautDepotRepositoryAdapter(AmendeDefautDepotJpaRepository jpaRepository,
                                              AmendeDefautDepotMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public AmendeDefautDepot save(AmendeDefautDepot amende) {
        if (amende.getCreatedAt() == null) {
            amende.setCreatedAt(OffsetDateTime.now());
        }
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(amende)));
    }

    @Override
    public Optional<AmendeDefautDepot> findByIdObligationFiscale(Long idObligationFiscale) {
        return jpaRepository.findByIdObligationFiscale(idObligationFiscale).map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdObligationFiscale(Long idObligationFiscale) {
        return jpaRepository.existsByIdObligationFiscale(idObligationFiscale);
    }

    @Override
    public List<AmendeDefautDepot> findByNif(String nif) {
        return jpaRepository.findByNif(nif).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

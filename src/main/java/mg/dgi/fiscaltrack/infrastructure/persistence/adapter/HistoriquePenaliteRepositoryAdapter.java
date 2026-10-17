package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.HistoriquePenaliteRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.HistoriquePenalite;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.HistoriquePenaliteMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.HistoriquePenaliteJpaRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HistoriquePenaliteRepositoryAdapter implements HistoriquePenaliteRepositoryPort {

    private final HistoriquePenaliteJpaRepository jpaRepository;
    private final HistoriquePenaliteMapper mapper;

    public HistoriquePenaliteRepositoryAdapter(HistoriquePenaliteJpaRepository jpaRepository,
                                               HistoriquePenaliteMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public HistoriquePenalite save(HistoriquePenalite ligne) {
        if (ligne.getCreatedAt() == null) {
            ligne.setCreatedAt(OffsetDateTime.now());
        }
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(ligne)));
    }

    @Override
    public List<HistoriquePenalite> findByIdCompte(Long idCompte) {
        return jpaRepository.findByIdCompteOrderByDateCalculAsc(idCompte).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<HistoriquePenalite> findByIdCompteAndTypeLigne(Long idCompte, String typeLigne) {
        return jpaRepository.findByIdCompteAndTypeLigne(idCompte, typeLigne).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

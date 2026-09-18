package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.JourFerieRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.JourFerie;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.JourFerieMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.JourFerieJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JourFerieRepositoryAdapter implements JourFerieRepositoryPort {

    private final JourFerieJpaRepository jpaRepository;
    private final JourFerieMapper mapper;

    public JourFerieRepositoryAdapter(JourFerieJpaRepository jpaRepository,
                                       JourFerieMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public JourFerie save(JourFerie jourFerie) {
        if (jourFerie.getCreatedAt() == null) {
            jourFerie.setCreatedAt(OffsetDateTime.now());
        }
        var entity = mapper.toEntity(jourFerie);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<JourFerie> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<JourFerie> findByDate(LocalDate date) {
        return jpaRepository.findByDateFerie(date).map(mapper::toDomain);
    }

    @Override
    public List<JourFerie> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<JourFerie> findByAnnee(int annee) {
        return jpaRepository.findByAnnee(annee).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByDate(LocalDate date) {
        return jpaRepository.existsByDateFerie(date);
    }
}

package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.NotificationRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.Notification;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.NotificationMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.NotificationJpaRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository jpaRepository;
    private final NotificationMapper mapper;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpaRepository,
                                          NotificationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Notification save(Notification notification) {
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(OffsetDateTime.now());
        }
        var entity = mapper.toEntity(notification);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Notification> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findByNif(String nif) {
        return jpaRepository.findByNif(nif).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findByStatutEnvoi(String statutEnvoi) {
        return jpaRepository.findByStatutEnvoi(statutEnvoi).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findEnAttenteAvant(OffsetDateTime date) {
        return jpaRepository.findByStatutEnvoiAndDateEnvoiPrevueBefore("EN_ATTENTE", date).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}

package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.NotificationRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.Notification;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.NotificationMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.NotificationJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
                .toList();
    }

    @Override
    public Page<Notification> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public List<Notification> findByNif(String nif) {
        return jpaRepository.findByNif(nif).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Notification> findByStatutEnvoi(String statutEnvoi) {
        return jpaRepository.findByStatutEnvoi(statutEnvoi).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Notification> findEnAttenteAvant(OffsetDateTime date) {
        return jpaRepository.findByStatutEnvoiAndDateEnvoiPrevueBefore("EN_ATTENTE", date).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<Notification> rechercher(String nif, String statutEnvoi, String canalEnvoi,
                                         Pageable pageable) {
        return jpaRepository.rechercher(
                        blankToNull(nif), blankToNull(statutEnvoi), blankToNull(canalEnvoi), pageable)
                .map(mapper::toDomain);
    }

    @Override
    public List<Notification> findEchecsRetentables(int nbTentativesMax) {
        return jpaRepository
                .findByStatutEnvoiAndNbTentativesLessThan("ECHEC", nbTentativesMax)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByObjetAndTypeAndCanal(Long idObligationFiscale, Long idCompte,
                                                String typeRelance, String canalEnvoi) {
        if (idObligationFiscale != null) {
            return jpaRepository.existsByIdObligationFiscaleAndTypeRelanceAndCanalEnvoi(
                    idObligationFiscale, typeRelance, canalEnvoi);
        }
        if (idCompte != null) {
            return jpaRepository.existsByIdCompteAndTypeRelanceAndCanalEnvoi(
                    idCompte, typeRelance, canalEnvoi);
        }
        return false;
    }

    @Override
    public long countNonLues(String nif) {
        return jpaRepository.countByNifAndCanalEnvoiAndLuFalse(nif, "PORTAIL");
    }

    @Override
    public List<Notification> findPortailByNif(String nif) {
        return jpaRepository.findByNifAndCanalEnvoiOrderByCreatedAtDesc(nif, "PORTAIL").stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private String blankToNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur;
    }
}

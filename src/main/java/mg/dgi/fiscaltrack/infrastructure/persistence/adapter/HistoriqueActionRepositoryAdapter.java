package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.HistoriqueActionRepositoryPort;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.HistoriqueActionEntity;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.HistoriqueActionJpaRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HistoriqueActionRepositoryAdapter implements HistoriqueActionRepositoryPort {

    private final HistoriqueActionJpaRepository jpaRepository;

    public HistoriqueActionRepositoryAdapter(HistoriqueActionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void enregistrer(String utilisateur, String action, String details) {
        HistoriqueActionEntity entity = HistoriqueActionEntity.builder()
                .utilisateur(utilisateur)
                .action(action)
                .details(details)
                .createdAt(OffsetDateTime.now())
                .build();
        jpaRepository.save(entity);
    }

    @Override
    public List<HistoriqueAction> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toVue)
                .collect(Collectors.toList());
    }

    @Override
    public List<HistoriqueAction> findByUtilisateur(String utilisateur) {
        return jpaRepository.findByUtilisateur(utilisateur).stream()
                .map(this::toVue)
                .collect(Collectors.toList());
    }

    private HistoriqueAction toVue(HistoriqueActionEntity e) {
        return new HistoriqueAction(
                e.getIdHistorique(),
                e.getUtilisateur(),
                e.getAction(),
                e.getDetails(),
                e.getCreatedAt());
    }
}

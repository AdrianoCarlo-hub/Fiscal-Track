package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {

    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    List<Notification> findAll();

    Page<Notification> findAll(Pageable pageable);

    List<Notification> findByNif(String nif);

    List<Notification> findByStatutEnvoi(String statutEnvoi);

    List<Notification> findEnAttenteAvant(OffsetDateTime date);

    /** Filtre par NIF / statut / canal (null = pas de filtre sur ce critere). */
    Page<Notification> rechercher(String nif, String statutEnvoi, String canalEnvoi, Pageable pageable);

    /** Notifications en ECHEC encore retentables (nb_tentatives < max). */
    List<Notification> findEchecsRetentables(int nbTentativesMax);

    /** Idempotence : existence d'une notification pour (objet, type, canal). */
    boolean existsByObjetAndTypeAndCanal(Long idObligationFiscale, Long idCompte,
                                         String typeRelance, String canalEnvoi);

    /** Nombre de notifications non lues sur le portail pour un NIF. */
    long countNonLues(String nif);

    /** Notifications du portail pour un NIF, triees de la plus recente a la plus ancienne. */
    List<Notification> findPortailByNif(String nif);

    void deleteById(Long id);
}

package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.application.port.out.NotificationRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.CanalEnvoi;
import mg.dgi.fiscaltrack.domain.enums.StatutEnvoi;
import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Creation idempotente de notifications.
 *
 * Une seule notification est creee par (objet concerne, type, canal) :
 * relancer plusieurs fois la generation ne cree aucun doublon.
 */
@Service
public class CreerNotificationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreerNotificationUseCase.class);

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final CanalNotificationService canalNotificationService;
    private final NotificationTemplateService templateService;
    private final Clock clock;

    public CreerNotificationUseCase(NotificationRepositoryPort notificationRepositoryPort,
                                    CanalNotificationService canalNotificationService,
                                    NotificationTemplateService templateService,
                                    Clock clock) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.canalNotificationService = canalNotificationService;
        this.templateService = templateService;
        this.clock = clock;
    }

    /**
     * Cree (sans doublon) une notification automatique sur chaque canal
     * disponible pour le contribuable.
     *
     * @return les notifications creees (jamais de doublon)
     */
    public List<Notification> creerAutomatique(TypeRelance type,
                                               ContexteNotification contexte,
                                               Long idObligationFiscale,
                                               Long idCompte) {
        Contribuable contribuable = canalNotificationService.contribuable(contexte.getNif())
                .orElse(null);
        List<CanalEnvoi> canaux = canalNotificationService.canauxPour(contribuable, type.estAvisLegal());

        List<Notification> creees = new ArrayList<>();
        for (CanalEnvoi canal : canaux) {
            Notification n = creer(type, contexte, idObligationFiscale, idCompte, canal);
            if (n != null) {
                creees.add(n);
            }
        }
        return creees;
    }

    /**
     * Cree une notification sur les canaux demandes (envoi manuel).
     * Pas de filtre de doublon : l'agent a explicitement demande l'envoi.
     */
    public List<Notification> creerManuel(TypeRelance type,
                                          ContexteNotification contexte,
                                          Long idObligationFiscale,
                                          Long idCompte,
                                          List<CanalEnvoi> canaux) {
        List<Notification> creees = new ArrayList<>();
        for (CanalEnvoi canal : canaux) {
            creees.add(creer(type, contexte, idObligationFiscale, idCompte, canal, true));
        }
        return creees;
    }

    private Notification creer(TypeRelance type,
                               ContexteNotification contexte,
                               Long idObligationFiscale,
                               Long idCompte,
                               CanalEnvoi canal) {
        return creer(type, contexte, idObligationFiscale, idCompte, canal, false);
    }

    private Notification creer(TypeRelance type,
                               ContexteNotification contexte,
                               Long idObligationFiscale,
                               Long idCompte,
                               CanalEnvoi canal,
                               boolean ignorerDoublon) {
        if (!ignorerDoublon
                && notificationRepositoryPort.existsByObjetAndTypeAndCanal(
                        idObligationFiscale, idCompte, type.getCode(), canal.name())) {
            logger.debug("[NOTIFICATION][IDEMPOTENCE] Doublon evite : objet={} type={} canal={}",
                    idObligationFiscale != null ? "obligation#" + idObligationFiscale
                            : (idCompte != null ? "compte#" + idCompte : "sans objet"),
                    type.getCode(), canal);
            return null;
        }

        ContenuMessage contenu = templateService.construire(type, contexte);
        OffsetDateTime maintenant = OffsetDateTime.now(clock);

        Notification notification = Notification.builder()
                .idObligationFiscale(idObligationFiscale)
                .idCompte(idCompte)
                .nif(contexte.getNif())
                .typeRelance(type)
                .messageContenu(contenu.corpsEmail())
                .objetEmail(contenu.sujet())
                .messageSms(contenu.corpsSms())
                .dateEnvoiPrevue(maintenant)
                .canalEnvoi(canal)
                .statutEnvoi(StatutEnvoi.EN_ATTENTE)
                .nbTentatives(0)
                .lu(Boolean.FALSE)
                .createdAt(maintenant)
                .build();
        return notificationRepositoryPort.save(notification);
    }
}

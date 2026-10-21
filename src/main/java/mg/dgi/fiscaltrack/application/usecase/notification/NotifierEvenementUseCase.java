package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Cree et met en file les notifications d'evenement (declaration validee ou
 * rejetee, paiement recu, penalite, amende).
 *
 * Contract : cette classe NE LEVE JAMAIS d'exception. Une panne d'envoi ou
 * de creation de notification n'annule ni ne modifie l'operation metier qui
 * l'a appelee.
 *
 * L'envoi n'est mis en file qu'apres le commit de la transaction en cours
 * (le scheduler journalise EN_ATTENTE, l'envoi asynchrone survit au commit).
 */
@Service
public class NotifierEvenementUseCase {

    private static final Logger logger = LoggerFactory.getLogger(NotifierEvenementUseCase.class);

    private final CreerNotificationUseCase creerNotificationUseCase;
    private final NotificationEnvoiAsyncService envoiAsyncService;

    public NotifierEvenementUseCase(CreerNotificationUseCase creerNotificationUseCase,
                                    NotificationEnvoiAsyncService envoiAsyncService) {
        this.creerNotificationUseCase = creerNotificationUseCase;
        this.envoiAsyncService = envoiAsyncService;
    }

    /**
     * Cree les notifications de l'evenement sur chaque canal disponible,
     * puis les met en file apres le commit (ou immediatement si hors
     * transaction). Toute erreur est capturee et journalisee.
     */
    public void notifier(TypeRelance type,
                         ContexteNotification contexte,
                         Long idObligationFiscale,
                         Long idCompte) {
        try {
            List<Notification> creees = creerNotificationUseCase.creerAutomatique(
                    type, contexte, idObligationFiscale, idCompte);
            if (creees.isEmpty()) {
                return;
            }
            List<Long> ids = creees.stream().map(Notification::getIdNotif).toList();
            enfilerApresTransaction(ids);
        } catch (Exception e) {
            logger.warn("[NOTIFICATION][EVENEMENT][{}] Creation impossible pour le NIF {} : {}",
                    type, contexte == null ? null : contexte.getNif(), e.getMessage());
        }
    }

    private void enfilerApresTransaction(List<Long> ids) {
        Runnable enfilement = () -> ids.forEach(envoiAsyncService::envoyerAsync);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enfilement.run();
                }
            });
        } else {
            enfilement.run();
        }
    }
}

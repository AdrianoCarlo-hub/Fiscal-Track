package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.application.port.out.NotificationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.CanalEnvoi;
import mg.dgi.fiscaltrack.domain.enums.StatutEnvoi;
import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import mg.dgi.fiscaltrack.domain.model.Notification;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GenererRappelUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final NotificationRepositoryPort notificationRepositoryPort;

    public GenererRappelUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                 NotificationRepositoryPort notificationRepositoryPort) {
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.notificationRepositoryPort = notificationRepositoryPort;
    }

    /**
     * RC17 : genere les rappels preventifs J-7 et J-1 pour les obligations
     * dont l'echeance approche. Les notifications sont creees au statut
     * EN_ATTENTE, elles seront envoyees par EnvoyerNotificationUseCase.
     */
    public List<Notification> execute(int nombreJoursAvantEcheance) {
        TypeRelance typeRelance = TypeRelance.fromCode("J-" + nombreJoursAvantEcheance);

        List<ObligationFiscale> obligations = obligationRepositoryPort
                .findEcheancesProches(java.time.LocalDate.now(), nombreJoursAvantEcheance);

        List<Notification> notifications = new ArrayList<>();
        for (ObligationFiscale obligation : obligations) {
            if (obligation.getStatutDeclaration() != mg.dgi.fiscaltrack.domain.enums.StatutDeclaration.ATTENTE) {
                continue;
            }

            String message = String.format(
                    "Rappel : votre obligation %s pour la periode %s arrive a echeance le %s.",
                    obligation.getCodeImpot(),
                    obligation.getPeriodeFiscale(),
                    obligation.getDateLimiteReelle());

            Notification notification = Notification.builder()
                    .idObligationFiscale(obligation.getIdObligationFiscale())
                    .nif(obligation.getNif())
                    .typeRelance(typeRelance)
                    .messageContenu(message)
                    .dateEnvoiPrevue(OffsetDateTime.now())
                    .canalEnvoi(CanalEnvoi.EMAIL)
                    .statutEnvoi(StatutEnvoi.EN_ATTENTE)
                    .build();

            notifications.add(notificationRepositoryPort.save(notification));
        }
        return notifications;
    }
}

package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.application.port.out.NotificationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.CanalEnvoi;
import mg.dgi.fiscaltrack.domain.enums.StatutDeclaration;
import mg.dgi.fiscaltrack.domain.enums.StatutEnvoi;
import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import mg.dgi.fiscaltrack.domain.model.Notification;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GenererAvisRetardUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final NotificationRepositoryPort notificationRepositoryPort;

    public GenererAvisRetardUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                      NotificationRepositoryPort notificationRepositoryPort) {
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.notificationRepositoryPort = notificationRepositoryPort;
    }

    /**
     * Genere les avis de retard J+1 : obligations dont la date limite reelle
     * est depassee depuis exactement 1 jour.
     */
    public List<Notification> execute() {
        LocalDate cible = LocalDate.now().minusDays(1);
        List<ObligationFiscale> obligations = obligationRepositoryPort.findAll().stream()
                .filter(o -> o.getStatutDeclaration() == StatutDeclaration.RETARD)
                .filter(o -> cible.equals(o.getDateLimiteReelle()))
                .toList();

        List<Notification> notifications = new ArrayList<>();
        for (ObligationFiscale o : obligations) {
            String message = String.format(
                    "Avis de retard : votre obligation %s pour la periode %s n'a pas ete deposee. Echeance depassee le %s.",
                    o.getCodeImpot(), o.getPeriodeFiscale(), o.getDateLimiteReelle());

            Notification notification = Notification.builder()
                    .idObligationFiscale(o.getIdObligationFiscale())
                    .nif(o.getNif())
                    .typeRelance(TypeRelance.J_PLUS_1)
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

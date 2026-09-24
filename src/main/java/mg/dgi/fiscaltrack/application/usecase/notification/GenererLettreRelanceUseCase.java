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
public class GenererLettreRelanceUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final NotificationRepositoryPort notificationRepositoryPort;

    public GenererLettreRelanceUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                         NotificationRepositoryPort notificationRepositoryPort) {
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.notificationRepositoryPort = notificationRepositoryPort;
    }

    /**
     * Genere les lettres de relance J+8 : obligations en RETARD depuis
     * exactement 8 jours.
     */
    public List<Notification> execute() {
        LocalDate cible = LocalDate.now().minusDays(8);
        List<ObligationFiscale> obligations = obligationRepositoryPort.findAll().stream()
                .filter(o -> o.getStatutDeclaration() == StatutDeclaration.RETARD)
                .filter(o -> cible.equals(o.getDateLimiteReelle()))
                .toList();

        List<Notification> notifications = new ArrayList<>();
        for (ObligationFiscale o : obligations) {
            String message = String.format(
                    "Lettre de relance : votre obligation %s pour la periode %s reste impayee apres 8 jours. Un delai franc de 8 jours vous est accorde pour regulariser.",
                    o.getCodeImpot(), o.getPeriodeFiscale());

            Notification notification = Notification.builder()
                    .idObligationFiscale(o.getIdObligationFiscale())
                    .nif(o.getNif())
                    .typeRelance(TypeRelance.J_PLUS_8)
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

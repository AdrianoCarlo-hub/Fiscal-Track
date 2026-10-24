package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.StatutDeclaration;
import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import mg.dgi.fiscaltrack.domain.model.Notification;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class GenererAvisRetardUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final ContexteObligationService contexteObligationService;
    private final CreerNotificationUseCase creerNotificationUseCase;
    private final Clock clock;

    public GenererAvisRetardUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                    ContexteObligationService contexteObligationService,
                                    CreerNotificationUseCase creerNotificationUseCase,
                                    Clock clock) {
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.contexteObligationService = contexteObligationService;
        this.creerNotificationUseCase = creerNotificationUseCase;
        this.clock = clock;
    }

    /**
     * Genere les avis de retard J+1 : obligations dont la date limite reelle
     * est depassee depuis exactement 1 jour (date locale du fuseau de
     * l'application). L'avis est un avis legal : toujours publie sur le
     * portail, en plus des canaux disponibles.
     */
    public List<Notification> execute() {
        LocalDate cible = LocalDate.now(clock).minusDays(1);
        List<ObligationFiscale> obligations = obligationRepositoryPort.findAll().stream()
                .filter(o -> o.getStatutDeclaration() == StatutDeclaration.RETARD)
                .filter(o -> cible.equals(o.getDateLimiteReelle()))
                .toList();

        List<Notification> notifications = new ArrayList<>();
        for (ObligationFiscale o : obligations) {
            ContexteNotification contexte = contexteObligationService.depuis(o);
            notifications.addAll(creerNotificationUseCase.creerAutomatique(
                    TypeRelance.J_PLUS_1, contexte, o.getIdObligationFiscale(), null));
        }
        return notifications;
    }
}

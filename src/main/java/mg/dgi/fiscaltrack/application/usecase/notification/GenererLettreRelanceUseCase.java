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
public class GenererLettreRelanceUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final ContexteObligationService contexteObligationService;
    private final CreerNotificationUseCase creerNotificationUseCase;
    private final Clock clock;

    public GenererLettreRelanceUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                       ContexteObligationService contexteObligationService,
                                       CreerNotificationUseCase creerNotificationUseCase,
                                       Clock clock) {
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.contexteObligationService = contexteObligationService;
        this.creerNotificationUseCase = creerNotificationUseCase;
        this.clock = clock;
    }

    /**
     * Genere les lettres de relance J+8 : obligations en RETARD depuis
     * exactement 8 jours. Avis legal : toujours publie sur le portail,
     * en plus des canaux disponibles.
     */
    public List<Notification> execute() {
        LocalDate cible = LocalDate.now(clock).minusDays(8);
        List<ObligationFiscale> obligations = obligationRepositoryPort.findAll().stream()
                .filter(o -> o.getStatutDeclaration() == StatutDeclaration.RETARD)
                .filter(o -> cible.equals(o.getDateLimiteReelle()))
                .toList();

        List<Notification> notifications = new ArrayList<>();
        for (ObligationFiscale o : obligations) {
            ContexteNotification contexte = contexteObligationService.depuis(o);
            notifications.addAll(creerNotificationUseCase.creerAutomatique(
                    TypeRelance.J_PLUS_8, contexte, o.getIdObligationFiscale(), null));
        }
        return notifications;
    }
}

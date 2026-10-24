package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.TypeImpotRepositoryPort;
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
public class GenererRappelUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final TypeImpotRepositoryPort typeImpotRepositoryPort;
    private final CreerNotificationUseCase creerNotificationUseCase;
    private final Clock clock;

    public GenererRappelUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                TypeImpotRepositoryPort typeImpotRepositoryPort,
                                CreerNotificationUseCase creerNotificationUseCase,
                                Clock clock) {
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.typeImpotRepositoryPort = typeImpotRepositoryPort;
        this.creerNotificationUseCase = creerNotificationUseCase;
        this.clock = clock;
    }

    /**
     * RC17 : genere les rappels preventifs J-7 et J-1 pour les obligations
     * dont l'echeance approche (date du jour dans le fuseau de l'application).
     * Les notifications sont creees au statut EN_ATTENTE sur chaque canal
     * disponible, sans doublon.
     */
    public List<Notification> execute(int nombreJoursAvantEcheance) {
        TypeRelance typeRelance = TypeRelance.fromCode("J-" + nombreJoursAvantEcheance);

        LocalDate aujourdHui = LocalDate.now(clock);
        List<ObligationFiscale> obligations = obligationRepositoryPort
                .findEcheancesProches(aujourdHui, nombreJoursAvantEcheance);

        List<Notification> notifications = new ArrayList<>();
        for (ObligationFiscale obligation : obligations) {
            if (obligation.getStatutDeclaration() != StatutDeclaration.ATTENTE) {
                continue;
            }

            ContexteNotification contexte = ContexteNotification.builder()
                    .nif(obligation.getNif())
                    .codeImpot(obligation.getCodeImpot())
                    .nomImpot(nomImpot(obligation.getCodeImpot()))
                    .periode(obligation.getPeriodeFiscale())
                    .echeance(obligation.getDateLimiteReelle())
                    .build();

            notifications.addAll(creerNotificationUseCase.creerAutomatique(
                    typeRelance, contexte, obligation.getIdObligationFiscale(), null));
        }
        return notifications;
    }

    private String nomImpot(String codeImpot) {
        return typeImpotRepositoryPort.findByCodeImpot(codeImpot)
                .map(t -> t.getNomImpot())
                .orElse(codeImpot);
    }
}

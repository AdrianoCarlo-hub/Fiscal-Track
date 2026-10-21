package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.application.usecase.notification.ContexteNotification;
import mg.dgi.fiscaltrack.application.usecase.notification.ContexteObligationService;
import mg.dgi.fiscaltrack.application.usecase.notification.NotifierEvenementUseCase;
import mg.dgi.fiscaltrack.domain.enums.StatutValidation;
import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VerifierDeclarationUseCase {

    private final DeclarationRepositoryPort repositoryPort;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    private ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private ContexteObligationService contexteObligationService;
    private NotifierEvenementUseCase notifierEvenementUseCase;

    public VerifierDeclarationUseCase(DeclarationRepositoryPort repositoryPort,
                                        EnregistrerActionUseCase enregistrerActionUseCase) {
        this.repositoryPort = repositoryPort;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    /** Injection optionnelle des dependances de notification (null = pas d'envoi). */
    @Autowired(required = false)
    public void setObligationRepositoryPort(ObligationFiscaleRepositoryPort obligationRepositoryPort) {
        this.obligationRepositoryPort = obligationRepositoryPort;
    }

    @Autowired(required = false)
    public void setContexteObligationService(ContexteObligationService contexteObligationService) {
        this.contexteObligationService = contexteObligationService;
    }

    @Autowired(required = false)
    public void setNotifierEvenementUseCase(NotifierEvenementUseCase notifierEvenementUseCase) {
        this.notifierEvenementUseCase = notifierEvenementUseCase;
    }

    public Declaration valider(Long idDeclaration) {
        Declaration d = changerStatut(idDeclaration, StatutValidation.VALIDEE);
        enregistrerActionUseCase.execute("AGENT_GESTION", "VALIDATION_DECLARATION",
                "Declaration #" + idDeclaration + " validee");
        notifier(d, null, TypeRelance.DECLARATION_VALIDEE);
        return d;
    }

    /**
     * Rejet d'une declaration : le motif est obligatoire, il est enregistre
     * sur la declaration et affiche au contribuable.
     */
    public Declaration rejeter(Long idDeclaration, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("Le motif du rejet est obligatoire");
        }

        Declaration d = changerStatut(idDeclaration, StatutValidation.REJETEE);
        d.setMotifRejet(motif.trim());
        Declaration sauvegardee = repositoryPort.save(d);

        enregistrerActionUseCase.execute("AGENT_GESTION", "REJET_DECLARATION",
                "Declaration #" + idDeclaration + " rejetee : " + motif.trim());
        notifier(sauvegardee, motif.trim(), TypeRelance.DECLARATION_REJETEE);
        return sauvegardee;
    }

    /** Notification d'evenement : jamais d'effet sur le resultat de la validation. */
    private void notifier(Declaration declaration, String motif, TypeRelance type) {
        if (notifierEvenementUseCase == null) {
            return;
        }
        try {
            notifierEvenementUseCase.notifier(
                    type, contexteDeclaration(declaration, motif),
                    declaration.getIdObligationFiscale(), null);
        } catch (Exception ignored) {
            // une panne de notification n'annule jamais la validation
        }
    }

    private ContexteNotification contexteDeclaration(Declaration declaration, String motif) {
        if (obligationRepositoryPort == null || contexteObligationService == null) {
            return ContexteNotification.builder()
                    .montantRetenu(declaration.getImpotPrincipalCalcule())
                    .motif(motif)
                    .build();
        }
        ObligationFiscale obligation = obligationRepositoryPort
                .findById(declaration.getIdObligationFiscale())
                .orElse(null);
        if (obligation == null) {
            return ContexteNotification.builder()
                    .montantRetenu(declaration.getImpotPrincipalCalcule())
                    .motif(motif)
                    .build();
        }
        ContexteNotification base = contexteObligationService.depuis(obligation);
        return ContexteNotification.builder()
                .nif(base.getNif())
                .codeImpot(base.getCodeImpot())
                .nomImpot(base.getNomImpot())
                .periode(base.getPeriode())
                .echeance(base.getEcheance())
                .montantRetenu(declaration.getImpotPrincipalCalcule())
                .motif(motif)
                .build();
    }

    private Declaration changerStatut(Long idDeclaration, StatutValidation nouveauStatut) {
        Declaration declaration = repositoryPort.findById(idDeclaration)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Declaration introuvable : " + idDeclaration));

        if (declaration.getStatutValidation() != StatutValidation.EN_ATTENTE_VALIDATION) {
            throw new IllegalArgumentException(
                    "Seule une declaration en attente peut etre modifiee");
        }

        declaration.setStatutValidation(nouveauStatut);
        return repositoryPort.save(declaration);
    }
}

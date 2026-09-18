package mg.dgi.fiscaltrack.interfaces.controller;

import mg.dgi.fiscaltrack.application.usecase.notification.ConsulterNotificationsUseCase;
import mg.dgi.fiscaltrack.application.usecase.notification.EnvoyerNotificationUseCase;
import mg.dgi.fiscaltrack.application.usecase.notification.GenererRappelUseCase;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final GenererRappelUseCase genererRappelUseCase;
    private final EnvoyerNotificationUseCase envoyerUseCase;
    private final ConsulterNotificationsUseCase consulterUseCase;

    public NotificationController(GenererRappelUseCase genererRappelUseCase,
                                    EnvoyerNotificationUseCase envoyerUseCase,
                                    ConsulterNotificationsUseCase consulterUseCase) {
        this.genererRappelUseCase = genererRappelUseCase;
        this.envoyerUseCase = envoyerUseCase;
        this.consulterUseCase = consulterUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public List<Map<String, Object>> lister() {
        return consulterUseCase.toutes().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> parId(@PathVariable Long id) {
        return toMap(consulterUseCase.parId(id));
    }

    @GetMapping("/contribuable/{nif}")
    public List<Map<String, Object>> parContribuable(@PathVariable String nif) {
        return consulterUseCase.parContribuable(nif).stream().map(this::toMap).toList();
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','RESPONSABLE','ADMIN')")
    public List<Map<String, Object>> parStatut(@PathVariable String statut) {
        return consulterUseCase.parStatut(statut).stream().map(this::toMap).toList();
    }

    /**
     * Genere les rappels J-7 (ou J-1) pour les obligations dont l'echeance
     * approche. Utilise principalement par le scheduler et en test manuel.
     */
    @PostMapping("/generer-rappels")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','RESPONSABLE','ADMIN')")
    public ResponseEntity<Map<String, Object>> genererRappels(
            @RequestParam(defaultValue = "7") int jours) {
        List<Notification> generees = genererRappelUseCase.execute(jours);
        return ResponseEntity.ok(Map.of(
                "nombreRappelsGeneres", generees.size(),
                "typeRelance", "J-" + jours
        ));
    }

    /**
     * Envoie toutes les notifications en attente.
     */
    @PostMapping("/envoyer-en-attente")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','ADMIN')")
    public ResponseEntity<Map<String, Object>> envoyerEnAttente() {
        int nombreEnvoyes = envoyerUseCase.execute();
        return ResponseEntity.ok(Map.of("nombreEnvoyes", nombreEnvoyes));
    }

    /**
     * Envoie une notification specifique.
     */
    @PostMapping("/{id}/envoyer")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','ADMIN')")
    public ResponseEntity<Map<String, Object>> envoyerUne(@PathVariable Long id) {
        return ResponseEntity.ok(toMap(envoyerUseCase.envoyerUne(id)));
    }

    private Map<String, Object> toMap(Notification n) {
        return Map.of(
                "idNotif", n.getIdNotif(),
                "idObligationFiscale", n.getIdObligationFiscale() == null ? 0 : n.getIdObligationFiscale(),
                "idCompte", n.getIdCompte() == null ? 0 : n.getIdCompte(),
                "nif", n.getNif(),
                "idAgent", n.getIdAgent() == null ? "" : n.getIdAgent(),
                "typeRelance", n.getTypeRelance().getCode(),
                "messageContenu", n.getMessageContenu(),
                "dateEnvoiPrevue", n.getDateEnvoiPrevue().toString(),
                "dateEnvoiEffective", n.getDateEnvoiEffective() == null ? "" : n.getDateEnvoiEffective().toString(),
                "canalEnvoi", n.getCanalEnvoi().name(),
                "statutEnvoi", n.getStatutEnvoi().name()
        );
    }
}

package mg.dgi.fiscaltrack.interfaces.controller;

import mg.dgi.fiscaltrack.application.usecase.notification.ConsulterNotificationsUseCase;
import mg.dgi.fiscaltrack.application.usecase.notification.EnvoyerNotificationUseCase;
import mg.dgi.fiscaltrack.application.usecase.notification.GenererRappelUseCase;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    @PostMapping("/envoyer-en-attente")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','ADMIN')")
    public ResponseEntity<Map<String, Object>> envoyerEnAttente() {
        int nombreEnvoyes = envoyerUseCase.execute();
        return ResponseEntity.ok(Map.of("nombreEnvoyes", nombreEnvoyes));
    }

    @PostMapping("/{id}/envoyer")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','ADMIN')")
    public ResponseEntity<Map<String, Object>> envoyerUne(@PathVariable Long id) {
        return ResponseEntity.ok(toMap(envoyerUseCase.envoyerUne(id)));
    }

    private Map<String, Object> toMap(Notification n) {
        Map<String, Object> map = new HashMap<>();
        map.put("idNotif", n.getIdNotif());
        map.put("idObligationFiscale", n.getIdObligationFiscale() == null ? 0 : n.getIdObligationFiscale());
        map.put("idCompte", n.getIdCompte() == null ? 0 : n.getIdCompte());
        map.put("nif", n.getNif());
        map.put("idAgent", n.getIdAgent() == null ? "" : n.getIdAgent());
        map.put("typeRelance", n.getTypeRelance().getCode());
        map.put("messageContenu", n.getMessageContenu());
        map.put("dateEnvoiPrevue", n.getDateEnvoiPrevue().toString());
        map.put("dateEnvoiEffective", n.getDateEnvoiEffective() == null ? "" : n.getDateEnvoiEffective().toString());
        map.put("canalEnvoi", n.getCanalEnvoi().name());
        map.put("statutEnvoi", n.getStatutEnvoi().name());
        return map;
    }
}

package mg.dgi.fiscaltrack.interfaces.controller;

import mg.dgi.fiscaltrack.application.usecase.notification.ConsulterNotificationsUseCase;
import mg.dgi.fiscaltrack.application.usecase.notification.EnvoyerNotificationUseCase;
import mg.dgi.fiscaltrack.application.usecase.notification.GenererRappelUseCase;
import mg.dgi.fiscaltrack.application.usecase.notification.MarquerNotificationLueUseCase;
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
    private final MarquerNotificationLueUseCase marquerLueUseCase;

    public NotificationController(GenererRappelUseCase genererRappelUseCase,
                                    EnvoyerNotificationUseCase envoyerUseCase,
                                    ConsulterNotificationsUseCase consulterUseCase,
                                    MarquerNotificationLueUseCase marquerLueUseCase) {
        this.genererRappelUseCase = genererRappelUseCase;
        this.envoyerUseCase = envoyerUseCase;
        this.consulterUseCase = consulterUseCase;
        this.marquerLueUseCase = marquerLueUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public List<Map<String, Object>> lister() {
        return consulterUseCase.toutes().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> parId(@PathVariable Long id) {
        Notification notification = consulterUseCase.parId(id);
        UtilisateurConnecte.verifierPropriete(notification.getNif(), "cette notification");
        return toMap(notification);
    }

    @GetMapping("/contribuable/{nif}")
    public List<Map<String, Object>> parContribuable(@PathVariable String nif) {
        UtilisateurConnecte.verifierPropriete(nif, "ces notifications");
        return consulterUseCase.parContribuable(nif).stream().map(this::toMap).toList();
    }

    /** Boite de reception du portail : notifications PORTAIL du contribuable, recentes d'abord. */
    @GetMapping("/contribuable/{nif}/portail")
    public List<Map<String, Object>> portail(@PathVariable String nif) {
        UtilisateurConnecte.verifierPropriete(nif, "cette boite de reception");
        return consulterUseCase.parContribuable(nif).stream()
                .filter(n -> n.getCanalEnvoi() != null
                        && "PORTAIL".equals(n.getCanalEnvoi().name()))
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) {
                        return 0;
                    }
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toMap)
                .toList();
    }

    /** Nombre de notifications du portail non lues (badge de navigation). */
    @GetMapping("/contribuable/{nif}/non-lues")
    public Map<String, Object> nonLues(@PathVariable String nif) {
        UtilisateurConnecte.verifierPropriete(nif, "ce compteur de notifications");
        long nombre = consulterUseCase.parContribuable(nif).stream()
                .filter(n -> n.getCanalEnvoi() != null
                        && "PORTAIL".equals(n.getCanalEnvoi().name()))
                .filter(n -> !Boolean.TRUE.equals(n.getLu()))
                .count();
        return Map.of("nif", nif, "nombreNonLues", nombre);
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

    /** Le contribuable marque comme lue une notification de son propre portail. */
    @PutMapping("/{id}/lu")
    public Map<String, Object> marquerLue(@PathVariable Long id) {
        Notification notification = consulterUseCase.parId(id);
        UtilisateurConnecte.verifierPropriete(notification.getNif(), "cette notification");
        return toMap(marquerLueUseCase.execute(id));
    }

    private Map<String, Object> toMap(Notification n) {
        Map<String, Object> map = new HashMap<>();
        map.put("idNotif", n.getIdNotif());
        map.put("idObligationFiscale", n.getIdObligationFiscale() == null ? 0 : n.getIdObligationFiscale());
        map.put("idCompte", n.getIdCompte() == null ? 0 : n.getIdCompte());
        map.put("nif", n.getNif());
        map.put("idAgent", n.getIdAgent() == null ? "" : n.getIdAgent());
        map.put("typeRelance", n.getTypeRelance() == null ? "" : n.getTypeRelance().getCode());
        map.put("objetEmail", n.getObjetEmail() == null ? "" : n.getObjetEmail());
        map.put("messageContenu", n.getMessageContenu());
        map.put("messageSms", n.getMessageSms() == null ? "" : n.getMessageSms());
        map.put("dateEnvoiPrevue", n.getDateEnvoiPrevue() == null ? "" : n.getDateEnvoiPrevue().toString());
        map.put("dateEnvoiEffective", n.getDateEnvoiEffective() == null ? "" : n.getDateEnvoiEffective().toString());
        map.put("canalEnvoi", n.getCanalEnvoi() == null ? "" : n.getCanalEnvoi().name());
        map.put("statutEnvoi", n.getStatutEnvoi() == null ? "" : n.getStatutEnvoi().name());
        map.put("nbTentatives", n.getNbTentatives() == null ? 0 : n.getNbTentatives());
        map.put("erreurEnvoi", n.getErreurEnvoi() == null ? "" : n.getErreurEnvoi());
        map.put("lu", Boolean.TRUE.equals(n.getLu()));
        return map;
    }
}

package mg.dgi.fiscaltrack.interfaces.controller;

import mg.dgi.fiscaltrack.application.usecase.admin.GererJoursFeriesUseCase;
import mg.dgi.fiscaltrack.application.usecase.notification.ContenuMessage;
import mg.dgi.fiscaltrack.application.usecase.notification.ContexteNotification;
import mg.dgi.fiscaltrack.application.usecase.notification.NotificationTemplateService;
import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import mg.dgi.fiscaltrack.application.usecase.admin.GererUtilisateursUseCase;
import mg.dgi.fiscaltrack.domain.enums.Division;
import mg.dgi.fiscaltrack.domain.enums.RoleSecurite;
import mg.dgi.fiscaltrack.domain.model.AgentFiscal;
import mg.dgi.fiscaltrack.domain.model.JourFerie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final GererUtilisateursUseCase utilisateursUseCase;
    private final GererJoursFeriesUseCase joursFeriesUseCase;
    private final NotificationTemplateService templateService;

    public AdminController(GererUtilisateursUseCase utilisateursUseCase,
                            GererJoursFeriesUseCase joursFeriesUseCase,
                            NotificationTemplateService templateService) {
        this.utilisateursUseCase = utilisateursUseCase;
        this.joursFeriesUseCase = joursFeriesUseCase;
        this.templateService = templateService;
    }

    // ===================== MODELES DE NOTIFICATION =====================

    /**
     * Apercu en lecture seule des modeles e-mail, SMS et portail.
     * Les textes sont rendus avec des donnees d'exemple ; aucun envoi.
     */
    @GetMapping("/modeles-notifications")
    public List<Map<String, Object>> modelesNotifications() {
        ContexteNotification exemple = ContexteNotification.builder()
                .nif("1234567890")
                .raisonSociale("SARL Exemple")
                .codeImpot("IR")
                .nomImpot("Impôt sur les Revenus")
                .periode("2026")
                .echeance(LocalDate.of(2026, 5, 15))
                .montantDu(new BigDecimal("1234000"))
                .montantPaye(new BigDecimal("500000"))
                .montantRetenu(new BigDecimal("1234000"))
                .reference("TXN-2026-0001")
                .motif("Pièce justificative manquante")
                .nature("Retard de dépôt")
                .montant(new BigDecimal("50000"))
                .messageLibre("Votre dossier a bien été mis à jour.")
                .build();

        List<Map<String, Object>> modeles = new ArrayList<>();
        for (TypeRelance type : TypeRelance.values()) {
            ContenuMessage contenu = templateService.construire(type, exemple);
            modeles.add(modele(type, "EMAIL", contenu.sujet(), contenu.corpsEmail()));
            modeles.add(modele(type, "SMS", "", contenu.corpsSms()));
            modeles.add(modele(type, "PORTAIL", contenu.sujet(), contenu.corpsEmail()));
        }
        return modeles;
    }

    private Map<String, Object> modele(TypeRelance type, String canal, String objet, String corps) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("typeRelance", type.getCode());
        map.put("canal", canal);
        map.put("objet", objet);
        map.put("corps", corps);
        map.put("longueurCaracteres", corps == null ? 0 : corps.length());
        return map;
    }

    // ===================== UTILISATEURS =====================

    @GetMapping("/utilisateurs")
    public List<Map<String, Object>> listerAgents() {
        return utilisateursUseCase.tous().stream().map(this::toAgentMap).toList();
    }

    @GetMapping("/utilisateurs/{id}")
    public Map<String, Object> agentParId(@PathVariable String id) {
        return toAgentMap(utilisateursUseCase.parId(id));
    }

    @PostMapping("/utilisateurs")
    public ResponseEntity<Map<String, Object>> ajouterAgent(@RequestBody Map<String, String> request,
                                                            java.security.Principal principal) {
        AgentFiscal agent = AgentFiscal.builder()
                .idAgent(request.get("idAgent"))
                .nomAgent(request.get("nomAgent"))
                .prenomAgent(request.get("prenomAgent"))
                .emailAgent(request.get("emailAgent"))
                .telephoneAgent(request.get("telephoneAgent"))
                .motDePasseHashAgent(request.get("motDePasseHashAgent"))
                .division(Division.valueOf(request.get("division")))
                .roleSecurite(RoleSecurite.valueOf(request.get("roleSecurite")))
                .build();
        String auteur = principal == null ? null : principal.getName();
        return ResponseEntity.ok(toAgentMap(utilisateursUseCase.ajouter(agent, auteur)));
    }

    @PutMapping("/utilisateurs/{id}")
    public ResponseEntity<Map<String, Object>> modifierAgent(@PathVariable String id,
                                                             @RequestBody Map<String, String> request,
                                                             java.security.Principal principal) {
        AgentFiscal donnees = AgentFiscal.builder()
                .nomAgent(request.get("nomAgent"))
                .prenomAgent(request.get("prenomAgent"))
                .emailAgent(request.get("emailAgent"))
                .telephoneAgent(request.get("telephoneAgent"))
                .motDePasseHashAgent(request.get("motDePasseHashAgent"))
                .division(parseDivision(request.get("division")))
                .roleSecurite(parseRole(request.get("roleSecurite")))
                .build();
        String auteur = principal == null ? null : principal.getName();
        return ResponseEntity.ok(toAgentMap(utilisateursUseCase.modifier(id, donnees, auteur)));
    }

    @DeleteMapping("/utilisateurs/{id}")
    public ResponseEntity<Void> supprimerAgent(@PathVariable String id,
                                               java.security.Principal principal) {
        utilisateursUseCase.supprimer(id, principal == null ? null : principal.getName());
        return ResponseEntity.noContent().build();
    }

    private Division parseDivision(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Division.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private RoleSecurite parseRole(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return RoleSecurite.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ===================== JOURS FERIES =====================

    @GetMapping("/jours-feries")
    public List<Map<String, Object>> listerJoursFeries() {
        return joursFeriesUseCase.tous().stream().map(this::toJourMap).toList();
    }

    @GetMapping("/jours-feries/{annee}")
    public List<Map<String, Object>> joursFeriesParAnnee(@PathVariable int annee) {
        return joursFeriesUseCase.parAnnee(annee).stream().map(this::toJourMap).toList();
    }

    @PostMapping("/jours-feries")
    public ResponseEntity<Map<String, Object>> ajouterJourFerie(@RequestBody Map<String, String> request) {
        LocalDate date = LocalDate.parse(request.get("dateFerie"));
        String description = request.get("description");
        return ResponseEntity.ok(toJourMap(joursFeriesUseCase.ajouter(date, description)));
    }

    @DeleteMapping("/jours-feries/{id}")
    public ResponseEntity<Void> supprimerJourFerie(@PathVariable Long id) {
        joursFeriesUseCase.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    // ===================== MAPPERS =====================

    private Map<String, Object> toAgentMap(AgentFiscal a) {
        return Map.of(
                "idAgent", a.getIdAgent(),
                "nomAgent", a.getNomAgent(),
                "prenomAgent", a.getPrenomAgent(),
                "emailAgent", a.getEmailAgent(),
                "telephoneAgent", a.getTelephoneAgent() == null ? "" : a.getTelephoneAgent(),
                "division", a.getDivision().name(),
                "roleSecurite", a.getRoleSecurite().name()
        );
    }

    private Map<String, Object> toJourMap(JourFerie j) {
        return Map.of(
                "idJourFerie", j.getIdJourFerie(),
                "dateFerie", j.getDateFerie().toString(),
                "description", j.getDescription() == null ? "" : j.getDescription()
        );
    }
}

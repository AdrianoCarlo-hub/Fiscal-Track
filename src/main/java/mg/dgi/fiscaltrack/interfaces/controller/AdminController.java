package mg.dgi.fiscaltrack.interfaces.controller;

import mg.dgi.fiscaltrack.application.usecase.admin.GererJoursFeriesUseCase;
import mg.dgi.fiscaltrack.application.usecase.admin.GererUtilisateursUseCase;
import mg.dgi.fiscaltrack.domain.enums.Division;
import mg.dgi.fiscaltrack.domain.enums.RoleSecurite;
import mg.dgi.fiscaltrack.domain.model.AgentFiscal;
import mg.dgi.fiscaltrack.domain.model.JourFerie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final GererUtilisateursUseCase utilisateursUseCase;
    private final GererJoursFeriesUseCase joursFeriesUseCase;

    public AdminController(GererUtilisateursUseCase utilisateursUseCase,
                            GererJoursFeriesUseCase joursFeriesUseCase) {
        this.utilisateursUseCase = utilisateursUseCase;
        this.joursFeriesUseCase = joursFeriesUseCase;
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
    public ResponseEntity<Map<String, Object>> ajouterAgent(@RequestBody Map<String, String> request) {
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
        return ResponseEntity.ok(toAgentMap(utilisateursUseCase.ajouter(agent)));
    }

    @DeleteMapping("/utilisateurs/{id}")
    public ResponseEntity<Void> supprimerAgent(@PathVariable String id) {
        utilisateursUseCase.supprimer(id);
        return ResponseEntity.noContent().build();
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

package mg.dgi.fiscaltrack.interfaces.controller;

import mg.dgi.fiscaltrack.infrastructure.scheduler.DetectionRetardScheduler;
import mg.dgi.fiscaltrack.infrastructure.scheduler.GenererObligationsScheduler;
import mg.dgi.fiscaltrack.infrastructure.scheduler.RappelEcheanceScheduler;
import mg.dgi.fiscaltrack.infrastructure.scheduler.SuiviRecouvrementScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/scheduler")
@PreAuthorize("hasRole('ADMIN')")
public class SchedulerController {

    private final DetectionRetardScheduler detectionRetardScheduler;
    private final RappelEcheanceScheduler rappelEcheanceScheduler;
    private final SuiviRecouvrementScheduler suiviRecouvrementScheduler;
    private final GenererObligationsScheduler genererObligationsScheduler;

    public SchedulerController(DetectionRetardScheduler detectionRetardScheduler,
                                 RappelEcheanceScheduler rappelEcheanceScheduler,
                                 SuiviRecouvrementScheduler suiviRecouvrementScheduler,
                                 GenererObligationsScheduler genererObligationsScheduler) {
        this.detectionRetardScheduler = detectionRetardScheduler;
        this.rappelEcheanceScheduler = rappelEcheanceScheduler;
        this.suiviRecouvrementScheduler = suiviRecouvrementScheduler;
        this.genererObligationsScheduler = genererObligationsScheduler;
    }

    @PostMapping("/{nom}/declencher")
    public ResponseEntity<Map<String, Object>> declencher(@PathVariable String nom) {
        switch (nom) {
            case "detection-retards":
                detectionRetardScheduler.executerDetectionRetards();
                return ResponseEntity.ok(Map.of("message", "Detection retards declenchee"));
            case "rappels-echeances":
                rappelEcheanceScheduler.executerRappelsQuotidiens();
                return ResponseEntity.ok(Map.of("message", "Rappels declenches"));
            case "suivi-recouvrement":
                suiviRecouvrementScheduler.executerSuiviRecouvrement();
                return ResponseEntity.ok(Map.of("message", "Suivi recouvrement declenche"));
            case "generer-obligations-mensuelles":
                int mensuelles = genererObligationsScheduler.declencherMensuelles();
                return ResponseEntity.ok(Map.of(
                        "message", "Obligations mensuelles generees",
                        "nombreGenerees", mensuelles));
            case "generer-obligations-annuelles":
                int annuelles = genererObligationsScheduler.declencherAnnuelles();
                return ResponseEntity.ok(Map.of(
                        "message", "Obligations annuelles generees",
                        "nombreGenerees", annuelles));
            default:
                return ResponseEntity.badRequest().body(
                        Map.of("erreur", "Scheduler inconnu : " + nom));
        }
    }
}

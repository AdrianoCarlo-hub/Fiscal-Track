package mg.dgi.fiscaltrack.interfaces.controller;

import mg.dgi.fiscaltrack.application.usecase.bi.GenererRapportUseCase;
import mg.dgi.fiscaltrack.application.usecase.bi.ObtenirIndicateursUseCase;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ObtenirIndicateursUseCase obtenirIndicateursUseCase;
    private final GenererRapportUseCase genererRapportUseCase;

    public DashboardController(ObtenirIndicateursUseCase obtenirIndicateursUseCase,
                                GenererRapportUseCase genererRapportUseCase) {
        this.obtenirIndicateursUseCase = obtenirIndicateursUseCase;
        this.genererRapportUseCase = genererRapportUseCase;
    }

    /**
     * KPI globaux du tableau de bord (Direction, Responsable, Admin).
     */
    @GetMapping("/indicateurs")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMIN','DIRECTEUR')")
    public Map<String, Object> indicateurs() {
        return obtenirIndicateursUseCase.execute();
    }

    /**
     * Top 10 des plus gros restes a recouvrer.
     */
    @GetMapping("/top10-restes")
    @PreAuthorize("hasAnyRole('RESPONSABLE','AGENT_RECETTE','ADMIN','DIRECTEUR')")
    public List<Map<String, Object>> top10Restes() {
        return genererRapportUseCase.top10RestesARecouvrer().stream()
                .map(this::toMap)
                .toList();
    }

    private Map<String, Object> toMap(CompteCourantFiscal c) {
        return Map.of(
                "idCompte", c.getIdCompte(),
                "nif", c.getNif(),
                "montantPrincipal", c.getMontantPrincipal(),
                "montantPenalites", c.getMontantPenalites(),
                "resteARecouvrer", c.getResteARecouvrer() == null ? 0 : c.getResteARecouvrer(),
                "statutRecouvrement", c.getStatutRecouvrement().name()
        );
    }
}

package mg.dgi.fiscaltrack.interfaces.controller;

import mg.dgi.fiscaltrack.application.usecase.obligation.AffecterObligationAgentUseCase;
import mg.dgi.fiscaltrack.application.usecase.obligation.ConsulterObligationsUseCase;
import mg.dgi.fiscaltrack.application.usecase.obligation.GenererObligationsUseCase;
import mg.dgi.fiscaltrack.application.usecase.obligation.IdentifierRetardsUseCase;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.interfaces.dto.response.ObligationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/obligations")
public class ObligationFiscaleController {

    private final GenererObligationsUseCase genererUseCase;
    private final ConsulterObligationsUseCase consulterUseCase;
    private final AffecterObligationAgentUseCase affecterUseCase;
    private final IdentifierRetardsUseCase identifierRetardsUseCase;

    public ObligationFiscaleController(GenererObligationsUseCase genererUseCase,
                                        ConsulterObligationsUseCase consulterUseCase,
                                        AffecterObligationAgentUseCase affecterUseCase,
                                        IdentifierRetardsUseCase identifierRetardsUseCase) {
        this.genererUseCase = genererUseCase;
        this.consulterUseCase = consulterUseCase;
        this.affecterUseCase = affecterUseCase;
        this.identifierRetardsUseCase = identifierRetardsUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public List<ObligationResponse> lister() {
        return consulterUseCase.toutes().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ObligationResponse parId(@PathVariable Long id) {
        return toResponse(consulterUseCase.parId(id));
    }

    @GetMapping("/contribuable/{nif}")
    public List<ObligationResponse> parContribuable(@PathVariable String nif) {
        return consulterUseCase.parContribuable(nif).stream().map(this::toResponse).toList();
    }

    @GetMapping("/agent/{idAgent}")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','RESPONSABLE','ADMIN')")
    public List<ObligationResponse> parAgent(@PathVariable String idAgent) {
        return consulterUseCase.parAgent(idAgent).stream().map(this::toResponse).toList();
    }

    @GetMapping("/retards")
    @PreAuthorize("hasAnyRole('AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public List<ObligationResponse> retards() {
        return identifierRetardsUseCase.execute().stream().map(this::toResponse).toList();
    }

    @GetMapping("/echeances-proches")
    public List<ObligationResponse> echeancesProches(@RequestParam(defaultValue = "7") int jours) {
        return identifierRetardsUseCase.echeancesProches(jours).stream().map(this::toResponse).toList();
    }

    @PostMapping("/generer")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','ADMIN')")
    public ResponseEntity<ObligationResponse> generer(@RequestBody Map<String, String> request) {
        String nif = request.get("nif");
        String codeImpot = request.get("codeImpot");
        String periodeFiscale = request.get("periodeFiscale");
        return ResponseEntity.ok(toResponse(genererUseCase.execute(nif, codeImpot, periodeFiscale)));
    }

    @PutMapping("/{id}/affecter/{idAgent}")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','RESPONSABLE','ADMIN')")
    public ResponseEntity<ObligationResponse> affecter(@PathVariable Long id,
                                                        @PathVariable String idAgent) {
        return ResponseEntity.ok(toResponse(affecterUseCase.execute(id, idAgent)));
    }

    private ObligationResponse toResponse(ObligationFiscale o) {
        return ObligationResponse.builder()
                .idObligationFiscale(o.getIdObligationFiscale())
                .nif(o.getNif())
                .codeImpot(o.getCodeImpot())
                .idAgent(o.getIdAgent())
                .idJourFerie(o.getIdJourFerie())
                .periodeFiscale(o.getPeriodeFiscale())
                .dateLimiteTheorique(o.getDateLimiteTheorique())
                .dateLimiteReelle(o.getDateLimiteReelle())
                .statutDeclaration(o.getStatutDeclaration() == null ? null : o.getStatutDeclaration().name())
                .dateDepotEffectif(o.getDateDepotEffectif())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}

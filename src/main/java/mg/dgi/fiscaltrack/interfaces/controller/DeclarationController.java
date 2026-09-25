package mg.dgi.fiscaltrack.interfaces.controller;

import jakarta.validation.Valid;
import mg.dgi.fiscaltrack.application.usecase.declaration.ConsulterDeclarationsUseCase;
import mg.dgi.fiscaltrack.application.usecase.declaration.DeposerDeclarationUseCase;
import mg.dgi.fiscaltrack.application.usecase.declaration.VerifierDeclarationUseCase;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.interfaces.dto.request.DeclarationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/declarations")
public class DeclarationController {

    private final DeposerDeclarationUseCase deposerUseCase;
    private final VerifierDeclarationUseCase verifierUseCase;
    private final ConsulterDeclarationsUseCase consulterUseCase;

    public DeclarationController(DeposerDeclarationUseCase deposerUseCase,
                                  VerifierDeclarationUseCase verifierUseCase,
                                  ConsulterDeclarationsUseCase consulterUseCase) {
        this.deposerUseCase = deposerUseCase;
        this.verifierUseCase = verifierUseCase;
        this.consulterUseCase = consulterUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public List<Map<String, Object>> lister() {
        return consulterUseCase.toutes().stream().map(this::toMap).toList();
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public Page<Map<String, Object>> listerPagine(@PageableDefault(size = 20) Pageable pageable) {
        return consulterUseCase.toutesPaginees(pageable).map(this::toMap);
    }

    @GetMapping("/{id}")
    public Map<String, Object> parId(@PathVariable Long id) {
        return toMap(consulterUseCase.parId(id));
    }

    @GetMapping("/obligation/{idObligation}")
    public Map<String, Object> parObligation(@PathVariable Long idObligation) {
        return toMap(consulterUseCase.parObligation(idObligation));
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','RESPONSABLE','ADMIN')")
    public List<Map<String, Object>> parStatut(@PathVariable String statut) {
        return consulterUseCase.parStatutValidation(statut).stream().map(this::toMap).toList();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> deposer(@Valid @RequestBody DeclarationRequest request) {
        Declaration declaration = deposerUseCase.execute(
                request.getIdObligationFiscale(),
                request.getChiffreAffairesDeclare(),
                request.getImpotPrincipalCalcule());
        return ResponseEntity.ok(toMap(declaration));
    }

    @PutMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','RESPONSABLE','ADMIN')")
    public ResponseEntity<Map<String, Object>> valider(@PathVariable Long id) {
        return ResponseEntity.ok(toMap(verifierUseCase.valider(id)));
    }

    @PutMapping("/{id}/rejeter")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','RESPONSABLE','ADMIN')")
    public ResponseEntity<Map<String, Object>> rejeter(@PathVariable Long id) {
        return ResponseEntity.ok(toMap(verifierUseCase.rejeter(id)));
    }

    private Map<String, Object> toMap(Declaration d) {
        return Map.of(
                "idDeclaration", d.getIdDeclaration(),
                "idObligationFiscale", d.getIdObligationFiscale(),
                "chiffreAffairesDeclare", d.getChiffreAffairesDeclare(),
                "impotPrincipalCalcule", d.getImpotPrincipalCalcule(),
                "dateSoumission", d.getDateSoumission().toString(),
                "statutValidation", d.getStatutValidation().name()
        );
    }
}

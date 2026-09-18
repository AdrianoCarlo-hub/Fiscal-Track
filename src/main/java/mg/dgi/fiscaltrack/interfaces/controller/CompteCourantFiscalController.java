package mg.dgi.fiscaltrack.interfaces.controller;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comptes")
public class CompteCourantFiscalController {

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;

    public CompteCourantFiscalController(CompteCourantFiscalRepositoryPort compteRepositoryPort) {
        this.compteRepositoryPort = compteRepositoryPort;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public List<Map<String, Object>> lister() {
        return compteRepositoryPort.findAll().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> parId(@PathVariable Long id) {
        CompteCourantFiscal compte = compteRepositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Compte introuvable : " + id));
        return toMap(compte);
    }

    @GetMapping("/contribuable/{nif}")
    public List<Map<String, Object>> parContribuable(@PathVariable String nif) {
        return compteRepositoryPort.findByNif(nif).stream().map(this::toMap).toList();
    }

    @GetMapping("/non-soldes")
    @PreAuthorize("hasAnyRole('AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public List<Map<String, Object>> nonSoldes() {
        return compteRepositoryPort.findNonSoldes().stream().map(this::toMap).toList();
    }

    private Map<String, Object> toMap(CompteCourantFiscal c) {
        return Map.of(
                "idCompte", c.getIdCompte(),
                "idDeclaration", c.getIdDeclaration(),
                "nif", c.getNif(),
                "dateCreationLigne", c.getDateCreationLigne().toString(),
                "montantPrincipal", c.getMontantPrincipal(),
                "montantPenalites", c.getMontantPenalites(),
                "montantPaye", c.getMontantPaye(),
                "montantTotalDu", c.getMontantTotalDu() == null ? 0 : c.getMontantTotalDu(),
                "resteARecouvrer", c.getResteARecouvrer() == null ? 0 : c.getResteARecouvrer(),
                "statutRecouvrement", c.getStatutRecouvrement().name()
        );
    }
}

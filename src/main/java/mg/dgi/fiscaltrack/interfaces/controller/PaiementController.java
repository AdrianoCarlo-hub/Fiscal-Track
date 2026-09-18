package mg.dgi.fiscaltrack.interfaces.controller;

import jakarta.validation.Valid;
import mg.dgi.fiscaltrack.application.usecase.paiement.CalculerResteARecouvrerUseCase;
import mg.dgi.fiscaltrack.application.usecase.paiement.ConsulterPaiementsUseCase;
import mg.dgi.fiscaltrack.application.usecase.paiement.EnregistrerPaiementUseCase;
import mg.dgi.fiscaltrack.domain.enums.ModePaiement;
import mg.dgi.fiscaltrack.domain.model.Paiement;
import mg.dgi.fiscaltrack.interfaces.dto.request.PaiementRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paiements")
public class PaiementController {

    private final EnregistrerPaiementUseCase enregistrerUseCase;
    private final ConsulterPaiementsUseCase consulterUseCase;
    private final CalculerResteARecouvrerUseCase calculerResteUseCase;

    public PaiementController(EnregistrerPaiementUseCase enregistrerUseCase,
                               ConsulterPaiementsUseCase consulterUseCase,
                               CalculerResteARecouvrerUseCase calculerResteUseCase) {
        this.enregistrerUseCase = enregistrerUseCase;
        this.consulterUseCase = consulterUseCase;
        this.calculerResteUseCase = calculerResteUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public List<Map<String, Object>> lister() {
        return consulterUseCase.tous().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> parId(@PathVariable Long id) {
        return toMap(consulterUseCase.parId(id));
    }

    @GetMapping("/compte/{idCompte}")
    public List<Map<String, Object>> parCompte(@PathVariable Long idCompte) {
        return consulterUseCase.parCompte(idCompte).stream().map(this::toMap).toList();
    }

    @GetMapping("/compte/{idCompte}/reste")
    public Map<String, Object> resteARecouvrer(@PathVariable Long idCompte) {
        BigDecimal reste = calculerResteUseCase.execute(idCompte);
        return Map.of(
                "idCompte", idCompte,
                "resteARecouvrer", reste
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT_RECETTE','ADMIN')")
    public ResponseEntity<Map<String, Object>> enregistrer(@Valid @RequestBody PaiementRequest request) {
        Paiement paiement = enregistrerUseCase.execute(
                request.getIdCompte(),
                request.getMontantVerse(),
                ModePaiement.valueOf(request.getModePaiement()),
                request.getReferenceTransaction());
        return ResponseEntity.ok(toMap(paiement));
    }

    private Map<String, Object> toMap(Paiement p) {
        return Map.of(
                "idPaiement", p.getIdPaiement(),
                "idCompte", p.getIdCompte(),
                "montantVerse", p.getMontantVerse(),
                "datePaiement", p.getDatePaiement().toString(),
                "modePaiement", p.getModePaiement().name(),
                "referenceTransaction", p.getReferenceTransaction() == null ? "" : p.getReferenceTransaction()
        );
    }
}

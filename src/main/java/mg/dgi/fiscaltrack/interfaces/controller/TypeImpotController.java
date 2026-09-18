package mg.dgi.fiscaltrack.interfaces.controller;

import jakarta.validation.Valid;
import mg.dgi.fiscaltrack.application.usecase.typeimpot.AjouterTypeImpotUseCase;
import mg.dgi.fiscaltrack.application.usecase.typeimpot.ConsulterTypesImpotsUseCase;
import mg.dgi.fiscaltrack.application.usecase.typeimpot.ModifierTypeImpotUseCase;
import mg.dgi.fiscaltrack.domain.enums.Periodicite;
import mg.dgi.fiscaltrack.domain.model.TypeImpot;
import mg.dgi.fiscaltrack.interfaces.dto.request.TypeImpotRequest;
import mg.dgi.fiscaltrack.interfaces.dto.response.ObligationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/types-impots")
public class TypeImpotController {

    private final AjouterTypeImpotUseCase ajouterUseCase;
    private final ModifierTypeImpotUseCase modifierUseCase;
    private final ConsulterTypesImpotsUseCase consulterUseCase;

    public TypeImpotController(AjouterTypeImpotUseCase ajouterUseCase,
                                ModifierTypeImpotUseCase modifierUseCase,
                                ConsulterTypesImpotsUseCase consulterUseCase) {
        this.ajouterUseCase = ajouterUseCase;
        this.modifierUseCase = modifierUseCase;
        this.consulterUseCase = consulterUseCase;
    }

    @GetMapping
    public List<Map<String, Object>> lister() {
        return consulterUseCase.tous().stream().map(this::toMap).toList();
    }

    @GetMapping("/{codeImpot}")
    public Map<String, Object> parCode(@PathVariable String codeImpot) {
        return toMap(consulterUseCase.parCode(codeImpot));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> ajouter(@Valid @RequestBody TypeImpotRequest request) {
        return ResponseEntity.ok(toMap(ajouterUseCase.execute(toDomain(request))));
    }

    @PutMapping("/{codeImpot}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> modifier(@PathVariable String codeImpot,
                                                         @Valid @RequestBody TypeImpotRequest request) {
        return ResponseEntity.ok(toMap(modifierUseCase.execute(codeImpot, toDomain(request))));
    }

    private TypeImpot toDomain(TypeImpotRequest r) {
        return TypeImpot.builder()
                .codeImpot(r.getCodeImpot())
                .nomImpot(r.getNomImpot())
                .periodicite(r.getPeriodicite() == null ? null : Periodicite.valueOf(r.getPeriodicite()))
                .echeanceTheoriqueJour(r.getEcheanceTheoriqueJour())
                .echeanceTheoriqueMois(r.getEcheanceTheoriqueMois())
                .build();
    }

    private Map<String, Object> toMap(TypeImpot t) {
        return Map.of(
                "codeImpot", t.getCodeImpot(),
                "nomImpot", t.getNomImpot(),
                "periodicite", t.getPeriodicite() == null ? "" : t.getPeriodicite().name(),
                "echeanceTheoriqueJour", t.getEcheanceTheoriqueJour() == null ? 0 : t.getEcheanceTheoriqueJour(),
                "echeanceTheoriqueMois", t.getEcheanceTheoriqueMois() == null ? 0 : t.getEcheanceTheoriqueMois()
        );
    }
}

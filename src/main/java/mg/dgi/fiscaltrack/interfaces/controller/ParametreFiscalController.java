package mg.dgi.fiscaltrack.interfaces.controller;

import jakarta.validation.Valid;
import mg.dgi.fiscaltrack.application.usecase.admin.ConsulterParametresFiscauxUseCase;
import mg.dgi.fiscaltrack.application.usecase.admin.ModifierParametreFiscalUseCase;
import mg.dgi.fiscaltrack.domain.model.ParametreFiscal;
import mg.dgi.fiscaltrack.interfaces.dto.request.ParametreFiscalRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Page ADMIN de gestion des parametres fiscaux (BLOC 4.3) :
 * lister, consulter et modifier les valeurs externees de `parametre_fiscal`.
 * Toute modification est tracee dans l'historique d'audit.
 */
@RestController
@RequestMapping("/api/admin/parametres-fiscaux")
@PreAuthorize("hasRole('ADMIN')")
public class ParametreFiscalController {

    private final ConsulterParametresFiscauxUseCase consulterUseCase;
    private final ModifierParametreFiscalUseCase modifierUseCase;

    public ParametreFiscalController(ConsulterParametresFiscauxUseCase consulterUseCase,
                                     ModifierParametreFiscalUseCase modifierUseCase) {
        this.consulterUseCase = consulterUseCase;
        this.modifierUseCase = modifierUseCase;
    }

    @GetMapping
    public List<Map<String, Object>> lister(@RequestParam(value = "secteur", required = false) String secteur) {
        List<ParametreFiscal> parametres = (secteur == null || secteur.isBlank())
                ? consulterUseCase.tous()
                : consulterUseCase.parSecteur(secteur);
        return parametres.stream().map(this::toMap).toList();
    }

    @GetMapping("/{code}")
    public Map<String, Object> parCode(@PathVariable String code) {
        return toMap(consulterUseCase.parCode(code));
    }

    @PutMapping("/{code}")
    public ResponseEntity<Map<String, Object>> modifier(@PathVariable String code,
                                                        @Valid @RequestBody ParametreFiscalRequest request,
                                                        java.security.Principal principal) {
        String auteur = principal == null ? null : principal.getName();
        ParametreFiscal saved = modifierUseCase.execute(
                code,
                request.getValeur(),
                request.getAnneeDebut(),
                request.getAnneeFin(),
                request.getBaseLegale(),
                request.getSecteur(),
                request.getDescription(),
                auteur);
        return ResponseEntity.ok(toMap(saved));
    }

    private Map<String, Object> toMap(ParametreFiscal p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", p.getCode());
        map.put("valeur", p.getValeur());
        map.put("anneeDebut", p.getAnneeDebut() == null ? null : p.getAnneeDebut().getValue());
        map.put("anneeFin", p.getAnneeFin() == null ? null : p.getAnneeFin().getValue());
        map.put("baseLegale", p.getBaseLegale());
        map.put("secteur", p.getSecteur());
        map.put("description", p.getDescription());
        return map;
    }
}

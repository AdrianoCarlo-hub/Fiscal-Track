package mg.dgi.fiscaltrack.interfaces.controller;

import jakarta.validation.Valid;
import mg.dgi.fiscaltrack.application.usecase.contribuable.AjouterContribuableUseCase;
import mg.dgi.fiscaltrack.application.usecase.contribuable.ConsulterContribuableUseCase;
import mg.dgi.fiscaltrack.application.usecase.contribuable.ModifierContribuableUseCase;
import mg.dgi.fiscaltrack.application.usecase.contribuable.RechercherContribuableUseCase;
import mg.dgi.fiscaltrack.domain.enums.FormeJuridique;
import mg.dgi.fiscaltrack.domain.enums.ObligationComptable;
import mg.dgi.fiscaltrack.domain.enums.RegimeImposition;
import mg.dgi.fiscaltrack.domain.enums.StatutActivite;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.interfaces.dto.request.ContribuableRequest;
import mg.dgi.fiscaltrack.interfaces.dto.response.ContribuableResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contribuables")
public class ContribuableController {

    private final AjouterContribuableUseCase ajouterUseCase;
    private final ConsulterContribuableUseCase consulterUseCase;
    private final ModifierContribuableUseCase modifierUseCase;
    private final RechercherContribuableUseCase rechercherUseCase;

    public ContribuableController(AjouterContribuableUseCase ajouterUseCase,
                                    ConsulterContribuableUseCase consulterUseCase,
                                    ModifierContribuableUseCase modifierUseCase,
                                    RechercherContribuableUseCase rechercherUseCase) {
        this.ajouterUseCase = ajouterUseCase;
        this.consulterUseCase = consulterUseCase;
        this.modifierUseCase = modifierUseCase;
        this.rechercherUseCase = rechercherUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public List<ContribuableResponse> lister() {
        return consulterUseCase.tous().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{nif}")
    public ContribuableResponse parNif(@PathVariable String nif) {
        return toResponse(consulterUseCase.parNif(nif));
    }

    @GetMapping("/search")
    public List<ContribuableResponse> rechercher(@RequestParam("q") String critere) {
        return rechercherUseCase.execute(critere).stream().map(this::toResponse).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT_GESTION','ADMIN')")
    public ResponseEntity<ContribuableResponse> ajouter(@Valid @RequestBody ContribuableRequest request) {
        Contribuable contribuable = toDomain(request);
        return ResponseEntity.ok(toResponse(ajouterUseCase.execute(contribuable)));
    }

    @PutMapping("/{nif}")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','ADMIN')")
    public ResponseEntity<ContribuableResponse> modifier(@PathVariable String nif,
                                                          @Valid @RequestBody ContribuableRequest request) {
        Contribuable donnees = toDomain(request);
        return ResponseEntity.ok(toResponse(modifierUseCase.execute(nif, donnees)));
    }

    private Contribuable toDomain(ContribuableRequest r) {
        return Contribuable.builder()
                .nif(r.getNif())
                .raisonSociale(r.getRaisonSociale())
                .formeJuridique(parseEnum(FormeJuridique.class, r.getFormeJuridique()))
                .nomDirigeant(r.getNomDirigeant())
                .prenomDirigeant(r.getPrenomDirigeant())
                .cinDirigeant(r.getCinDirigeant())
                .emailContribuable(r.getEmailContribuable())
                .telephoneContribuable(r.getTelephoneContribuable())
                .adresseContribuable(r.getAdresseContribuable())
                .communeContribuable(r.getCommuneContribuable())
                .motDePasseHashContribuable(r.getMotDePasseHashContribuable())
                .regimeImposition(parseEnum(RegimeImposition.class, r.getRegimeImposition()))
                .obligationComptable(parseEnum(ObligationComptable.class, r.getObligationComptable()))
                .statutActivite(parseEnum(StatutActivite.class, r.getStatutActivite()))
                .dateImmatriculation(r.getDateImmatriculation())
                .build();
    }

    private ContribuableResponse toResponse(Contribuable c) {
        return ContribuableResponse.builder()
                .nif(c.getNif())
                .raisonSociale(c.getRaisonSociale())
                .formeJuridique(enumName(c.getFormeJuridique()))
                .nomDirigeant(c.getNomDirigeant())
                .prenomDirigeant(c.getPrenomDirigeant())
                .cinDirigeant(c.getCinDirigeant())
                .emailContribuable(c.getEmailContribuable())
                .telephoneContribuable(c.getTelephoneContribuable())
                .adresseContribuable(c.getAdresseContribuable())
                .communeContribuable(c.getCommuneContribuable())
                .regimeImposition(enumName(c.getRegimeImposition()))
                .obligationComptable(enumName(c.getObligationComptable()))
                .statutActivite(enumName(c.getStatutActivite()))
                .dateImmatriculation(c.getDateImmatriculation())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (value == null) return null;
        try { return Enum.valueOf(clazz, value); } catch (IllegalArgumentException e) { return null; }
    }

    private String enumName(Enum<?> e) {
        return e == null ? null : e.name();
    }
}

package mg.dgi.fiscaltrack.interfaces.controller;

import jakarta.validation.Valid;
import mg.dgi.fiscaltrack.application.usecase.acompte.ConsulterAcomptesUseCase;
import mg.dgi.fiscaltrack.application.usecase.contribuable.AjouterContribuableUseCase;
import mg.dgi.fiscaltrack.application.usecase.contribuable.ConsulterContribuableUseCase;
import mg.dgi.fiscaltrack.application.usecase.contribuable.ModifierContribuableUseCase;
import mg.dgi.fiscaltrack.application.usecase.contribuable.ModifierPreferencesContactUseCase;
import mg.dgi.fiscaltrack.application.usecase.contribuable.RechercherContribuableUseCase;
import mg.dgi.fiscaltrack.domain.enums.FormeJuridique;
import mg.dgi.fiscaltrack.domain.enums.ObligationComptable;
import mg.dgi.fiscaltrack.domain.enums.RegimeImposition;
import mg.dgi.fiscaltrack.domain.enums.StatutActivite;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.interfaces.dto.request.ContribuableRequest;
import mg.dgi.fiscaltrack.interfaces.dto.request.PreferencesContactRequest;
import mg.dgi.fiscaltrack.interfaces.dto.response.ContribuableResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    private final ConsulterAcomptesUseCase consulterAcomptesUseCase;
    private final ModifierPreferencesContactUseCase modifierPreferencesUseCase;

    public ContribuableController(AjouterContribuableUseCase ajouterUseCase,
                                    ConsulterContribuableUseCase consulterUseCase,
                                    ModifierContribuableUseCase modifierUseCase,
                                    RechercherContribuableUseCase rechercherUseCase,
                                    ConsulterAcomptesUseCase consulterAcomptesUseCase,
                                    ModifierPreferencesContactUseCase modifierPreferencesUseCase) {
        this.ajouterUseCase = ajouterUseCase;
        this.consulterUseCase = consulterUseCase;
        this.modifierUseCase = modifierUseCase;
        this.rechercherUseCase = rechercherUseCase;
        this.consulterAcomptesUseCase = consulterAcomptesUseCase;
        this.modifierPreferencesUseCase = modifierPreferencesUseCase;
    }

    /**
     * Acomptes obligatoires IR/IS d'un exercice. Le contribuable ne voit que
     * ses propres acomptes (403 sinon) ; les agents consultent tout dossier.
     */
    @GetMapping("/{nif}/acomptes")
    public List<java.util.Map<String, Object>> acomptes(@PathVariable String nif,
                                                        @RequestParam(value = "exercice", required = false)
                                                        Integer exercice) {
        UtilisateurConnecte.verifierPropriete(nif, "ces acomptes");
        int annee = exercice == null ? java.time.LocalDate.now().getYear() : exercice;
        return consulterAcomptesUseCase.execute(nif, annee);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public List<ContribuableResponse> lister() {
        return consulterUseCase.tous().stream().map(this::toResponse).toList();
    }

    /** Endpoint pagine : /api/contribuables/paginated?page=0&size=20 */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public Page<ContribuableResponse> listerPagine(
            @PageableDefault(size = 20) Pageable pageable) {
        return consulterUseCase.tousPagine(pageable).map(this::toResponse);
    }

    @GetMapping("/{nif}")
    public ContribuableResponse parNif(@PathVariable String nif) {
        UtilisateurConnecte.verifierPropriete(nif, "ce dossier contribuable");
        return toResponse(consulterUseCase.parNif(nif));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public List<ContribuableResponse> rechercher(@RequestParam("q") String critere) {
        return rechercherUseCase.execute(critere).stream().map(this::toResponse).toList();
    }

    /** Recherche paginee : /api/contribuables/search/paginated?q=text&page=0&size=20 */
    @GetMapping("/search/paginated")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','RESPONSABLE','ADMIN')")
    public Page<ContribuableResponse> rechercherPagine(
            @RequestParam("q") String critere,
            @PageableDefault(size = 20) Pageable pageable) {
        return rechercherUseCase.executePagine(critere, pageable).map(this::toResponse);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT_GESTION','ADMIN')")
    public ResponseEntity<ContribuableResponse> ajouter(@Valid @RequestBody ContribuableRequest request,
                                                         java.security.Principal principal) {
        Contribuable contribuable = toDomain(request);
        String auteur = principal == null ? null : principal.getName();
        return ResponseEntity.ok(toResponse(ajouterUseCase.execute(contribuable, auteur)));
    }

    @PutMapping("/{nif}")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','ADMIN')")
    public ResponseEntity<ContribuableResponse> modifier(@PathVariable String nif,
                                                          @Valid @RequestBody ContribuableRequest request,
                                                          java.security.Principal principal) {
        Contribuable donnees = toDomain(request);
        String auteur = principal == null ? null : principal.getName();
        return ResponseEntity.ok(toResponse(modifierUseCase.execute(nif, donnees, auteur)));
    }

    /** Le contribuable met a jour ses propres preferences de contact. */
    @PutMapping("/{nif}/preferences-contact")
    public ResponseEntity<ContribuableResponse> preferencesContact(
            @PathVariable String nif,
            @Valid @RequestBody PreferencesContactRequest request,
            java.security.Principal principal) {
        UtilisateurConnecte.verifierPropriete(nif, "ces préférences de contact");
        String auteur = principal == null ? null : principal.getName();
        return ResponseEntity.ok(toResponse(modifierPreferencesUseCase.execute(
                nif,
                request.getCanalPrefere(),
                request.getAccepteEmail(),
                request.getAccepteSms(),
                auteur)));
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
                .autorisationOuverture(r.getAutorisationOuverture())
                .optionSante(r.getOptionSante())
                .dateCloture(r.getDateCloture())
                .categorieActivite(r.getCategorieActivite())
                .canalPrefere(parseEnum(mg.dgi.fiscaltrack.domain.enums.CanalEnvoi.class, r.getCanalPrefere()))
                .accepteEmail(r.getAccepteEmail())
                .accepteSms(r.getAccepteSms())
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
                .autorisationOuverture(c.getAutorisationOuverture())
                .optionSante(c.getOptionSante())
                .dateCloture(c.getDateCloture())
                .categorieActivite(c.getCategorieActivite())
                .canalPrefere(enumName(c.getCanalPrefere()))
                .accepteEmail(c.getAccepteEmail())
                .accepteSms(c.getAccepteSms())
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

package mg.dgi.fiscaltrack.interfaces.controller;

import jakarta.validation.Valid;
import mg.dgi.fiscaltrack.application.usecase.declaration.ConsulterDeclarationsUseCase;
import mg.dgi.fiscaltrack.application.usecase.declaration.DeposerDeclarationUseCase;
import mg.dgi.fiscaltrack.application.usecase.declaration.PrevisualiserDeclarationUseCase;
import mg.dgi.fiscaltrack.application.usecase.declaration.VerifierDeclarationUseCase;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.interfaces.dto.request.DeclarationRequest;
import mg.dgi.fiscaltrack.interfaces.dto.request.DeclarationRequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(DeclarationController.class);

    private final DeposerDeclarationUseCase deposerUseCase;
    private final VerifierDeclarationUseCase verifierUseCase;
    private final ConsulterDeclarationsUseCase consulterUseCase;
    private final PrevisualiserDeclarationUseCase previsualiserUseCase;

    public DeclarationController(DeposerDeclarationUseCase deposerUseCase,
                                  VerifierDeclarationUseCase verifierUseCase,
                                  ConsulterDeclarationsUseCase consulterUseCase,
                                  PrevisualiserDeclarationUseCase previsualiserUseCase) {
        this.deposerUseCase = deposerUseCase;
        this.verifierUseCase = verifierUseCase;
        this.consulterUseCase = consulterUseCase;
        this.previsualiserUseCase = previsualiserUseCase;
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

    /**
     * Previsualise l'impot calcule par le serveur pour une obligation donnee
     * (BLOC 4.7 : le champ n'est plus libre, le serveur impose son montant).
     * Le formulaire envoye est celui adapte au type d'impot ; le calcul
     * reste assure par les calculateurs existants. Ne persiste rien.
     */
    @PostMapping("/preview")
    public Map<String, Object> previsualiser(@Valid @RequestBody DeclarationRequest request) {
        return previsualiserUseCase.previsualiser(
                DeclarationRequestMapper.toDomain(request), nifCourant());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> deposer(@Valid @RequestBody DeclarationRequest request) {
        Declaration declaration = deposerUseCase.deposer(
                DeclarationRequestMapper.toDomain(request),
                serialiser(request),
                nifCourant());
        return ResponseEntity.ok(toMap(declaration));
    }

    @PutMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','RESPONSABLE','ADMIN')")
    public ResponseEntity<Map<String, Object>> valider(@PathVariable Long id) {
        return ResponseEntity.ok(toMap(verifierUseCase.valider(id)));
    }

    /**
     * Rejet d'une declaration : le motif (corps JSON {\"motif\": \"...\"})
     * est obligatoire et affiche au contribuable.
     */
    @PutMapping("/{id}/rejeter")
    @PreAuthorize("hasAnyRole('AGENT_GESTION','RESPONSABLE','ADMIN')")
    public ResponseEntity<Map<String, Object>> rejeter(@PathVariable Long id,
                                                       @RequestBody Map<String, String> corps) {
        return ResponseEntity.ok(toMap(verifierUseCase.rejeter(
                id, corps == null ? null : corps.get("motif"))));
    }

    /**
     * NIF de l'utilisateur connecte si c'est un contribuable, sinon null
     * (les agents consultent toutes les obligations).
     */
    private String nifCourant() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        boolean estContribuable = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CONTRIBUABLE".equals(a.getAuthority()));
        return estContribuable ? auth.getName() : null;
    }

    /** Copie JSON des champs saisis, pour la tracabilite de la declaration. */
    private String serialiser(DeclarationRequest request) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.warn("[DECLARATION] Serialisation des donnees saisies impossible : {}",
                    e.getMessage());
            return null;
        }
    }

    private Map<String, Object> toMap(Declaration d) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("idDeclaration", d.getIdDeclaration());
        map.put("idObligationFiscale", d.getIdObligationFiscale());
        map.put("chiffreAffairesDeclare", d.getChiffreAffairesDeclare());
        map.put("impotPrincipalCalcule", d.getImpotPrincipalCalcule());
        map.put("dateSoumission", d.getDateSoumission().toString());
        map.put("statutValidation", d.getStatutValidation().name());
        map.put("detailCalcul", d.getDetailCalcul());
        map.put("motifRejet", d.getMotifRejet());
        return map;
    }
}

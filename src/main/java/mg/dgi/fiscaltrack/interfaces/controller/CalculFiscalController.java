package mg.dgi.fiscaltrack.interfaces.controller;

import jakarta.validation.Valid;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerFiscalDispatcherUseCase;
import mg.dgi.fiscaltrack.application.usecase.regime.DeterminerRegimeUseCase;
import mg.dgi.fiscaltrack.domain.model.DeficitReportable;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import mg.dgi.fiscaltrack.interfaces.dto.request.CalculImpRequest;
import mg.dgi.fiscaltrack.interfaces.dto.request.CalculIpviRequest;
import mg.dgi.fiscaltrack.interfaces.dto.request.CalculIrRequest;
import mg.dgi.fiscaltrack.interfaces.dto.request.CalculIrcmRequest;
import mg.dgi.fiscaltrack.interfaces.dto.request.CalculIrsaRequest;
import mg.dgi.fiscaltrack.interfaces.dto.request.CalculIsRequest;
import mg.dgi.fiscaltrack.interfaces.dto.request.CalculTvaRequest;
import mg.dgi.fiscaltrack.interfaces.dto.response.CalculFiscalResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controleur de simulation fiscale. Tous les calculs passent par le
 * {@link CalculerFiscalDispatcherUseCase} (point 3.5) : le controleur ne
 * connait aucun use case de calcul concret.
 */
@RestController
@RequestMapping("/api/calculs")
@PreAuthorize("hasAnyRole('AGENT_GESTION','AGENT_RECETTE','RESPONSABLE','ADMIN')")
public class CalculFiscalController {

    private static final String AVERTISSEMENT_AUCUN = "Aucun avertissement de seuil.";

    private final CalculerFiscalDispatcherUseCase dispatcher;
    private final DeterminerRegimeUseCase determinerRegimeUseCase;

    public CalculFiscalController(CalculerFiscalDispatcherUseCase dispatcher,
                                  DeterminerRegimeUseCase determinerRegimeUseCase) {
        this.dispatcher = dispatcher;
        this.determinerRegimeUseCase = determinerRegimeUseCase;
    }

    /**
     * Ajoute les avertissements de seuil de regime (seuil TVA confirme et
     * parametres explicitement marques) au resultat de simulation. Le calcul
     * de regime ne modifie jamais le montant : il informe seulement.
     */
    private void ajouterAvertissementsRegime(ResultatCalculFiscal resultat, BigDecimal chiffreAffaires) {
        if (chiffreAffaires == null) {
            return;
        }
        for (String avertissement : determinerRegimeUseCase.verifierAvertissements(chiffreAffaires)) {
            if (!AVERTISSEMENT_AUCUN.equals(avertissement)) {
                resultat.getAvertissements().add(avertissement);
            }
        }
    }

    /**
     * Simulation d'un calcul IR. Ne persiste rien.
     */
    @PostMapping("/ir")
    public ResponseEntity<CalculFiscalResponse> calculerIr(@Valid @RequestBody CalculIrRequest req) {
        int mois = req.getMoisActifs() == null ? 12 : req.getMoisActifs();
        boolean autorisationOuverture = req.getAutorisationOuverture() != null
                && req.getAutorisationOuverture();
        boolean optionSante = req.getOptionSante() != null && req.getOptionSante();
        int anneeExercice = req.getAnneeExercice() == null
                ? java.time.Year.now().getValue()
                : req.getAnneeExercice();

        List<DeficitReportable> deficits = new ArrayList<>();
        if (req.getDeficits() != null) {
            for (CalculIrRequest.DeficitRequest d : req.getDeficits()) {
                if (d.getAnneeOrigine() != null && d.getMontant() != null) {
                    deficits.add(new DeficitReportable(d.getAnneeOrigine(), d.getMontant()));
                }
            }
        }
        if (deficits.isEmpty() && req.getDeficitsReportables() != null) {
            deficits.add(new DeficitReportable(anneeExercice - 1, req.getDeficitsReportables()));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("caHT", req.getCaHT());
        params.put("chargesDeductibles", req.getChargesDeductibles());
        params.put("reintegrations", req.getReintegrations());
        params.put("deductions", req.getDeductions());
        params.put("deficitsReportables", deficits);
        params.put("anneeExercice", anneeExercice);
        params.put("categorieActivite", req.getCategorieActivite());
        params.put("acomptes", req.getAcomptes());
        params.put("moisActifs", mois);
        params.put("autorisationOuverture", autorisationOuverture);
        params.put("optionSante", optionSante);

        ResultatCalculFiscal resultat = dispatcher.execute("IR", params);
        ajouterAvertissementsRegime(resultat, req.getCaHT());
        return ResponseEntity.ok(toResponse(resultat));
    }

    @PostMapping("/is")
    public ResponseEntity<CalculFiscalResponse> calculerIs(@Valid @RequestBody CalculIsRequest req) {
        Map<String, Object> params = new HashMap<>();
        params.put("caBrut", req.getCaBrut());
        params.put("categorieActivite", req.getCategorieActivite());
        params.put("adherentCga", req.getAdherentCga() != null && req.getAdherentCga());
        params.put("achatsConformes", req.getAchatsConformes());
        params.put("chargesSalariales", req.getChargesSalariales());
        params.put("acomptes", req.getAcomptes());

        ResultatCalculFiscal resultat = dispatcher.execute("IS", params);
        ajouterAvertissementsRegime(resultat, req.getCaBrut());
        return ResponseEntity.ok(toResponse(resultat));
    }

    @PostMapping("/irsa")
    public ResponseEntity<CalculFiscalResponse> calculerIrsa(@Valid @RequestBody CalculIrsaRequest req) {
        Map<String, Object> params = new HashMap<>();
        params.put("salaireBrut", req.getSalaireBrut());
        params.put("cotisationsSociales", req.getCotisationsSociales());
        params.put("avantageLogement", req.getAvantageLogement());
        params.put("avantageVehicule", req.getAvantageVehicule());
        params.put("avantageTelephone", req.getAvantageTelephone());
        params.put("nombrePersonnesCharge",
                req.getNombrePersonnesCharge() == null ? 0 : req.getNombrePersonnesCharge());

        ResultatCalculFiscal resultat = dispatcher.execute("IRSA", params);
        return ResponseEntity.ok(toResponse(resultat));
    }

    @PostMapping("/tva")
    public ResponseEntity<CalculFiscalResponse> calculerTva(@Valid @RequestBody CalculTvaRequest req) {
        Map<String, Object> params = new HashMap<>();
        params.put("tvaCollectee", req.getTvaCollectee());
        params.put("tvaDeductible", req.getTvaDeductible());
        params.put("creditTvaAnterieur", req.getCreditTvaAnterieur());
        params.put("exportation", req.getExportation() != null && req.getExportation());
        params.put("gazButane", req.getGazButane() != null && req.getGazButane());
        params.put("chiffreAffaires", req.getChiffreAffaires());

        ResultatCalculFiscal resultat = dispatcher.execute("TVA", params);
        ajouterAvertissementsRegime(resultat, req.getChiffreAffaires());
        return ResponseEntity.ok(toResponse(resultat));
    }

    @PostMapping("/imp")
    public ResponseEntity<CalculFiscalResponse> calculerImp(@Valid @RequestBody CalculImpRequest req) {
        Map<String, Object> params = new HashMap<>();
        params.put("montantMarche", req.getMontantMarche());
        params.put("acomptes", req.getAcomptes());
        return ResponseEntity.ok(toResponse(dispatcher.execute("IMP", params)));
    }

    @PostMapping("/ipvi")
    public ResponseEntity<CalculFiscalResponse> calculerIpvi(@Valid @RequestBody CalculIpviRequest req) {
        Map<String, Object> params = new HashMap<>();
        params.put("prixVente", req.getPrixVente());
        params.put("acomptes", req.getAcomptes());
        return ResponseEntity.ok(toResponse(dispatcher.execute("IPVI", params)));
    }

    @PostMapping("/ircm")
    public ResponseEntity<CalculFiscalResponse> calculerIrcm(@Valid @RequestBody CalculIrcmRequest req) {
        Map<String, Object> params = new HashMap<>();
        params.put("caBrut", req.getCaBrut());
        params.put("estCooperative", req.getEstCooperative() != null && req.getEstCooperative());
        params.put("acomptes", req.getAcomptes());
        return ResponseEntity.ok(toResponse(dispatcher.execute("IRCM", params)));
    }

    private CalculFiscalResponse toResponse(ResultatCalculFiscal r) {
        return CalculFiscalResponse.builder()
                .typeImpot(r.getTypeImpot())
                .periode(r.getPeriode())
                .categorieActivite(r.getCategorieActivite() == null ? null : r.getCategorieActivite().name())
                .baseImposable(r.getBaseImposable())
                .tauxApplique(r.getTauxApplique())
                .montantBrut(r.getMontantBrut())
                .montantMinimum(r.getMontantMinimum())
                .reductions(r.getReductions())
                .acomptesDeduits(r.getAcomptesDeduits())
                .penalites(r.getPenalites())
                .creditFiscal(r.getCreditFiscal())
                .solde(r.getSolde())
                .explication(r.getExplication())
                .etapesCalcul(r.getEtapesCalcul())
                .avertissements(r.getAvertissements())
                .build();
    }
}

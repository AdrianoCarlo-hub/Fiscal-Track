package mg.dgi.fiscaltrack.interfaces.controller;

import jakarta.validation.Valid;
import mg.dgi.fiscaltrack.application.dto.ResultatCalculFiscalDto;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerFiscalDispatcherUseCase;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerIrUseCase;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerIrsaUseCase;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerIsUseCase;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerTvaUseCase;
import mg.dgi.fiscaltrack.domain.enums.CategorieActivite;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import mg.dgi.fiscaltrack.interfaces.dto.request.CalculIrRequest;
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

@RestController
@RequestMapping("/api/calculs")
@PreAuthorize("hasAnyRole('AGENT_GESTION','RESPONSABLE','ADMIN')")
public class CalculFiscalController {

    private final CalculerIrUseCase calculerIrUseCase;
    private final CalculerIsUseCase calculerIsUseCase;
    private final CalculerIrsaUseCase calculerIrsaUseCase;
    private final CalculerTvaUseCase calculerTvaUseCase;

    public CalculFiscalController(CalculerIrUseCase calculerIrUseCase,
                                    CalculerIsUseCase calculerIsUseCase,
                                    CalculerIrsaUseCase calculerIrsaUseCase,
                                    CalculerTvaUseCase calculerTvaUseCase) {
        this.calculerIrUseCase = calculerIrUseCase;
        this.calculerIsUseCase = calculerIsUseCase;
        this.calculerIrsaUseCase = calculerIrsaUseCase;
        this.calculerTvaUseCase = calculerTvaUseCase;
    }

    /**
     * Simulation d'un calcul IR. Ne persiste rien.
     */
    @PostMapping("/ir")
    public ResponseEntity<CalculFiscalResponse> calculerIr(@Valid @RequestBody CalculIrRequest req) {
        CategorieActivite cat = CategorieActivite.valueOf(req.getCategorieActivite().toUpperCase());
        int mois = req.getMoisActifs() == null ? 12 : req.getMoisActifs();

        ResultatCalculFiscal resultat = calculerIrUseCase.execute(
                req.getCaHT(),
                req.getChargesDeductibles(),
                req.getReintegrations(),
                req.getDeductions(),
                req.getDeficitsReportables(),
                cat,
                req.getAcomptes(),
                mois);

        return ResponseEntity.ok(toResponse(resultat));
    }

    @PostMapping("/is")
    public ResponseEntity<CalculFiscalResponse> calculerIs(@Valid @RequestBody CalculIsRequest req) {
        boolean cga = req.getAdherentCga() != null && req.getAdherentCga();
        ResultatCalculFiscal resultat = calculerIsUseCase.execute(
                req.getCaBrut(),
                cga,
                req.getAchatsConformes(),
                req.getChargesSalariales(),
                req.getAcomptes());
        return ResponseEntity.ok(toResponse(resultat));
    }

    @PostMapping("/irsa")
    public ResponseEntity<CalculFiscalResponse> calculerIrsa(@Valid @RequestBody CalculIrsaRequest req) {
        int charges = req.getNombrePersonnesCharge() == null ? 0 : req.getNombrePersonnesCharge();
        ResultatCalculFiscal resultat = calculerIrsaUseCase.execute(
                req.getSalaireBrut(),
                req.getCotisationsSociales(),
                req.getAvantageLogement(),
                req.getAvantageVehicule(),
                req.getAvantageTelephone(),
                charges);
        return ResponseEntity.ok(toResponse(resultat));
    }

    @PostMapping("/tva")
    public ResponseEntity<CalculFiscalResponse> calculerTva(@Valid @RequestBody CalculTvaRequest req) {
        boolean exportation = req.getExportation() != null && req.getExportation();
        ResultatCalculFiscal resultat = calculerTvaUseCase.execute(
                req.getTvaCollectee(),
                req.getTvaDeductible(),
                req.getCreditTvaAnterieur(),
                exportation);
        return ResponseEntity.ok(toResponse(resultat));
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
                .build();
    }
}

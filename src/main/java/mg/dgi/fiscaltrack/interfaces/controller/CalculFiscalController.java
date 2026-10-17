package mg.dgi.fiscaltrack.interfaces.controller;

import jakarta.validation.Valid;
import mg.dgi.fiscaltrack.application.dto.ResultatCalculFiscalDto;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerFiscalDispatcherUseCase;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerIrUseCase;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerIrsaUseCase;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerIsUseCase;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerTvaUseCase;
import mg.dgi.fiscaltrack.application.usecase.regime.DeterminerRegimeUseCase;
import mg.dgi.fiscaltrack.domain.enums.CategorieActivite;
import mg.dgi.fiscaltrack.domain.model.DeficitReportable;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/calculs")
@PreAuthorize("hasAnyRole('AGENT_GESTION','RESPONSABLE','ADMIN')")
public class CalculFiscalController {

    private static final String AVERTISSEMENT_AUCUN = "Aucun avertissement de seuil.";

    private final CalculerIrUseCase calculerIrUseCase;
    private final CalculerIsUseCase calculerIsUseCase;
    private final CalculerIrsaUseCase calculerIrsaUseCase;
    private final CalculerTvaUseCase calculerTvaUseCase;
    private final DeterminerRegimeUseCase determinerRegimeUseCase;

    public CalculFiscalController(CalculerIrUseCase calculerIrUseCase,
                                    CalculerIsUseCase calculerIsUseCase,
                                    CalculerIrsaUseCase calculerIrsaUseCase,
                                    CalculerTvaUseCase calculerTvaUseCase,
                                    DeterminerRegimeUseCase determinerRegimeUseCase) {
        this.calculerIrUseCase = calculerIrUseCase;
        this.calculerIsUseCase = calculerIsUseCase;
        this.calculerIrsaUseCase = calculerIrsaUseCase;
        this.calculerTvaUseCase = calculerTvaUseCase;
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
        CategorieActivite cat = CategorieActivite.valueOf(req.getCategorieActivite().toUpperCase());
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

        ResultatCalculFiscal resultat = calculerIrUseCase.execute(
                req.getCaHT(),
                req.getChargesDeductibles(),
                req.getReintegrations(),
                req.getDeductions(),
                deficits,
                anneeExercice,
                cat,
                req.getAcomptes(),
                mois,
                autorisationOuverture,
                optionSante);

        ajouterAvertissementsRegime(resultat, req.getCaHT());

        return ResponseEntity.ok(toResponse(resultat));
    }

    @PostMapping("/is")
    public ResponseEntity<CalculFiscalResponse> calculerIs(@Valid @RequestBody CalculIsRequest req) {
        boolean cga = req.getAdherentCga() != null && req.getAdherentCga();
        ResultatCalculFiscal resultat = calculerIsUseCase.execute(
                req.getCaBrut(),
                req.getCategorieActivite(),
                cga,
                req.getAchatsConformes(),
                req.getChargesSalariales(),
                req.getAcomptes());
        ajouterAvertissementsRegime(resultat, req.getCaBrut());
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
        boolean gazButane = req.getGazButane() != null && req.getGazButane();
        ResultatCalculFiscal resultat = calculerTvaUseCase.execute(
                req.getTvaCollectee(),
                req.getTvaDeductible(),
                req.getCreditTvaAnterieur(),
                exportation,
                gazButane,
                req.getChiffreAffaires());
        ajouterAvertissementsRegime(resultat, req.getChiffreAffaires());
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
                .avertissements(r.getAvertissements())
                .build();
    }
}

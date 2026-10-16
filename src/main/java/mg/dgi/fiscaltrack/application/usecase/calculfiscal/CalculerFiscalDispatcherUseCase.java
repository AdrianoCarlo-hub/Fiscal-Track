package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.domain.enums.CategorieActivite;
import mg.dgi.fiscaltrack.domain.exception.TypeImpotNonSupporteException;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Orchestrateur du moteur de calcul fiscal.
 * Route vers le bon use case selon le code impot.
 *
 * Codes supportes : IR, IS, IRSA, TVA, IPVI, IMP, IRCM.
 */
@Service
public class CalculerFiscalDispatcherUseCase {

    private final CalculerIrUseCase calculerIrUseCase;
    private final CalculerIsUseCase calculerIsUseCase;
    private final CalculerIrsaUseCase calculerIrsaUseCase;
    private final CalculerTvaUseCase calculerTvaUseCase;
    private final CalculerIpviUseCase calculerIpviUseCase;
    private final CalculerImpUseCase calculerImpUseCase;
    private final CalculerIrcmUseCase calculerIrcmUseCase;

    public CalculerFiscalDispatcherUseCase(CalculerIrUseCase calculerIrUseCase,
                                             CalculerIsUseCase calculerIsUseCase,
                                             CalculerIrsaUseCase calculerIrsaUseCase,
                                             CalculerTvaUseCase calculerTvaUseCase,
                                             CalculerIpviUseCase calculerIpviUseCase,
                                             CalculerImpUseCase calculerImpUseCase,
                                             CalculerIrcmUseCase calculerIrcmUseCase) {
        this.calculerIrUseCase = calculerIrUseCase;
        this.calculerIsUseCase = calculerIsUseCase;
        this.calculerIrsaUseCase = calculerIrsaUseCase;
        this.calculerTvaUseCase = calculerTvaUseCase;
        this.calculerIpviUseCase = calculerIpviUseCase;
        this.calculerImpUseCase = calculerImpUseCase;
        this.calculerIrcmUseCase = calculerIrcmUseCase;
    }

    public ResultatCalculFiscal execute(String codeImpot, Map<String, Object> params) {
        if (codeImpot == null) {
            throw new TypeImpotNonSupporteException("null");
        }

        switch (codeImpot.toUpperCase()) {
            case "IR":
                return calculerIr(params);
            case "IS":
                return calculerIs(params);
            case "IRSA":
                return calculerIrsa(params);
            case "TVA":
                return calculerTva(params);
            case "IPVI":
                return calculerIpvi(params);
            case "IMP":
                return calculerImp(params);
            case "IRCM":
                return calculerIrcm(params);
            default:
                throw new TypeImpotNonSupporteException(codeImpot);
        }
    }

    private ResultatCalculFiscal calculerIr(Map<String, Object> params) {
        return calculerIrUseCase.execute(
                bigDecimal(params, "caHT"),
                bigDecimal(params, "chargesDeductibles"),
                bigDecimal(params, "reintegrations"),
                bigDecimal(params, "deductions"),
                bigDecimal(params, "deficitsReportables"),
                categorie(params, "categorieActivite"),
                bigDecimal(params, "acomptes"),
                integer(params, "moisActifs", 12),
                bool(params, "autorisationOuverture"),
                bool(params, "optionSante"));
    }

    private ResultatCalculFiscal calculerIs(Map<String, Object> params) {
        return calculerIsUseCase.execute(
                bigDecimal(params, "caBrut"),
                bool(params, "adherentCga"),
                bigDecimal(params, "achatsConformes"),
                bigDecimal(params, "chargesSalariales"),
                bigDecimal(params, "acomptes"));
    }

    private ResultatCalculFiscal calculerIrsa(Map<String, Object> params) {
        return calculerIrsaUseCase.execute(
                bigDecimal(params, "salaireBrut"),
                bigDecimal(params, "cotisationsSociales"),
                bigDecimal(params, "avantageLogement"),
                bigDecimal(params, "avantageVehicule"),
                bigDecimal(params, "avantageTelephone"),
                integer(params, "nombrePersonnesCharge", 0));
    }

    private ResultatCalculFiscal calculerTva(Map<String, Object> params) {
        return calculerTvaUseCase.execute(
                bigDecimal(params, "tvaCollectee"),
                bigDecimal(params, "tvaDeductible"),
                bigDecimal(params, "creditTvaAnterieur"),
                bool(params, "exportation"));
    }

    private ResultatCalculFiscal calculerIpvi(Map<String, Object> params) {
        return calculerIpviUseCase.execute(
                bigDecimal(params, "prixVente"),
                bigDecimal(params, "acomptes"));
    }

    private ResultatCalculFiscal calculerImp(Map<String, Object> params) {
        return calculerImpUseCase.execute(
                bigDecimal(params, "montantMarche"),
                bigDecimal(params, "acomptes"));
    }

    private ResultatCalculFiscal calculerIrcm(Map<String, Object> params) {
        return calculerIrcmUseCase.execute(
                bigDecimal(params, "caBrut"),
                bool(params, "estCooperative"),
                bigDecimal(params, "acomptes"));
    }

    // === Helpers de conversion ===

    private BigDecimal bigDecimal(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) return null;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        if (v instanceof String) return new BigDecimal((String) v);
        return null;
    }

    private Integer integer(Map<String, Object> params, String key, int defaut) {
        Object v = params.get(key);
        if (v == null) return defaut;
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) return Integer.parseInt((String) v);
        return defaut;
    }

    private boolean bool(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return false;
    }

    private CategorieActivite categorie(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) return CategorieActivite.AUTRE;
        if (v instanceof CategorieActivite) return (CategorieActivite) v;
        try {
            return CategorieActivite.valueOf(v.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CategorieActivite.AUTRE;
        }
    }
}

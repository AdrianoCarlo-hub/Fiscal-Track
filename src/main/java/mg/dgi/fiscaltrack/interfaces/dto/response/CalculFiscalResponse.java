package mg.dgi.fiscaltrack.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculFiscalResponse {

    private String typeImpot;
    private String periode;
    private String categorieActivite;
    private BigDecimal baseImposable;
    private BigDecimal tauxApplique;
    private BigDecimal montantBrut;
    private BigDecimal montantMinimum;
    private BigDecimal reductions;
    private BigDecimal acomptesDeduits;
    private BigDecimal penalites;
    private BigDecimal creditFiscal;
    private BigDecimal solde;
    private String explication;
    private List<String> etapesCalcul;
}

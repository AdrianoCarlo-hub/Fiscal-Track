package mg.dgi.fiscaltrack.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private long nombreContribuables;
    private long obligationsEnAttente;
    private long obligationsDeposees;
    private long obligationsEnRetard;
    private long totalObligations;
    private double tauxConformite;
    private BigDecimal montantTotalDu;
    private BigDecimal montantTotalPaye;
    private BigDecimal montantTotalARecouvrer;
    private Map<String, Object> details;
}

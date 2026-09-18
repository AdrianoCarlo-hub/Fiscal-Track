package mg.dgi.fiscaltrack.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObligationResponse {

    private Long idObligationFiscale;
    private String nif;
    private String codeImpot;
    private String idAgent;
    private Long idJourFerie;
    private String periodeFiscale;
    private LocalDate dateLimiteTheorique;
    private LocalDate dateLimiteReelle;
    private String statutDeclaration;
    private LocalDate dateDepotEffectif;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

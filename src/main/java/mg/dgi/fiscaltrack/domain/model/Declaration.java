package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.dgi.fiscaltrack.domain.enums.StatutValidation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Declaration {

    private Long idDeclaration;
    private Long idObligationFiscale;
    private BigDecimal chiffreAffairesDeclare;
    private BigDecimal impotPrincipalCalcule;
    private OffsetDateTime dateSoumission;
    private StatutValidation statutValidation;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompteCourantFiscal {

    private Long idCompte;
    private Long idDeclaration;
    private String nif;
    private OffsetDateTime dateCreationLigne;
    private BigDecimal montantPrincipal;
    private BigDecimal montantPenalites;
    private BigDecimal montantPaye;
    private BigDecimal montantTotalDu;
    private BigDecimal resteARecouvrer;
    private StatutRecouvrement statutRecouvrement;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

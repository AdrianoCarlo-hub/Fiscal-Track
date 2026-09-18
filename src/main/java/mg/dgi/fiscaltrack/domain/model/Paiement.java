package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.dgi.fiscaltrack.domain.enums.ModePaiement;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paiement {

    private Long idPaiement;
    private Long idCompte;
    private BigDecimal montantVerse;
    private OffsetDateTime datePaiement;
    private ModePaiement modePaiement;
    private String referenceTransaction;
    private OffsetDateTime createdAt;
}

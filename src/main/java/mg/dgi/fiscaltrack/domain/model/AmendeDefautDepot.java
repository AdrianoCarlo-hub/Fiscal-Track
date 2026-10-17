package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Amende pour defaut de depot d'une obligation fiscale (CDI 20.01.52).
 *
 * Une seule amende par obligation (contrainte d'unicite en base). Elle est
 * stockee separement des interets de retard (CDI 20.01.53) et reportee dans
 * le total du du compte courant fiscal a la declaration.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmendeDefautDepot {

    private Long idAmende;
    private Long idObligationFiscale;
    private String nif;
    private BigDecimal chiffreAffaires;
    private BigDecimal montant;
    private OffsetDateTime dateCalcul;
    private OffsetDateTime createdAt;
}

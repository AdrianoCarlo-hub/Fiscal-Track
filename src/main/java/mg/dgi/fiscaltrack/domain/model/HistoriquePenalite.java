package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Ligne d'historique pour la tracabilite des penalites et majorations
 * appliquees a un compte courant fiscal.
 *
 * Chaque recalcul ajoute une ligne (jamais de mise a jour destructive) :
 * on conserve la date, le mois de retard, le taux, la base et le montant
 * a chaque etape.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriquePenalite {

    /** Type de ligne : PENALITE_RETARD, MAJORATION, AMENDE, REMISE_GRACIEUSE, VERSEMENT. */
    public static final String TYPE_PENALITE_RETARD = "PENALITE_RETARD";
    public static final String TYPE_MAJORATION = "MAJORATION";
    public static final String TYPE_AMENDE = "AMENDE";
    public static final String TYPE_REMISE_GRACIEUSE = "REMISE_GRACIEUSE";

    private Long idHistoriquePenalite;
    private Long idCompte;
    private String typeLigne;
    private OffsetDateTime dateCalcul;
    private Integer moisRetard;
    private BigDecimal taux;
    private BigDecimal base;
    private BigDecimal montant;
    private OffsetDateTime createdAt;
}

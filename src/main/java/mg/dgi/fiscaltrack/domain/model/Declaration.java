package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.dgi.fiscaltrack.domain.enums.CategorieActivite;
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

    // === Champs ajoutes pour le moteur de calcul fiscal ===

    /** Base imposable avant application du taux. */
    private BigDecimal baseImposable;

    /** Taux applique (0.2000, 0.0500, etc.). */
    private BigDecimal tauxApplique;

    /** Montant brut avant application du minimum. */
    private BigDecimal montantBrut;

    /** Minimum forfaitaire applicable. */
    private BigDecimal montantMinimum;

    /** Total des acomptes deduits. */
    private BigDecimal acomptesDeduits;

    /** Credit fiscal genere. */
    private BigDecimal creditFiscal;

    /** Categorie d'activite (pour IR et IS). */
    private CategorieActivite categorieActivite;

    /** Detail du calcul (texte lisible). */
    private String detailCalcul;
}

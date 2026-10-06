package mg.dgi.fiscaltrack.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "declarations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclarationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_declaration")
    private Long idDeclaration;

    @Column(name = "id_obligation_fiscale", nullable = false, unique = true)
    private Long idObligationFiscale;

    @Column(name = "chiffre_affaires_declare", nullable = false, precision = 15, scale = 2)
    private BigDecimal chiffreAffairesDeclare;

    @Column(name = "impot_principal_calcule", nullable = false, precision = 15, scale = 2)
    private BigDecimal impotPrincipalCalcule;

    @Column(name = "date_soumission", nullable = false)
    private OffsetDateTime dateSoumission;

    @Column(name = "statut_validation", nullable = false)
    private String statutValidation;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // === Nouveaux champs pour le moteur de calcul fiscal ===

    @Column(name = "base_imposable", precision = 15, scale = 2)
    private BigDecimal baseImposable;

    @Column(name = "taux_applique", precision = 5, scale = 4)
    private BigDecimal tauxApplique;

    @Column(name = "montant_brut", precision = 15, scale = 2)
    private BigDecimal montantBrut;

    @Column(name = "montant_minimum", precision = 15, scale = 2)
    private BigDecimal montantMinimum;

    @Column(name = "acomptes_deduits", precision = 15, scale = 2)
    private BigDecimal acomptesDeduits;

    @Column(name = "credit_fiscal", precision = 15, scale = 2)
    private BigDecimal creditFiscal;

    @Column(name = "categorie_activite", length = 50)
    private String categorieActivite;

    @Column(name = "detail_calcul", columnDefinition = "TEXT")
    private String detailCalcul;
}

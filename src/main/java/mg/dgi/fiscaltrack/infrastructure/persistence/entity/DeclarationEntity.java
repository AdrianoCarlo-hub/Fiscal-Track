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
}

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
import org.hibernate.annotations.Generated;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "comptes_courants_fiscaux")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompteCourantFiscalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compte")
    private Long idCompte;

    @Column(name = "id_declaration", nullable = false, unique = true)
    private Long idDeclaration;

    @Column(name = "nif", nullable = false, length = 10)
    private String nif;

    @Column(name = "date_creation_ligne", nullable = false)
    private OffsetDateTime dateCreationLigne;

    @Column(name = "montant_principal", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantPrincipal;

    @Column(name = "montant_penalites", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantPenalites;

    @Column(name = "montant_paye", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantPaye;

    @Generated
    @Column(name = "montant_total_du", insertable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal montantTotalDu;

    @Generated
    @Column(name = "reste_a_recouvrer", insertable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal resteARecouvrer;

    @Column(name = "statut_recouvrement", nullable = false)
    private String statutRecouvrement;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

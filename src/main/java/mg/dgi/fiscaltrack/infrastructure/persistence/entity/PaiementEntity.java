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
@Table(name = "paiements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paiement")
    private Long idPaiement;

    @Column(name = "id_compte", nullable = false)
    private Long idCompte;

    @Column(name = "montant_verse", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantVerse;

    @Column(name = "date_paiement", nullable = false)
    private OffsetDateTime datePaiement;

    @Column(name = "mode_paiement", nullable = false)
    private String modePaiement;

    @Column(name = "reference_transaction", unique = true)
    private String referenceTransaction;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

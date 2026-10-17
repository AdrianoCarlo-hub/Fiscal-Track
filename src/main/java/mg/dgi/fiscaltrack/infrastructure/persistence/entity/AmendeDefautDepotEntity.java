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
@Table(name = "amendes_defaut_depot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmendeDefautDepotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_amende")
    private Long idAmende;

    @Column(name = "id_obligation_fiscale", nullable = false)
    private Long idObligationFiscale;

    @Column(name = "nif", nullable = false, length = 10)
    private String nif;

    @Column(name = "chiffre_affaires", precision = 15, scale = 2)
    private BigDecimal chiffreAffaires;

    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_calcul", nullable = false)
    private OffsetDateTime dateCalcul;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

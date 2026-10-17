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
@Table(name = "historique_penalites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriquePenaliteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historique_penalite")
    private Long idHistoriquePenalite;

    @Column(name = "id_compte", nullable = false)
    private Long idCompte;

    @Column(name = "type_ligne", nullable = false, length = 30)
    private String typeLigne;

    @Column(name = "date_calcul", nullable = false)
    private OffsetDateTime dateCalcul;

    @Column(name = "mois_retard")
    private Integer moisRetard;

    @Column(name = "taux", precision = 8, scale = 4)
    private BigDecimal taux;

    @Column(name = "base", precision = 15, scale = 2)
    private BigDecimal base;

    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

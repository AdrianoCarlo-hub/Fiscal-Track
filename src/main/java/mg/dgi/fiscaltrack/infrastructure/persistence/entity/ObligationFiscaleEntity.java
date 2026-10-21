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
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "obligations_fiscales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObligationFiscaleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_obligation_fiscale")
    private Long idObligationFiscale;

    @Column(name = "nif", nullable = false, length = 10)
    private String nif;

    @Column(name = "code_impot", nullable = false, length = 30)
    private String codeImpot;

    @Column(name = "id_agent", length = 6)
    private String idAgent;

    @Column(name = "id_jour_ferie")
    private Long idJourFerie;

    @Column(name = "periode_fiscale", nullable = false)
    private String periodeFiscale;

    @Column(name = "date_limite_theorique", nullable = false)
    private LocalDate dateLimiteTheorique;

    @Column(name = "date_limite_reelle", nullable = false)
    private LocalDate dateLimiteReelle;

    @Column(name = "statut_declaration", nullable = false)
    private String statutDeclaration;

    @Column(name = "date_depot_effectif")
    private LocalDate dateDepotEffectif;

    @Column(name = "montant_acompte", precision = 15, scale = 2)
    private BigDecimal montantAcompte;

    @Column(name = "base_acompte", precision = 15, scale = 2)
    private BigDecimal baseAcompte;

    @Column(name = "taux_acompte", precision = 10, scale = 4)
    private BigDecimal tauxAcompte;

    @Column(name = "exercice_base_acompte")
    private Integer exerciceBaseAcompte;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

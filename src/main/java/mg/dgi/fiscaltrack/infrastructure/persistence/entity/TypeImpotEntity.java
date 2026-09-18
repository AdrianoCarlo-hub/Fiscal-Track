package mg.dgi.fiscaltrack.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "types_impots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeImpotEntity {

    @Id
    @Column(name = "code_impot", length = 30)
    private String codeImpot;

    @Column(name = "nom_impot", nullable = false)
    private String nomImpot;

    @Column(name = "periodicite", nullable = false)
    private String periodicite;

    @Column(name = "echeance_theorique_jour")
    private Integer echeanceTheoriqueJour;

    @Column(name = "echeance_theorique_mois")
    private Integer echeanceTheoriqueMois;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

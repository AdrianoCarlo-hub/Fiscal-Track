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

import java.math.BigDecimal;
import java.time.Year;

/**
 * Entite JPA pour les parametres fiscaux.
 * Toutes les valeurs fiscales (taux, seuils, minima, plafonds) doivent etre stockes ici.
 */
@Entity
@Table(name = "parametres_fiscaux")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametreFiscalEntity {

    @Id
    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "valeur", nullable = false, precision = 20, scale = 4)
    private BigDecimal valeur;

    @Column(name = "annee_debut", nullable = false)
    private Year anneeDebut;

    @Column(name = "annee_fin")
    private Year anneeFin;

    @Column(name = "base_legale", length = 100, nullable = false)
    private String baseLegale;

    @Column(name = "secteur", length = 100)
    private String secteur;

    @Column(name = "description", length = 255)
    private String description;
}

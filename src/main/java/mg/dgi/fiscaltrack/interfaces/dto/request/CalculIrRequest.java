package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CalculIrRequest {

    @NotNull(message = "Le chiffre d'affaires HT est obligatoire")
    @PositiveOrZero(message = "Le chiffre d'affaires HT doit etre positif ou nul")
    private BigDecimal caHT;

    private BigDecimal chargesDeductibles;
    private BigDecimal reintegrations;
    private BigDecimal deductions;

    /**
     * Montant unique de deficit reportable (compatibilite). Traite comme un
     * deficit de l'exercice precedent (anneeExercice - 1) lorsque {@link #deficits}
     * n'est pas renseigne.
     */
    private BigDecimal deficitsReportables;

    /**
     * Deficits reportables structures, rattaches a leur exercice d'origine
     * (report limite a 5 exercices, CDI 01.01.10 11°).
     */
    private List<DeficitRequest> deficits;

    /** Exercice d'imposition courant (defaut : annee en cours). */
    private Integer anneeExercice;

    @NotNull(message = "La categorie d'activite est obligatoire")
    private String categorieActivite;

    private BigDecimal acomptes;

    @PositiveOrZero(message = "Les mois actifs doivent etre positifs")
    private Integer moisActifs;

    /**
     * Autorisation d'ouverture renseignee : condition du regime reduit education/sante
     * (CDI 01.01.05 V, 01.01.14 V). Sans autorisation, regime general (20%).
     */
    private Boolean autorisationOuverture;

    /**
     * Option sante explicite : en plus de l'autorisation, condition du regime
     * reduit pour la categorie SANTE uniquement.
     */
    private Boolean optionSante;

    @Data
    public static class DeficitRequest {
        private Integer anneeOrigine;
        private BigDecimal montant;
    }
}

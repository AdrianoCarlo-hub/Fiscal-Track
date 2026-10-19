package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Donnees de depot / previsualisation d'une declaration.
 *
 * Le formulaire s'adapte au code impot de l'obligation : chaque type d'impot
 * a ses propres champs (facultatifs ici, valides cote serveur selon le type).
 * Le champ historique {@code chiffreAffairesDeclare} reste accepte pour les
 * anciens clients : le serveur privilegie toujours les champs dedies au type.
 *
 * Aucune valeur saisie par le client ne fait foi : le serveur recalcule
 * l'impot principal et le compare a l'apercu affiche.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeclarationRequest {

    @NotNull(message = "L'identifiant de l'obligation est obligatoire")
    private Long idObligationFiscale;

    /**
     * Chiffre d'affaires declare (ancien champ unique). Facultatif : le
     * serveur le recalcule a partir des champs dedies au type d'impot.
     */
    @PositiveOrZero(message = "Le chiffre d'affaires ne peut pas etre negatif")
    private BigDecimal chiffreAffairesDeclare;

    /** Montant affiche a l'ecran par la previsualisation (uniquement compare). */
    @NotNull(message = "L'impot principal calcule est obligatoire")
    @PositiveOrZero(message = "L'impot principal ne peut pas etre negatif")
    private BigDecimal impotPrincipalCalcule;

    /** Acomptes deja verses (utilises pour l'IS et pour l'IR). */
    @PositiveOrZero(message = "Les acomptes verses ne peuvent pas etre negatifs")
    private BigDecimal acomptes;

    // ==================== IS (impot synthetique) ====================

    @PositiveOrZero(message = "Le chiffre d'affaires brut encaisse ne peut pas etre negatif")
    private BigDecimal caBrut;

    private Boolean adherentCga;

    @PositiveOrZero(message = "Les achats regularises ne peuvent pas etre negatifs")
    private BigDecimal achatsConformes;

    @PositiveOrZero(message = "Les charges salariales ne peuvent pas etre negatives")
    private BigDecimal chargesSalariales;

    private String categorieActiviteIs;

    // ==================== TVA ====================

    @PositiveOrZero(message = "Le chiffre d'affaires imposable localement ne peut pas etre negatif")
    private BigDecimal caLocal;

    @PositiveOrZero(message = "Le chiffre d'affaires a l'exportation ne peut pas etre negatif")
    private BigDecimal caExportation;

    private Boolean gazButane;

    @PositiveOrZero(message = "La TVA deductible sur les biens et services ne peut pas etre negative")
    private BigDecimal tvaDeductibleBiens;

    @PositiveOrZero(message = "La TVA deductible sur les immobilisations ne peut pas etre negative")
    private BigDecimal tvaDeductibleImmobilisations;

    @PositiveOrZero(message = "Le credit de TVA anterieur ne peut pas etre negatif")
    private BigDecimal creditTvaAnterieur;

    // ==================== IR ====================

    @PositiveOrZero(message = "Le chiffre d'affaires HT ne peut pas etre negatif")
    private BigDecimal caHT;

    @PositiveOrZero(message = "Les charges deductibles ne peuvent pas etre negatives")
    private BigDecimal chargesDeductibles;

    @PositiveOrZero(message = "Les reintegrations extra-comptables ne peuvent pas etre negatives")
    private BigDecimal reintegrations;

    @PositiveOrZero(message = "Les deductions extra-comptables ne peuvent pas etre negatives")
    private BigDecimal deductions;

    @Valid
    @Size(max = 10, message = "Au plus 10 deficits reportables (report limite a 5 exercices)")
    private List<DeficitRequest> deficits;

    private String categorieActivite;

    @PositiveOrZero(message = "Le nombre de mois d'activite ne peut pas etre negatif")
    private Integer moisActifs;

    // ==================== IRSA (par l'employeur) ====================

    @Size(max = 50, message = "Au plus 50 salaries par declaration")
    @Valid
    private List<LigneSalarieRequest> salaries;

    // ==================== Sous-objets ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeficitRequest {
        private Integer anneeOrigine;

        @NotNull(message = "Le montant du deficit est obligatoire")
        @PositiveOrZero(message = "Le montant du deficit ne peut pas etre negatif")
        private BigDecimal montant;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LigneSalarieRequest {
        /** Nom ou matricule du salarie (affichage uniquement). */
        private String nom;

        @NotNull(message = "Le salaire brut mensuel est obligatoire pour chaque salarie")
        @PositiveOrZero(message = "Le salaire brut ne peut pas etre negatif")
        private BigDecimal salaireBrut;

        @PositiveOrZero(message = "Les cotisations sociales ne peuvent pas etre negatives")
        private BigDecimal cotisationsSociales;

        @PositiveOrZero(message = "L'avantage logement ne peut pas etre negatif")
        private BigDecimal avantageLogement;

        @PositiveOrZero(message = "L'avantage vehicule ne peut pas etre negatif")
        private BigDecimal avantageVehicule;

        @PositiveOrZero(message = "L'avantage telephone ne peut pas etre negatif")
        private BigDecimal avantageTelephone;

        @PositiveOrZero(message = "Le nombre de personnes a charge ne peut pas etre negatif")
        private Integer personnesCharge;
    }
}

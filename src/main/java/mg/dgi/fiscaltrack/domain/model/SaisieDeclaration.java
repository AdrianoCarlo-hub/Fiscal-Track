package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Saisie d'une declaration, telle qu'elle est envoyee par le formulaire du
 * contribuable. Objet pur du domaine : il est construit a partir du DTO HTTP
 * (cote interfaces) puis valide et transforme en parametres de calcul par
 * {@code PreparerDonneesDeclarationUseCase}.
 *
 * Tous les champs sont facultatifs ici : leur presence est verifiee selon le
 * code impot de l'obligation, avec des messages en francais.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaisieDeclaration {

    private Long idObligationFiscale;

    /** Montant affiche a l'ecran lors de la previsualisation (jamais retenu seul). */
    private BigDecimal impotPrincipalCalcule;

    /** Ancien champ unique (chiffre d'affaires). Sert de repli si les champs dedies sont absents. */
    private BigDecimal chiffreAffairesDeclare;

    // ===== IS =====
    private BigDecimal caBrut;
    private Boolean adherentCga;
    private BigDecimal achatsConformes;
    private BigDecimal chargesSalariales;
    private String categorieActiviteIs;

    /** Ancien champ libre des acomptes : ignore pour la liquidation (serveur fait foi). */
    private BigDecimal acomptes;

    /** Acomptes de douane saisis (facultatif, R5). */
    private BigDecimal acomptesDouane;

    /** Acomptes marches publics saisis (facultatif, R5). */
    private BigDecimal acomptesMarchesPublics;

    // ===== TVA =====
    private BigDecimal caLocal;
    private BigDecimal caExportation;
    private Boolean gazButane;
    private BigDecimal tvaDeductibleBiens;
    private BigDecimal tvaDeductibleImmobilisations;
    private BigDecimal creditTvaAnterieur;

    // ===== IR =====
    private BigDecimal caHT;
    private BigDecimal chargesDeductibles;
    private BigDecimal reintegrations;
    private BigDecimal deductions;
    private List<DeficitReportable> deficits;
    private String categorieActivite;
    private Integer moisActifs;

    // ===== IRSA =====
    private List<LigneSalarie> salaries;

    /** Une ligne du tableau des salaries (IRSA). */
    public record LigneSalarie(String nom,
                               BigDecimal salaireBrut,
                               BigDecimal cotisationsSociales,
                               BigDecimal avantageLogement,
                               BigDecimal avantageVehicule,
                               BigDecimal avantageTelephone,
                               Integer personnesCharge) {
    }
}

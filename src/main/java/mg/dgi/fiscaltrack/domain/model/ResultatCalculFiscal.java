package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.dgi.fiscaltrack.domain.enums.CategorieActivite;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultat d'un calcul fiscal. Contient non seulement le montant final,
 * mais aussi tous les elements intermediaires permettant a l'utilisateur
 * de comprendre comment le systeme est arrive au resultat.
 *
 * Objet pur du domaine, sans dependance a Spring ou JPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultatCalculFiscal {

    /** Code de l'impot concerne : IR, IS, IRSA, TVA. */
    private String typeImpot;

    /** Periode fiscale concernee (ex: 2026-10 ou 2025). */
    private String periode;

    /** Categorie d'activite utilisee (pour IR et IS). */
    private CategorieActivite categorieActivite;

    /** Base imposable avant application du taux. */
    private BigDecimal baseImposable;

    /** Taux applique (ex: 0.2000 pour 20 %). */
    private BigDecimal tauxApplique;

    /** Montant brut avant application du minimum. */
    private BigDecimal montantBrut;

    /** Minimum forfaitaire applicable (nullable si non applicable). */
    private BigDecimal montantMinimum;

    /** Reductions d'impot (utilise pour l'IS). */
    private BigDecimal reductions;

    /** Total des acomptes deja verses. */
    private BigDecimal acomptesDeduits;

    /** Penalites applicables (si en retard). */
    private BigDecimal penalites;

    /** Credit fiscal genere (si acomptes > impot du). */
    private BigDecimal creditFiscal;

    /** Montant final a payer (apres minimum, acomptes, credit). */
    private BigDecimal solde;

    /** Explication textuelle du calcul. */
    private String explication;

    /** Detail etape par etape du calcul. */
    @Builder.Default
    private List<String> etapesCalcul = new ArrayList<>();
}

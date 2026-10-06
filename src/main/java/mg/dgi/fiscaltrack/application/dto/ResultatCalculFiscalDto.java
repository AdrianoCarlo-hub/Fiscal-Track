package mg.dgi.fiscaltrack.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Objet de transfert interne pour le resultat d'un calcul fiscal.
 * Sert d'interface entre les use cases de calcul et les controllers.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultatCalculFiscalDto {

    private String typeImpot;
    private String periode;
    private String categorieActivite;
    private BigDecimal baseImposable;
    private BigDecimal tauxApplique;
    private BigDecimal montantBrut;
    private BigDecimal montantMinimum;
    private BigDecimal reductions;
    private BigDecimal acomptesDeduits;
    private BigDecimal penalites;
    private BigDecimal creditFiscal;
    private BigDecimal solde;
    private String explication;

    @Builder.Default
    private List<String> etapesCalcul = new ArrayList<>();
}

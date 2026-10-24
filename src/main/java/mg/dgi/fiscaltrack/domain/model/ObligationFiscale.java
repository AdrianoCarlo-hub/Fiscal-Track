package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.dgi.fiscaltrack.domain.enums.StatutDeclaration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObligationFiscale {

    private Long idObligationFiscale;
    private String nif;
    private String codeImpot;
    private String idAgent;
    private Long idJourFerie;
    private String periodeFiscale;
    private LocalDate dateLimiteTheorique;
    private LocalDate dateLimiteReelle;
    private StatutDeclaration statutDeclaration;
    private LocalDate dateDepotEffectif;

    /** Apercu du montant de l'acompte calcule (jamais source de verite). */
    private BigDecimal montantAcompte;

    /** Apercu de la base (impot de reference) utilisee pour l'apercu. */
    private BigDecimal baseAcompte;

    /** Apercu du taux applique par acompte. */
    private BigDecimal tauxAcompte;

    /** Exercice de la declaration servant de base a l'apercu. */
    private Integer exerciceBaseAcompte;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

package mg.dgi.fiscaltrack.application.usecase.notification;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Donnees utilitaires pour construire le texte d'une notification.
 * Tous les champs sont facultatifs selon le type de notification.
 */
@Getter
@Builder
public class ContexteNotification {

    private String nif;
    private String raisonSociale;
    private String codeImpot;
    private String nomImpot;
    private String periode;
    private LocalDate echeance;
    /** Montant du sur le compte courant fiscal (si connu). */
    private BigDecimal montantDu;
    /** Montant deja paye (si connu). */
    private BigDecimal montantPaye;
    /** Montant retenu par le recalcul serveur (declaration validee). */
    private BigDecimal montantRetenu;
    /** Reference de transaction (paiement). */
    private String reference;
    /** Motif de rejet (declaration rejetee). */
    private String motif;
    /** Nature du texte : penalite de retard, amende pour defaut de depot... */
    private String nature;
    /** Montant propre au texte (penalite, amende). */
    private BigDecimal montant;

    /** Contenu du message libre (envoi manuel). */
    private String messageLibre;
}

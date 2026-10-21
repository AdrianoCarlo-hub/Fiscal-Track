package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Construction des textes de notification (e-mail, SMS, portail).
 *
 * - E-mail / portail : francais accentue, avec NIF, impot, periode,
 *   echeance et montant du quand il est connu.
 * - SMS : 160 caracteres maximum, sans accent ni caractere special
 *   (translitteration), sans montant detaille (le detail reste dans
 *   l'e-mail et sur le portail).
 */
@Service
public class NotificationTemplateService {

    static final int LONGUEUR_MAX_SMS = 160;

    private static final DateTimeFormatter DATE_COURTE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);

    public ContenuMessage construire(TypeRelance type, ContexteNotification ctx) {
        return new ContenuMessage(sujet(type, ctx), corpsEmail(type, ctx), corpsSms(type, ctx));
    }

    private String sujet(TypeRelance type, ContexteNotification ctx) {
        String libelle = switch (type) {
            case J_MOINS_7 -> "Rappel d'échéance (J-7)";
            case J_MOINS_1 -> "Rappel d'échéance (J-1)";
            case J_PLUS_1 -> "Avis de retard (J+1)";
            case J_PLUS_8 -> "Lettre de relance (J+8)";
            case DECLARATION_VALIDEE -> "Déclaration validée";
            case DECLARATION_REJETEE -> "Déclaration rejetée";
            case PAIEMENT_RECU -> "Paiement enregistré";
            case PENALITE_APPLIQUEE -> "Pénalité de retard appliquée";
            case AMENDE_APPLIQUEE -> "Amende pour défaut de dépôt";
            case MESSAGE_LIBRE -> "Message des Impôts";
        };
        String impot = nomImpot(ctx);
        if (impot != null && ctx.getPeriode() != null) {
            return "Fiscal-Track - " + libelle + " - " + impot + " (" + ctx.getPeriode() + ")";
        }
        return "Fiscal-Track - " + libelle;
    }

    private String corpsEmail(TypeRelance type, ContexteNotification ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bonjour");

        if (ctx.getRaisonSociale() != null && !ctx.getRaisonSociale().isBlank()) {
            sb.append(" ").append(ctx.getRaisonSociale().trim());
        }
        sb.append(",\n\n");

        String impot = nomImpot(ctx);
        String periode = ctx.getPeriode() == null ? "" : " pour la période " + ctx.getPeriode();

        switch (type) {
            case J_MOINS_7, J_MOINS_1 -> {
                sb.append("Sauf erreur de notre part, votre déclaration ")
                        .append(libelleImpot(impot))
                        .append(" (NIF ").append(nif(ctx)).append(")")
                        .append(periode)
                        .append(" doit être déposée avant le ")
                        .append(date(ctx.getEcheance())).append(".\n");
                ajouterMontantDu(sb, ctx);
                sb.append("\nNous vous invitons à vous connecter à votre espace Fiscal-Track ")
                        .append("pour effectuer votre dépôt.\n");
            }
            case J_PLUS_1 -> {
                sb.append("Votre déclaration ")
                        .append(libelleImpot(impot))
                        .append(" (NIF ").append(nif(ctx)).append(")")
                        .append(periode)
                        .append(" n'a pas été déposée. L'échéance était fixée au ")
                        .append(date(ctx.getEcheance())).append(".\n");
                ajouterMontantDu(sb, ctx);
                sb.append("\nNous vous mettons en demeure de régulariser votre situation ")
                        .append("sans délai.\n");
            }
            case J_PLUS_8 -> {
                sb.append("Votre déclaration ")
                        .append(libelleImpot(impot))
                        .append(" (NIF ").append(nif(ctx)).append(")")
                        .append(periode)
                        .append(" n'a toujours pas été déposée, ")
                        .append("8 jours après l'échéance du ")
                        .append(date(ctx.getEcheance())).append(".\n");
                ajouterMontantDu(sb, ctx);
                sb.append("\nUn délai franc de 8 jours vous est accordé pour régulariser. ")
                        .append("À défaut, des poursuites seront engagées.\n");
            }
            case DECLARATION_VALIDEE -> {
                sb.append("Votre déclaration ")
                        .append(libelleImpot(impot))
                        .append(" (NIF ").append(nif(ctx)).append(")")
                        .append(periode)
                        .append(" a été validée par le service des Impôts.\n");
                if (ctx.getMontantRetenu() != null) {
                    sb.append("Montant retenu : ").append(montant(ctx.getMontantRetenu())).append(".\n");
                }
                sb.append("\nLe détail est disponible dans votre espace Fiscal-Track.\n");
            }
            case DECLARATION_REJETEE -> {
                sb.append("Votre déclaration ")
                        .append(libelleImpot(impot))
                        .append(" (NIF ").append(nif(ctx)).append(")")
                        .append(periode)
                        .append(" a été rejetée.\n");
                if (ctx.getMotif() != null && !ctx.getMotif().isBlank()) {
                    sb.append("Motif du rejet : ").append(ctx.getMotif().trim()).append("\n");
                }
                sb.append("\nVous pouvez déposer une nouvelle déclaration depuis votre espace ")
                        .append("Fiscal-Track.\n");
            }
            case PAIEMENT_RECU -> {
                sb.append("Nous accusons réception de votre paiement");
                if (ctx.getMontantPaye() != null) {
                    sb.append(" de ").append(montant(ctx.getMontantPaye()));
                }
                sb.append(" (NIF ").append(nif(ctx)).append(")");
                if (ctx.getReference() != null && !ctx.getReference().isBlank()) {
                    sb.append(", référence ").append(ctx.getReference().trim());
                }
                sb.append(".\n");
                if (ctx.getMontantDu() != null) {
                    sb.append("Reste à payer : ").append(montant(ctx.getMontantDu())).append(".\n");
                }
                sb.append("\nMerci de votre règlement.\n");
            }
            case PENALITE_APPLIQUEE -> {
                sb.append("Une pénalité de retard a été appliquée sur votre compte courant fiscal")
                        .append(" (NIF ").append(nif(ctx)).append(")").append(periode).append(".\n");
                if (ctx.getNature() != null) {
                    sb.append("Nature : ").append(ctx.getNature()).append(".\n");
                }
                if (ctx.getMontant() != null) {
                    sb.append("Montant de la pénalité : ").append(montant(ctx.getMontant())).append(".\n");
                }
                sb.append("\nLe détail est consultable dans votre espace Fiscal-Track.\n");
            }
            case AMENDE_APPLIQUEE -> {
                sb.append("Une amende pour défaut de dépôt a été appliquée sur votre compte ")
                        .append("courant fiscal (NIF ").append(nif(ctx)).append(")")
                        .append(periode).append(".\n");
                if (ctx.getNature() != null) {
                    sb.append("Nature : ").append(ctx.getNature()).append(".\n");
                }
                if (ctx.getMontant() != null) {
                    sb.append("Montant de l'amende : ").append(montant(ctx.getMontant())).append(".\n");
                }
                sb.append("\nLe détail est consultable dans votre espace Fiscal-Track.\n");
            }
            case MESSAGE_LIBRE -> {
                if (ctx.getMessageLibre() != null) {
                    sb.append(ctx.getMessageLibre().trim()).append("\n");
                }
            }
        }

        sb.append("\nService des Impôts — Fiscal-Track\n");
        return sb.toString();
    }

    private String corpsSms(TypeRelance type, ContexteNotification ctx) {
        String nif = nif(ctx);
        String impot = court(nomImpot(ctx), 12);
        String periode = court(ctx.getPeriode(), 16);
        String echeance = date(ctx.getEcheance());

        String texte = switch (type) {
            case J_MOINS_7, J_MOINS_1 ->
                    "Fiscal-Track: NIF " + nif + ". Rappel " + impot + " " + periode
                            + ", echeance " + echeance + ". A deposer sur le portail.";
            case J_PLUS_1 ->
                    "Fiscal-Track: NIF " + nif + ". Avis de retard " + impot + " " + periode
                            + ". Echeance depassee le " + echeance + ". Consultez le portail.";
            case J_PLUS_8 ->
                    "Fiscal-Track: NIF " + nif + ". Relance " + impot + " " + periode
                            + ". Echeance depassee. Consultez le portail.";
            case DECLARATION_VALIDEE ->
                    "Fiscal-Track: NIF " + nif + ". Declaration " + impot + " " + periode
                            + " validee. Consultez le portail.";
            case DECLARATION_REJETEE ->
                    "Fiscal-Track: NIF " + nif + ". Declaration " + impot + " " + periode
                            + " rejetee. Motif sur le portail.";
            case PAIEMENT_RECU ->
                    "Fiscal-Track: NIF " + nif + ". Paiement enregistre. Consultez le portail.";
            case PENALITE_APPLIQUEE ->
                    "Fiscal-Track: NIF " + nif + ". Penalite de retard appliquee. Detail sur le portail.";
            case AMENDE_APPLIQUEE ->
                    "Fiscal-Track: NIF " + nif + ". Amende pour defaut de depot. Detail sur le portail.";
            case MESSAGE_LIBRE -> {
                String libre = ctx.getMessageLibre() == null ? "" : ctx.getMessageLibre().trim();
                yield "Fiscal-Track: NIF " + nif + ". " + libre;
            }
        };

        return limiterSms(translitterer(texte));
    }

    /**
     * Translitteration en ASCII pur : accents retires, ligatures deplacees,
     * tout caractere restant non-ASCII supprime.
     */
    static String translitterer(String texte) {
        if (texte == null) {
            return "";
        }
        String sansLigatures = texte
                .replace("œ", "oe").replace("Œ", "OE")
                .replace("æ", "ae").replace("Æ", "AE")
                .replace("’", "'").replace("–", "-").replace("—", "-");
        String deaccentue = Normalizer.normalize(sansLigatures, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return deaccentue.replaceAll("[^\\x20-\\x7E]", "");
    }

    /** Limite le SMS a 160 caracteres, en coupant sur une frontiere de mot si possible. */
    static String limiterSms(String texte) {
        if (texte == null || texte.length() <= LONGUEUR_MAX_SMS) {
            return texte == null ? "" : texte;
        }
        String tronque = texte.substring(0, LONGUEUR_MAX_SMS - 3);
        int dernierEspace = tronque.lastIndexOf(' ');
        if (dernierEspace > LONGUEUR_MAX_SMS / 2) {
            tronque = tronque.substring(0, dernierEspace);
        }
        return tronque + "...";
    }

    private void ajouterMontantDu(StringBuilder sb, ContexteNotification ctx) {
        if (ctx.getMontantDu() != null) {
            sb.append("Montant dû : ").append(montant(ctx.getMontantDu())).append(".\n");
        }
    }

    private String nomImpot(ContexteNotification ctx) {
        if (ctx.getNomImpot() != null && !ctx.getNomImpot().isBlank()) {
            return ctx.getNomImpot().trim();
        }
        return ctx.getCodeImpot();
    }

    private String libelleImpot(String impot) {
        return impot == null || impot.isBlank() ? "fiscale" : impot;
    }

    private String nif(ContexteNotification ctx) {
        return ctx.getNif() == null ? "" : ctx.getNif();
    }

    private String date(LocalDate d) {
        return d == null ? "" : DATE_COURTE.format(d);
    }

    private String montant(BigDecimal m) {
        return m.stripTrailingZeros().toPlainString() + " Ar";
    }

    private String court(String valeur, int max) {
        if (valeur == null) {
            return "";
        }
        String v = valeur.trim();
        return v.length() <= max ? v : v.substring(0, max);
    }
}

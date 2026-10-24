package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Modeles de notification conformes a l'annexe : objet explicite, montants
 * formates "1 234 000 Ar", dates jj/MM/aaaa et SMS de 160 caracteres maximum
 * sans accent.
 */
class NotificationTemplateServiceTest {

    private final NotificationTemplateService service = new NotificationTemplateService();

    private ContexteNotification contexte() {
        return ContexteNotification.builder()
                .nif("1234567890")
                .raisonSociale("SARL Exemple")
                .codeImpot("IR")
                .nomImpot("Impôt sur les Revenus")
                .periode("2026")
                .echeance(LocalDate.of(2026, 5, 15))
                .montantDu(new BigDecimal("1234000"))
                .montantPaye(new BigDecimal("500000"))
                .montantRetenu(new BigDecimal("1234000"))
                .reference("TXN-2026-0001")
                .motif("Pièce justificative manquante")
                .nature("Retard de dépôt")
                .montant(new BigDecimal("50000"))
                .messageLibre("Votre dossier a bien été mis à jour.")
                .build();
    }

    @Test
    @DisplayName("Chaque type produit un objet, un e-mail et un SMS non vides")
    void chaqueTypeProduitUnContenu() {
        for (TypeRelance type : TypeRelance.values()) {
            ContenuMessage contenu = service.construire(type, contexte());
            assertNotNull(contenu);
            assertFalse(contenu.sujet() == null || contenu.sujet().isBlank(),
                    "objet vide pour " + type);
            assertFalse(contenu.corpsEmail() == null || contenu.corpsEmail().isBlank(),
                    "corps e-mail vide pour " + type);
            assertFalse(contenu.corpsSms() == null || contenu.corpsSms().isBlank(),
                    "corps SMS vide pour " + type);
        }
    }

    @Test
    @DisplayName("SMS : 160 caracteres maximum et ASCII pur, pour tous les types")
    void smsLimitesEtSansAccent() {
        for (TypeRelance type : TypeRelance.values()) {
            String sms = service.construire(type, contexte()).corpsSms();
            assertTrue(sms.length() <= NotificationTemplateService.LONGUEUR_MAX_SMS,
                    "SMS trop long pour " + type + " : " + sms.length());
            for (int i = 0; i < sms.length(); i++) {
                char c = sms.charAt(i);
                assertTrue(c >= 32 && c <= 126,
                        "caractere non ASCII dans le SMS " + type + " : " + c);
            }
        }
    }

    @Test
    @DisplayName("Montants au format 1 234 000 Ar et dates au format jj/MM/aaaa")
    void montantsEtDatesFormates() {
        String email = service.construire(TypeRelance.J_MOINS_7, contexte()).corpsEmail();
        assertTrue(email.contains("1 234 000 Ar"), email);
        assertTrue(email.contains("15/05/2026"), email);
    }

    @Test
    @DisplayName("Formatage des montants : milliers, negatifs, centimes non nuls")
    void formatageMontant() {
        assertEquals("1 234 000 Ar", NotificationTemplateService.montant(new BigDecimal("1234000")));
        assertEquals("1 234 000 Ar", NotificationTemplateService.montant(new BigDecimal("1234000.00")));
        assertEquals("50 000 Ar", NotificationTemplateService.montant(new BigDecimal("50000")));
        assertEquals("-1 500 Ar", NotificationTemplateService.montant(new BigDecimal("-1500")));
        assertEquals("1 234,50 Ar", NotificationTemplateService.montant(new BigDecimal("1234.50")));
        assertEquals("", NotificationTemplateService.montant(null));
    }

    @Test
    @DisplayName("E-mail : contient le NIF, l'echeance et la signature du service")
    void emailContientLesDonneesEssentielles() {
        String email = service.construire(TypeRelance.J_PLUS_8, contexte()).corpsEmail();
        assertTrue(email.contains("1234567890"), email);
        assertTrue(email.contains("15/05/2026"), email);
        assertTrue(email.contains("Service des Impôts"), email);
    }

    @Test
    @DisplayName("Message libre : le texte fourni est repris tel quel dans l'e-mail")
    void messageLibreRepris() {
        String email = service.construire(TypeRelance.MESSAGE_LIBRE, contexte()).corpsEmail();
        assertTrue(email.contains("Votre dossier a bien été mis à jour."), email);
    }
}

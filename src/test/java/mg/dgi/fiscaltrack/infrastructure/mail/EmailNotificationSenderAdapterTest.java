package mg.dgi.fiscaltrack.infrastructure.mail;

import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.CanalEnvoi;
import mg.dgi.fiscaltrack.domain.enums.StatutEnvoi;
import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'e-mail envoye contient l'objet du modele, le corps complet et un bouton
 * d'acces au portail (version HTML).
 */
class EmailNotificationSenderAdapterTest {

    private MimeMessage message;
    private JavaMailSender mailSender;
    private EmailNotificationSenderAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        message = new MimeMessage(Session.getInstance(new Properties()));
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(message);

        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        ContribuableRepositoryPort contribuables = mock(ContribuableRepositoryPort.class);
        when(contribuables.findByNif("1234567890")).thenReturn(Optional.of(
                Contribuable.builder()
                        .nif("1234567890")
                        .raisonSociale("SARL Exemple")
                        .emailContribuable("destinataire@exemple.mg")
                        .build()));

        adapter = new EmailNotificationSenderAdapter(
                provider, contribuables, true, "smtp.exemple.mg",
                "no-reply@fiscaltrack.mg", "https://fiscaltrack.mg");
    }

    private Notification notification() {
        return Notification.builder()
                .idNotif(1L)
                .nif("1234567890")
                .typeRelance(TypeRelance.J_MOINS_7)
                .objetEmail("Rappel d'échéance (J-7)")
                .messageContenu("Bonjour SARL Exemple,\n\nVotre déclaration est attendue.")
                .canalEnvoi(CanalEnvoi.EMAIL)
                .statutEnvoi(StatutEnvoi.EN_ATTENTE)
                .dateEnvoiPrevue(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Objet du modele repris tel quel")
    void objetDuModele() throws Exception {
        adapter.envoyer(notification());

        assertEquals("Rappel d'échéance (J-7)", message.getSubject());
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Corps texte et version HTML avec bouton d'acces au portail")
    void corpsEtBouton() throws Exception {
        adapter.envoyer(notification());

        String texte = texteDe(message.getContent());
        String html = htmlDe(message.getContent());

        assertTrue(texte.contains("Votre déclaration est attendue."), texte);
        assertTrue(html.contains("Votre déclaration est attendue."), html);
        assertTrue(html.contains("Accéder à mon espace"), html);
        assertTrue(html.contains("https://fiscaltrack.mg"), html);
    }

    @Test
    @DisplayName("Sans objet stocke : repli sur un objet construit a partir du type")
    void repliSansObjet() throws Exception {
        Notification sansObjet = notification();
        sansObjet.setObjetEmail(null);

        adapter.envoyer(sansObjet);

        assertTrue(message.getSubject().startsWith("Fiscal-Track -"), message.getSubject());
    }

    private String texteDe(Object contenu) throws Exception {
        if (contenu instanceof String texte) {
            return texte;
        }
        Multipart multipart = (Multipart) contenu;
        return texteDe(multipart.getBodyPart(0).getContent());
    }

    private String htmlDe(Object contenu) throws Exception {
        if (contenu instanceof String texte) {
            return texte.contains("<div") ? texte : "";
        }
        Multipart multipart = (Multipart) contenu;
        for (int i = multipart.getCount() - 1; i >= 0; i--) {
            String html = htmlDe(multipart.getBodyPart(i).getContent());
            if (!html.isEmpty()) {
                return html;
            }
        }
        return "";
    }
}

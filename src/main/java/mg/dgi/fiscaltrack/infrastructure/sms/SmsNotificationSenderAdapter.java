package mg.dgi.fiscaltrack.infrastructure.sms;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.NotificationSenderPort;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.domain.model.Notification;
import mg.dgi.fiscaltrack.domain.util.Masquage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Envoi reel des notifications par SMS via l'API Textbee
 * (fournisseur utilise par l'application de test).
 *
 * Configuration (variables d'environnement) :
 *   NOTIFICATIONS_SMS_ENABLED : true pour activer l'envoi reel
 *   SMS_API_KEY               : cle d'API Textbee (jamais journalisee)
 *   SMS_API_URL               : point d'envoi (defaut : API Textbee)
 *
 * Si l'envoi est desactive ou si la cle manque, la notification est
 * journalisee (mode simulation) et consideree comme traitee.
 */
@Component
public class SmsNotificationSenderAdapter implements NotificationSenderPort {

    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationSenderAdapter.class);

    private final ContribuableRepositoryPort contribuableRepositoryPort;
    private final RestTemplate restTemplate;
    private final boolean envoiReelActive;
    private final String apiKey;
    private final String apiUrl;

    @Autowired
    public SmsNotificationSenderAdapter(ContribuableRepositoryPort contribuableRepositoryPort,
                                        @Value("${NOTIFICATIONS_SMS_ENABLED:false}") boolean envoiReelActive,
                                        @Value("${SMS_API_KEY:}") String apiKey,
                                        @Value("${SMS_API_URL:https://api.textbee.dev/api/v1/gateway/send-sms}") String apiUrl) {
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.envoiReelActive = envoiReelActive;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.restTemplate = new RestTemplate();
    }

    /** Constructeur pour les tests (injection du client HTTP). */
    SmsNotificationSenderAdapter(ContribuableRepositoryPort contribuableRepositoryPort,
                                 RestTemplate restTemplate,
                                 boolean envoiReelActive,
                                 String apiKey,
                                 String apiUrl) {
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.restTemplate = restTemplate;
        this.envoiReelActive = envoiReelActive;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    @Override
    public void envoyer(Notification notification) {
        if (!envoiReelActive || apiKey == null || apiKey.isBlank()) {
            logger.info("[NOTIFICATION][SMS][SIMULATION] Notification #{} NIF {} | SMS: {}",
                    notification.getIdNotif(),
                    Masquage.masquerNif(notification.getNif()),
                    apercuSms(notification));
            return;
        }

        String numero = contribuableRepositoryPort.findByNif(notification.getNif())
                .map(Contribuable::getTelephoneContribuable)
                .filter(t -> t != null && !t.isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        "Numéro de téléphone non renseigné pour ce contribuable"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("recipients", List.of(numero));
        body.put("message", notification.getMessageSms());

        ResponseEntity<String> reponse = restTemplate.postForEntity(
                apiUrl, new HttpEntity<>(body, headers), String.class);

        if (!reponse.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("L'opérateur SMS a répondu " + reponse.getStatusCode().value());
        }
        logger.info("[NOTIFICATION][SMS][REEL] Notification #{} envoyée",
                notification.getIdNotif());
    }

    private String apercuSms(Notification notification) {
        String sms = notification.getMessageSms();
        if (sms == null) {
            return "";
        }
        return sms.length() > 60 ? sms.substring(0, 60) + "…" : sms;
    }

    @Override
    public boolean supporterCanal(String canalEnvoi) {
        return "SMS".equals(canalEnvoi);
    }
}

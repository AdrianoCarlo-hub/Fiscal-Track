package mg.dgi.fiscaltrack.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mg.dgi.fiscaltrack.application.port.out.NotificationRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.CanalEnvoi;
import mg.dgi.fiscaltrack.domain.enums.StatutEnvoi;
import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrat de la Phase 0 : cloisonnement par NIF des nouvelles routes,
 * preferences de contact, lecture des notifications du portail, erreurs de
 * validation par champ et apercu des modeles de notification.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RefonteApiIntegrationTest {

    private static final String MDP = "password123";
    private static final String NIF = "0000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepositoryPort notificationRepositoryPort;

    private String tokenContribuable;
    private String tokenAgent;
    private String tokenAdmin;

    @BeforeEach
    void connexions() throws Exception {
        tokenContribuable = login(NIF);
        tokenAgent = login("rakoto.jean@impots.mg");
        tokenAdmin = login("admin@impots.mg");
    }

    private String login(String identifiant) throws Exception {
        String corps = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"" + identifiant
                                + "\",\"motDePasse\":\"" + MDP + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(corps);
        return json.get("token").asText();
    }

    // ===================== DECLARATIONS =====================

    @Test
    @DisplayName("Declarations : le contribuable lit les siennes, pas celles d'un autre")
    void declarationsCloisonnees() throws Exception {
        mockMvc.perform(get("/api/declarations/contribuable/" + NIF)
                        .header("Authorization", "Bearer " + tokenContribuable))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/declarations/contribuable/9999999999")
                        .header("Authorization", "Bearer " + tokenContribuable))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Declarations : un agent lit n'importe quel dossier")
    void declarationsAgent() throws Exception {
        mockMvc.perform(get("/api/declarations/contribuable/9999999999")
                        .header("Authorization", "Bearer " + tokenAgent))
                .andExpect(status().isOk());
    }

    // ===================== NOTIFICATIONS =====================

    @Test
    @DisplayName("Boite portail : lecture cloisonnee et marquage lu")
    void notificationsPortail() throws Exception {
        Notification notification = Notification.builder()
                .nif(NIF)
                .typeRelance(TypeRelance.MESSAGE_LIBRE)
                .objetEmail("Message des Impôts")
                .messageContenu("Bonjour, votre dossier est a jour.")
                .canalEnvoi(CanalEnvoi.PORTAIL)
                .statutEnvoi(StatutEnvoi.ENVOYE)
                .dateEnvoiPrevue(OffsetDateTime.now())
                .dateEnvoiEffective(OffsetDateTime.now())
                .nbTentatives(1)
                .lu(Boolean.FALSE)
                .createdAt(OffsetDateTime.now())
                .build();
        Notification enregistree = notificationRepositoryPort.save(notification);

        mockMvc.perform(get("/api/notifications/contribuable/" + NIF + "/portail")
                        .header("Authorization", "Bearer " + tokenContribuable))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/contribuable/" + NIF + "/non-lues")
                        .header("Authorization", "Bearer " + tokenContribuable))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreNonLues").isNumber());

        mockMvc.perform(get("/api/notifications/contribuable/9999999999/portail")
                        .header("Authorization", "Bearer " + tokenContribuable))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/notifications/" + enregistree.getIdNotif() + "/lu")
                        .header("Authorization", "Bearer " + tokenContribuable))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lu").value(true));
    }

    // ===================== PREFERENCES DE CONTACT =====================

    @Test
    @DisplayName("Preferences de contact : mises a jour par le contribuable proprietaire")
    void preferencesContact() throws Exception {
        mockMvc.perform(put("/api/contribuables/" + NIF + "/preferences-contact")
                        .header("Authorization", "Bearer " + tokenContribuable)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canalPrefere\":\"SMS\",\"accepteEmail\":true,\"accepteSms\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canalPrefere").value("SMS"))
                .andExpect(jsonPath("$.accepteSms").value(false));

        mockMvc.perform(put("/api/contribuables/9999999999/preferences-contact")
                        .header("Authorization", "Bearer " + tokenContribuable)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accepteSms\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Preferences de contact : canal inconnu refuse (400)")
    void preferencesCanalInvalide() throws Exception {
        mockMvc.perform(put("/api/contribuables/" + NIF + "/preferences-contact")
                        .header("Authorization", "Bearer " + tokenContribuable)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canalPrefere\":\"PIGEON\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.canalPrefere").isNotEmpty());
    }

    // ===================== RECHERCHE CONTRIBUABLES =====================

    @Test
    @DisplayName("Recherche : reservee aux agents, refusee au contribuable")
    void rechercheReserveeAuxAgents() throws Exception {
        mockMvc.perform(get("/api/contribuables/search")
                        .param("q", "SARL")
                        .header("Authorization", "Bearer " + tokenContribuable))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/contribuables/search")
                        .param("q", "SARL")
                        .header("Authorization", "Bearer " + tokenAgent))
                .andExpect(status().isOk());
    }

    // ===================== ERREURS DE VALIDATION =====================

    @Test
    @DisplayName("Validation : la reponse 400 expose les erreurs par champ")
    void erreursParChamp() throws Exception {
        mockMvc.perform(post("/api/contribuables")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nif\":\"123\",\"raisonSociale\":\"\","
                                + "\"dateImmatriculation\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.champs.nif").isNotEmpty())
                .andExpect(jsonPath("$.champs.raisonSociale").isNotEmpty())
                .andExpect(jsonPath("$.champs.dateImmatriculation").isNotEmpty());
    }

    // ===================== MODELES DE NOTIFICATION =====================

    @Test
    @DisplayName("Modeles : 10 types x 3 canaux, SMS de 160 caracteres maximum")
    void modelesNotifications() throws Exception {
        mockMvc.perform(get("/api/admin/modeles-notifications")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(30))
                .andExpect(jsonPath("$[0].typeRelance").isNotEmpty())
                .andExpect(jsonPath("$[0].objet").isNotEmpty());
    }

    @Test
    @DisplayName("Modeles : accessibles uniquement a l'administrateur")
    void modelesReservesAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/modeles-notifications")
                        .header("Authorization", "Bearer " + tokenAgent))
                .andExpect(status().isForbidden());
    }
}

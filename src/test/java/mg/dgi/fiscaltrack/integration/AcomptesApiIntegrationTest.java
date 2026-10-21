package mg.dgi.fiscaltrack.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controle d'acces de l'endpoint des acomptes obligatoires : un contribuable
 * ne peut lire que ses propres acomptes, un autre NIF renvoie 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AcomptesApiIntegrationTest {

    private static final String MDP = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void login() throws Exception {
        String corps = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"NIF0000001\",\"motDePasse\":\"" + MDP + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(corps);
        token = json.get("token").asText();
    }

    @Test
    @DisplayName("Le contribuable consulte ses propres acomptes (200)")
    void contribuableConsulteSesAcomptes() throws Exception {
        mockMvc.perform(get("/api/contribuables/NIF0000001/acomptes")
                        .param("exercice", "2026")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Le contribuable ne peut pas lire les acomptes d'un autre NIF (403)")
    void contribuableNeConsultePasLesAcomptesDUnAutre() throws Exception {
        mockMvc.perform(get("/api/contribuables/NIF9999999/acomptes")
                        .param("exercice", "2026")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}

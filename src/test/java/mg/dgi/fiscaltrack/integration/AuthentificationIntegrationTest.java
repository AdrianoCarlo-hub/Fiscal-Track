package mg.dgi.fiscaltrack.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BLOC 5.1 : test de login reel (contexte Spring complet + MockMvc + base reelle).
 * Le mot de passe des comptes de test est "password123" (hash BCrypt de data.sql).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthentificationIntegrationTest {

    private static final String MDP = "password123";
    private static final String HASH_PASSWORD123 =
            "$2a$10$xppsnB9gYQu.hJaRTX5IyOc5gxCAJejG5OSQ0Umyktsuzxqy43Fi6";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String loginBody(String identifiant, String motDePasse) {
        return "{\"identifiant\":\"" + identifiant + "\",\"motDePasse\":\"" + motDePasse + "\"}";
    }

    @Test
    @DisplayName("Login agent : email + mot de passe valide renvoie un token et le role")
    void loginAgent() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("rakoto.jean@impots.mg", MDP)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("AGENT_GESTION"))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    @DisplayName("Login admin : renvoie le role ADMIN")
    void loginAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@impots.mg", MDP)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Login contribuable : NIF + mot de passe renvoie le role CONTRIBUABLE")
    void loginContribuable() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("NIF0000001", MDP)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CONTRIBUABLE"));
    }

    @Test
    @DisplayName("Login agent : mauvais mot de passe refuse (401)")
    void loginAgentMauvaisMotDePasse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("rakoto.jean@impots.mg", "mauvais")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login contribuable RADIE : connexion refusee (401)")
    void loginContribuableRadie() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO contribuables (nif, raison_sociale, mot_de_passe_hash_contribuable, "
                        + "statut_activite, date_immatriculation, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, CURRENT_DATE, NOW(), NOW())",
                "NIFRADIE01", "SOCIETE RADIEE", HASH_PASSWORD123, "RADIE");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("NIFRADIE01", MDP)))
                .andExpect(status().isUnauthorized());
    }
}

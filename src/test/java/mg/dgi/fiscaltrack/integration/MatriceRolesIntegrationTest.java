package mg.dgi.fiscaltrack.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BLOC 5.1 : matrice roles / routes via MockMvc et securite Spring reelle
 * ({@code @EnableMethodSecurity} + regles d'URL de {@code SecurityConfig}).
 *
 * Les roles applicatifs sont AGENT_GESTION, AGENT_RECETTE, RESPONSABLE, ADMIN
 * (plus CONTRIBUABLE cote contribuable).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MatriceRolesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // --- /api/admin/** : reserve a ADMIN ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("/api/admin/** : ADMIN autorise")
    void adminAutorise() throws Exception {
        mockMvc.perform(get("/api/admin/utilisateurs")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "AGENT_GESTION")
    @DisplayName("/api/admin/** : AGENT_GESTION interdit (403)")
    void adminRefuseAgentGestion() throws Exception {
        mockMvc.perform(get("/api/admin/utilisateurs")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESPONSABLE")
    @DisplayName("/api/admin/** : RESPONSABLE interdit (403)")
    void adminRefuseResponsable() throws Exception {
        mockMvc.perform(get("/api/admin/utilisateurs")).andExpect(status().isForbidden());
    }

    // --- /api/dashboard/** : RESPONSABLE, AGENT_RECETTE, ADMIN ---

    @Test
    @WithMockUser(roles = "RESPONSABLE")
    @DisplayName("/api/dashboard/indicateurs : RESPONSABLE autorise")
    void indicateursResponsable() throws Exception {
        mockMvc.perform(get("/api/dashboard/indicateurs")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "AGENT_RECETTE")
    @DisplayName("/api/dashboard/indicateurs : AGENT_RECETTE interdit (403)")
    void indicateursRefuseAgentRecette() throws Exception {
        mockMvc.perform(get("/api/dashboard/indicateurs")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AGENT_RECETTE")
    @DisplayName("/api/dashboard/top10-restes : AGENT_RECETTE autorise (regression corrigee)")
    void top10AgentRecette() throws Exception {
        mockMvc.perform(get("/api/dashboard/top10-restes")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "AGENT_GESTION")
    @DisplayName("/api/dashboard/top10-restes : AGENT_GESTION interdit (403)")
    void top10RefuseAgentGestion() throws Exception {
        mockMvc.perform(get("/api/dashboard/top10-restes")).andExpect(status().isForbidden());
    }

    // --- /api/paiements : AGENT_RECETTE, RESPONSABLE, ADMIN ---

    @Test
    @WithMockUser(roles = "AGENT_RECETTE")
    @DisplayName("/api/paiements : AGENT_RECETTE autorise")
    void paiementsAgentRecette() throws Exception {
        mockMvc.perform(get("/api/paiements")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "AGENT_GESTION")
    @DisplayName("/api/paiements : AGENT_GESTION interdit (403)")
    void paiementsRefuseAgentGestion() throws Exception {
        mockMvc.perform(get("/api/paiements")).andExpect(status().isForbidden());
    }

    // --- /api/calculs : les quatre roles metier ---

    @Test
    @WithMockUser(roles = "CONTRIBUABLE")
    @DisplayName("/api/declarations : CONTRIBUABLE interdit sur la liste (403)")
    void declarationsRefuseContribuable() throws Exception {
        mockMvc.perform(get("/api/declarations")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AGENT_GESTION")
    @DisplayName("/api/declarations : AGENT_GESTION autorise")
    void declarationsAgentGestion() throws Exception {
        mockMvc.perform(get("/api/declarations")).andExpect(status().isOk());
    }

    // --- Non authentifie : refuse ---

    @Test
    @DisplayName("Requete non authentifiee : refusee (401 ou 403)")
    void nonAuthentifie() throws Exception {
        int status = mockMvc.perform(get("/api/contribuables"))
                .andReturn().getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertTrue(
                status == 401 || status == 403,
                "Statut attendu 401/403, obtenu " + status);
    }
}

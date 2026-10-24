package mg.dgi.fiscaltrack.interfaces.controller;

import mg.dgi.fiscaltrack.domain.exception.AccesRefuseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Controle de propriete : un contribuable ne peut agir que sur son propre NIF,
 * un agent conserve l'acces a toutes les ressources.
 */
class UtilisateurConnecteTest {

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private void connecter(String identifiant, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        identifiant, null,
                        List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    @DisplayName("Contribuable : le NIF connecte est lu depuis le contexte")
    void nifContribuable() {
        connecter("NIF0000001", "ROLE_CONTRIBUABLE");
        assertEquals("NIF0000001", UtilisateurConnecte.nifSiContribuable());
    }

    @Test
    @DisplayName("Agent : aucun NIF, aucun controle de propriete")
    void agentSansControle() {
        connecter("AG0002", "ROLE_AGENT_GESTION");
        assertNull(UtilisateurConnecte.nifSiContribuable());
        UtilisateurConnecte.verifierPropriete("NIF0000009", "cette obligation fiscale");
    }

    @Test
    @DisplayName("Ressource d'un autre contribuable -> AccesRefuseException (HTTP 403)")
    void ressourceEtrangere() {
        connecter("NIF0000001", "ROLE_CONTRIBUABLE");
        AccesRefuseException e = assertThrows(AccesRefuseException.class,
                () -> UtilisateurConnecte.verifierPropriete("NIF0000009", "cette obligation fiscale"));
        assertTrue(e.getMessage().contains("Acces refuse"));
    }

    @Test
    @DisplayName("Ressource propre -> pas d'exception")
    void ressourcePropre() {
        connecter("NIF0000001", "ROLE_CONTRIBUABLE");
        UtilisateurConnecte.verifierPropriete("NIF0000001", "cette obligation fiscale");
    }

    @Test
    @DisplayName("Sans contexte d'authentification -> pas d'exception")
    void sansContexte() {
        assertNull(UtilisateurConnecte.nifSiContribuable());
        UtilisateurConnecte.verifierPropriete("NIF0000001", "cette obligation fiscale");
    }
}

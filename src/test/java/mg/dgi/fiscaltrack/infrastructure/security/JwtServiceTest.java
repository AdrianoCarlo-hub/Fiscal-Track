package mg.dgi.fiscaltrack.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BLOC 3.3 : le service JWT refuse une cle trop courte (moins de 32 caracteres).
 */
class JwtServiceTest {

    private static final String SECRET_VALIDE = "UneCleTresLongueDe32CaracteresMinimum!!";

    @Test
    @DisplayName("Une cle de moins de 32 caracteres fait echouer la construction")
    void cleTropCourteRefusee() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService("tropCourt", 86400000L));
    }

    @Test
    @DisplayName("Une cle de 32 caracteres est acceptee et un token peut etre valide")
    void cleValide() {
        JwtService service = new JwtService(SECRET_VALIDE, 86400000L);

        String token = service.genererToken("AG0001", "ADMIN", "AGENT");

        assertEquals("AG0001", service.extraireIdentifiant(token));
        assertEquals("ADMIN", service.extraireRole(token));
        assertTrue(service.estValide(token, "AG0001"));
    }
}

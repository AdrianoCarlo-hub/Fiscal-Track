package mg.dgi.fiscaltrack.application.usecase.contribuable;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.auth.AuthentifierContribuableUseCase;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.StatutActivite;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * BLOC 3.1 : le mot de passe d'un contribuable est encode a la creation et a
 * la modification, n'est jamais stocke en clair, et un contribuable cree via
 * le use case peut s'authentifier.
 */
class ContribuablePasswordUseCaseTest {

    private ContribuableRepositoryPort repositoryPort;
    private PasswordEncoder passwordEncoder;
    private AjouterContribuableUseCase ajouterUseCase;
    private ModifierContribuableUseCase modifierUseCase;
    private AuthentifierContribuableUseCase authentifierUseCase;

    @BeforeEach
    void setUp() {
        repositoryPort = Mockito.mock(ContribuableRepositoryPort.class);
        passwordEncoder = new BCryptPasswordEncoder();
        EnregistrerActionUseCase audit = Mockito.mock(EnregistrerActionUseCase.class);
        ajouterUseCase = new AjouterContribuableUseCase(repositoryPort, passwordEncoder, audit);
        modifierUseCase = new ModifierContribuableUseCase(repositoryPort, passwordEncoder, audit);
        authentifierUseCase = new AuthentifierContribuableUseCase(repositoryPort, passwordEncoder);
        when(repositoryPort.save(any(Contribuable.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repositoryPort.existsByNif(any())).thenReturn(false);
    }

    private Contribuable contribuableAvecMotDePasse(String motDePasse) {
        return Contribuable.builder()
                .nif("NIF0000009")
                .raisonSociale("TEST SARL")
                .motDePasseHashContribuable(motDePasse)
                .statutActivite(StatutActivite.ACTIF)
                .build();
    }

    @Test
    @DisplayName("Creation : le mot de passe est encode et non stocke en clair")
    void creationEncodeLeMotDePasse() {
        Contribuable saved = ajouterUseCase.execute(contribuableAvecMotDePasse("password123"));

        assertNotEquals("password123", saved.getMotDePasseHashContribuable());
        assertTrue(saved.getMotDePasseHashContribuable().startsWith("$2"));
        assertTrue(passwordEncoder.matches("password123", saved.getMotDePasseHashContribuable()));
    }

    @Test
    @DisplayName("Un contribuable cree peut s'authentifier (le hash correspond)")
    void contribuableCreePeutSeConnecter() {
        Contribuable saved = ajouterUseCase.execute(contribuableAvecMotDePasse("password123"));
        when(repositoryPort.findByNif("NIF0000009")).thenReturn(Optional.of(saved));

        Contribuable authentifie = authentifierUseCase.execute("NIF0000009", "password123");

        assertEquals("NIF0000009", authentifie.getNif());
    }

    @Test
    @DisplayName("Creation sans mot de passe : rejet")
    void creationSansMotDePasse() {
        assertThrows(IllegalArgumentException.class,
                () -> ajouterUseCase.execute(contribuableAvecMotDePasse(null)));
        assertThrows(IllegalArgumentException.class,
                () -> ajouterUseCase.execute(contribuableAvecMotDePasse("  ")));
    }

    @Test
    @DisplayName("Modification avec un nouveau mot de passe : l'ancien hash est remplace")
    void modificationEncodeNouveauMotDePasse() {
        Contribuable existant = contribuableAvecMotDePasse("password123");
        existant.setMotDePasseHashContribuable(passwordEncoder.encode("password123"));
        String ancienHash = existant.getMotDePasseHashContribuable();
        when(repositoryPort.findByNif("NIF0000009")).thenReturn(Optional.of(existant));

        Contribuable donnees = Contribuable.builder()
                .motDePasseHashContribuable("nouveauSecret")
                .build();
        Contribuable saved = modifierUseCase.execute("NIF0000009", donnees);

        assertNotEquals(ancienHash, saved.getMotDePasseHashContribuable());
        assertTrue(passwordEncoder.matches("nouveauSecret", saved.getMotDePasseHashContribuable()));
        assertFalse(passwordEncoder.matches("password123", saved.getMotDePasseHashContribuable()));
    }

    @Test
    @DisplayName("Modification sans mot de passe : l'ancien hash est conserve")
    void modificationSansMotDePasseConserveHash() {
        Contribuable existant = contribuableAvecMotDePasse("password123");
        existant.setMotDePasseHashContribuable(passwordEncoder.encode("password123"));
        String ancienHash = existant.getMotDePasseHashContribuable();
        when(repositoryPort.findByNif("NIF0000009")).thenReturn(Optional.of(existant));

        Contribuable donnees = Contribuable.builder().raisonSociale("NOUVELLE RAISON").build();
        Contribuable saved = modifierUseCase.execute("NIF0000009", donnees);

        assertEquals(ancienHash, saved.getMotDePasseHashContribuable());
        assertEquals("NOUVELLE RAISON", saved.getRaisonSociale());
    }
}

package mg.dgi.fiscaltrack.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test d'integration (coherence seeds / code).
 *
 * Les tests unitaires mockent {@code ParametreFiscalService} : ils ne peuvent pas
 * detecter qu'un code utilise par un use case est absent des donnees initiales.
 * Ce test lit le code source de production, extrait TOUS les codes demandes via
 * {@code getValeur("...")} / {@code getParametre("...")}, puis verifie que chacun
 * est bien insere par {@code data.sql}. Il echoue si un code manque.
 */
class ParametreFiscalSeedsIntegrationTest {

    private static final Pattern CODE_UTILISE =
            Pattern.compile("get(?:Valeur|Parametre)\\(\"([A-Z0-9_]+)\"\\)");

    private static final Pattern CODE_SEED =
            Pattern.compile("^\\s*\\(\'([A-Z0-9_]+)\'", Pattern.MULTILINE);

    @Test
    @DisplayName("Chaque code de parametre_fiscal utilise par les use cases est present dans data.sql")
    void chaqueCodeUtiliseEstSeed() throws IOException {
        Set<String> codesUtilises = extraireCodesUtilises(Path.of("src", "main", "java"));
        Set<String> codesSeed = extraireCodesSeedes();

        Set<String> manquants = new TreeSet<>(codesUtilises);
        manquants.removeAll(codesSeed);

        assertTrue(codesUtilises.size() >= 50,
                "Le scan des sources doit trouver de nombreux codes (trouve : "
                        + codesUtilises.size() + ")");
        assertTrue(manquants.isEmpty(),
                "Codes utilises par les use cases mais absents de data.sql : " + manquants);
    }

    private Set<String> extraireCodesUtilises(Path racineSources) throws IOException {
        Set<String> codes = new LinkedHashSet<>();
        try (Stream<Path> fichiers = Files.walk(racineSources)) {
            fichiers.filter(p -> p.toString().endsWith(".java"))
                    .forEach(fichier -> {
                        try {
                            String contenu = Files.readString(fichier, StandardCharsets.UTF_8);
                            Matcher m = CODE_UTILISE.matcher(contenu);
                            while (m.find()) {
                                codes.add(m.group(1));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        return codes;
    }

    private Set<String> extraireCodesSeedes() throws IOException {
        String contenu;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("data.sql")) {
            if (in == null) {
                throw new IllegalStateException("data.sql introuvable sur le classpath");
            }
            contenu = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Set<String> codes = new TreeSet<>();
        Matcher m = CODE_SEED.matcher(contenu);
        while (m.find()) {
            codes.add(m.group(1));
        }
        return codes;
    }
}

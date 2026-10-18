package mg.dgi.fiscaltrack.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BLOC 5.4 : verification du demarrage sur base vierge.
 *
 * Les tests d'integration tournent sur la base {@code fiscal_track} deja peuplee :
 * ils ne prouvent pas que {@code schema.sql} + {@code data.sql} s'executent sans
 * erreur sur une base PostgreSQL totalement vide (cas du premier demarrage).
 *
 * Ce test cree un schema PostgreSQL dedie et vide ({@code fiscal_track_base_vierge}),
 * y execute les scripts de production tels quels (via {@code search_path}), puis
 * verifie la creation des tables/colonnes et l'insertion des donnees initiales.
 * Il ne touche pas au schema {@code public} et nettoie le schema temporaire a la fin
 * (y compris la restauration de {@code search_path} pour ne pas contaminer le pool).
 */
@SpringBootTest
class BaseViergeInitializationTest {

    private static final String SCHEMA = "fiscal_track_base_vierge";

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("schema.sql + data.sql s'executent sur un schema vide et peuplent la base")
    void initialisationSurBaseVierge() throws Exception {
        try (Connection cn = dataSource.getConnection()) {
            cn.setAutoCommit(true);
            try (Statement st = cn.createStatement()) {
                st.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
                st.execute("CREATE SCHEMA " + SCHEMA);
                st.execute("SET search_path TO " + SCHEMA);
            }

            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.setContinueOnError(false);
            populator.addScript(new ClassPathResource("schema.sql"));
            populator.addScript(new ClassPathResource("data.sql"));
            populator.populate(cn);

            int nbTables = compter(cn,
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='" + SCHEMA + "'");
            assertTrue(nbTables >= 13,
                    "13 tables attendues au minimum, trouvees : " + nbTables);

            int nbColonnesCompte = compter(cn,
                    "SELECT COUNT(*) FROM information_schema.columns"
                            + " WHERE table_schema='" + SCHEMA + "'"
                            + " AND table_name='comptes_courants_fiscaux'"
                            + " AND column_name IN ('montant_amende','montant_total_du','reste_a_recouvrer')");
            assertEquals(3, nbColonnesCompte,
                    "montant_amende / montant_total_du / reste_a_recouvrer doivent exister");

            int nbColonnesGenerees = compter(cn,
                    "SELECT COUNT(*) FROM information_schema.columns"
                            + " WHERE table_schema='" + SCHEMA + "'"
                            + " AND table_name='comptes_courants_fiscaux'"
                            + " AND is_generated='ALWAYS'"
                            + " AND column_name IN ('montant_total_du','reste_a_recouvrer')");
            assertEquals(2, nbColonnesGenerees,
                    "montant_total_du et reste_a_recouvrer doivent etre des colonnes generees");

            int nbContribuable = compter(cn,
                    "SELECT COUNT(*) FROM " + SCHEMA + ".contribuables");
            assertTrue(nbContribuable >= 1, "au moins un contribuable de test doit etre insere");

            int nbParametres = compter(cn,
                    "SELECT COUNT(*) FROM " + SCHEMA + ".parametres_fiscaux");
            assertTrue(nbParametres >= 50, "les parametres fiscaux doivent etre inseres : " + nbParametres);

            try (Statement st = cn.createStatement()) {
                st.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
                st.execute("SET search_path TO public");
            }
        }
    }

    private int compter(Connection cn, String sql) throws Exception {
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}

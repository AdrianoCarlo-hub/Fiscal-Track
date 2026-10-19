package mg.dgi.fiscaltrack.integration;

import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.obligation.GenererObligationsUseCase;
import mg.dgi.fiscaltrack.domain.enums.StatutValidation;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tracabilite des declarations (ajouts de colonnes idempotents) :
 *   declarations.donnees_saisies -> copie des champs saisis par le contribuable
 *   declarations.motif_rejet     -> motif du rejet affiche au contribuable
 *
 * Test {@code @Transactional} : rien n'est conserve dans la base.
 */
@SpringBootTest
@Transactional
class DeclarationTracabiliteIntegrationTest {

    private static final String NIF = "NIFTEST001";

    @Autowired
    private GenererObligationsUseCase genererObligationsUseCase;

    @Autowired
    private DeclarationRepositoryPort declarationRepositoryPort;

    @Test
    @DisplayName("Donnees saisies et motif de rejet sont persistes et relus")
    void tracabiliteSaisieEtRejet() {
        ObligationFiscale obligation = genererObligationsUseCase.execute(NIF, "IR", "2020");

        String saisi = "{\"caHT\":10000000,\"chargesDeductibles\":8000000}";
        Declaration sauvee = declarationRepositoryPort.save(Declaration.builder()
                .idObligationFiscale(obligation.getIdObligationFiscale())
                .chiffreAffairesDeclare(new BigDecimal("10000000"))
                .impotPrincipalCalcule(new BigDecimal("4000000"))
                .dateSoumission(OffsetDateTime.now())
                .statutValidation(StatutValidation.EN_ATTENTE_VALIDATION)
                .donneesSaisies(saisi)
                .build());

        Optional<Declaration> relue = declarationRepositoryPort.findById(sauvee.getIdDeclaration());
        assertEquals(saisi, relue.orElseThrow().getDonneesSaisies());
        assertNull(relue.orElseThrow().getMotifRejet());

        relue.orElseThrow().setMotifRejet("Montant incoherent avec le calcul du serveur");
        declarationRepositoryPort.save(relue.orElseThrow());

        Declaration avecMotif = declarationRepositoryPort
                .findById(sauvee.getIdDeclaration()).orElseThrow();
        assertEquals(StatutValidation.EN_ATTENTE_VALIDATION, avecMotif.getStatutValidation());
        assertEquals("Montant incoherent avec le calcul du serveur", avecMotif.getMotifRejet());
        assertEquals(saisi, avecMotif.getDonneesSaisies());
    }
}

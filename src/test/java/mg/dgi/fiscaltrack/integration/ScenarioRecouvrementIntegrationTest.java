package mg.dgi.fiscaltrack.integration;

import mg.dgi.fiscaltrack.application.port.out.AmendeDefautDepotRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.declaration.DeposerDeclarationUseCase;
import mg.dgi.fiscaltrack.application.usecase.obligation.GenererObligationsUseCase;
import mg.dgi.fiscaltrack.application.usecase.obligation.IdentifierRetardsUseCase;
import mg.dgi.fiscaltrack.application.usecase.paiement.EnregistrerPaiementUseCase;
import mg.dgi.fiscaltrack.application.usecase.penalite.CalculerAmendeDefautDepotUseCase;
import mg.dgi.fiscaltrack.application.usecase.penalite.CalculerPenaliteUseCase;
import mg.dgi.fiscaltrack.domain.enums.ModePaiement;
import mg.dgi.fiscaltrack.domain.enums.StatutDeclaration;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.model.AmendeDefautDepot;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BLOC 5.1 : scenario complet d'integration sur le contexte Spring reel
 * (base PostgreSQL, parametres fiscaux reels, use cases reels).
 *
 * Parcours : generation d'obligation -> passage en RETARD -> declaration
 * -> amende de defaut de depot (idempotente) -> interets de retard
 * -> paiement partiel -> paiement total -> statut SOLDE.
 *
 * Le test est {@code @Transactional} : toutes les donnees inserees sont
 * annulees a la fin (aucune pollution de la base de developpement).
 */
@SpringBootTest
@Transactional
class ScenarioRecouvrementIntegrationTest {

    private static final String NIF = "NIFTEST001";
    private static final BigDecimal CHIFFRE_AFFAIRES = new BigDecimal("500000000");

    @Autowired
    private GenererObligationsUseCase genererObligationsUseCase;
    @Autowired
    private IdentifierRetardsUseCase identifierRetardsUseCase;
    @Autowired
    private CalculerAmendeDefautDepotUseCase calculerAmendeUseCase;
    @Autowired
    private DeposerDeclarationUseCase deposerDeclarationUseCase;
    @Autowired
    private CalculerPenaliteUseCase calculerPenaliteUseCase;
    @Autowired
    private EnregistrerPaiementUseCase enregistrerPaiementUseCase;
    @Autowired
    private CompteCourantFiscalRepositoryPort compteRepositoryPort;
    @Autowired
    private ObligationFiscaleRepositoryPort obligationRepositoryPort;
    @Autowired
    private AmendeDefautDepotRepositoryPort amendeRepositoryPort;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CompteCourantFiscal recharger(Long idCompte) {
        return compteRepositoryPort.findById(idCompte).orElseThrow();
    }

    @Test
    @DisplayName("Scenario complet : obligation -> retard -> declaration -> amende -> interets -> paiements -> SOLDE")
    void scenarioComplet() {
        // 1. Generation d'une obligation IR pour un exercice ancien (echeance passee)
        ObligationFiscale obligation = genererObligationsUseCase.execute(NIF, "IR", "2019");
        assertTrue(obligation.getDateLimiteReelle().isBefore(LocalDate.now()),
                "L'echeance doit etre passee pour le scenario");
        assertEquals(StatutDeclaration.ATTENTE, obligation.getStatutDeclaration());

        // 2. Passage en RETARD
        identifierRetardsUseCase.execute();
        ObligationFiscale apresRetard = obligationRepositoryPort
                .findById(obligation.getIdObligationFiscale()).orElseThrow();
        assertEquals(StatutDeclaration.RETARD, apresRetard.getStatutDeclaration());

        // 3. Amende de defaut de depot (idempotente)
        AmendeDefautDepot amende1 = calculerAmendeUseCase.execute(obligation.getIdObligationFiscale());
        AmendeDefautDepot amende2 = calculerAmendeUseCase.execute(obligation.getIdObligationFiscale());
        assertEquals(amende1.getIdAmende(), amende2.getIdAmende(), "L'amende ne doit pas etre dupliquee");
        Integer nbAmendes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM amendes_defaut_depot WHERE id_obligation_fiscale = ?",
                Integer.class, obligation.getIdObligationFiscale());
        assertEquals(1, nbAmendes, "Une seule amende par obligation");
        assertTrue(amende1.getMontant().compareTo(BigDecimal.ZERO) > 0);

        // 4. Declaration : le serveur recalcule l'impot (montant soumis ignore)
        Declaration declaration = deposerDeclarationUseCase.execute(
                obligation.getIdObligationFiscale(), CHIFFRE_AFFAIRES, BigDecimal.ZERO);
        assertTrue(declaration.getImpotPrincipalCalcule().compareTo(BigDecimal.ZERO) > 0,
                "L'impot recalcule par le serveur doit etre positif");

        CompteCourantFiscal compte = compteRepositoryPort
                .findByIdDeclaration(declaration.getIdDeclaration()).orElseThrow();
        assertEquals(StatutRecouvrement.NON_SOLDE, compte.getStatutRecouvrement());
        assertEquals(0, compte.getMontantAmende().compareTo(amende1.getMontant()),
                "L'amende doit etre reprise sur le compte");
        assertEquals(0, compte.getMontantPenalites().compareTo(BigDecimal.ZERO));

        // 5. Interets de retard (CDI 20.01.53)
        BigDecimal penalites = calculerPenaliteUseCase.execute(
                compte.getIdCompte(), obligation.getDateLimiteReelle());
        assertTrue(penalites.compareTo(BigDecimal.ZERO) > 0);

        CompteCourantFiscal comptePenalise = recharger(compte.getIdCompte());
        assertEquals(0, comptePenalise.getMontantPenalites().compareTo(penalites));
        BigDecimal total = comptePenalise.getMontantTotalDu();
        assertTrue(total.compareTo(BigDecimal.ZERO) > 0);

        // 6. Paiement partiel (moitie du total)
        BigDecimal partiel = total.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        enregistrerPaiementUseCase.execute(
                compte.getIdCompte(), partiel, ModePaiement.ESPECES, "REF-PARTIEL-1");
        CompteCourantFiscal apresPartiel = recharger(compte.getIdCompte());
        assertEquals(StatutRecouvrement.PARTIEL, apresPartiel.getStatutRecouvrement());

        // 7. Paiement du reste -> SOLDE
        BigDecimal reste = apresPartiel.getResteARecouvrer();
        assertTrue(reste.compareTo(BigDecimal.ZERO) > 0);
        enregistrerPaiementUseCase.execute(
                compte.getIdCompte(), reste, ModePaiement.ESPECES, "REF-TOTAL-1");
        CompteCourantFiscal apresTotal = recharger(compte.getIdCompte());
        assertEquals(StatutRecouvrement.SOLDE, apresTotal.getStatutRecouvrement());
    }

    @Test
    @DisplayName("Les 3 valeurs de penalite de retard (500 000 / 400 000 / 300 000 Ar) en integration")
    void troisValeursPenalite() {
        // Grande entreprise : CA >= 400M, 3 mois de retard -> 3% + 1%*2 = 5%
        assertEquals(0, penalitePour("NIFPEN0001", new BigDecimal("500000000"), 3)
                .compareTo(new BigDecimal("500000")));
        // Moyenne entreprise : 100M <= CA < 400M -> 2% + 1%*2 = 4%
        assertEquals(0, penalitePour("NIFPEN0002", new BigDecimal("200000000"), 3)
                .compareTo(new BigDecimal("400000")));
        // Petite entreprise : CA < 100M -> 1% + 1%*2 = 3%
        assertEquals(0, penalitePour("NIFPEN0003", new BigDecimal("50000000"), 3)
                .compareTo(new BigDecimal("300000")));
    }

    /**
     * Cree une obligation + declaration sur un NIF dedie, fixe le principal a
     * 10 000 000 Ar (valeur attendue deterministe) puis calcule la penalite
     * pour le nombre de mois de retard demande.
     */
    private BigDecimal penalitePour(String nif, BigDecimal chiffreAffaires, int moisRetard) {
        ObligationFiscale obligation = genererObligationsUseCase.execute(nif, "IR", "2019");

        Declaration declaration = deposerDeclarationUseCase.execute(
                obligation.getIdObligationFiscale(), chiffreAffaires, BigDecimal.ZERO);
        CompteCourantFiscal compte = compteRepositoryPort
                .findByIdDeclaration(declaration.getIdDeclaration()).orElseThrow();

        compte.setMontantPrincipal(new BigDecimal("10000000"));
        compte.setMontantPenalites(BigDecimal.ZERO);
        compte.setMontantPaye(BigDecimal.ZERO);
        compteRepositoryPort.save(compte);

        LocalDate dateLimite = LocalDate.now().minusMonths(moisRetard);
        return calculerPenaliteUseCase.execute(compte.getIdCompte(), dateLimite);
    }
}

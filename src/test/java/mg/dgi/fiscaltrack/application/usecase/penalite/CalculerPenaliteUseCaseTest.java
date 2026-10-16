package mg.dgi.fiscaltrack.application.usecase.penalite;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CalculerPenaliteUseCaseTest {

    private CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private DeclarationRepositoryPort declarationRepositoryPort;
    private ParametreFiscalService parametreFiscalService;
    private CalculerPenaliteUseCase useCase;

    @BeforeEach
    void setUp() {
        compteRepositoryPort = Mockito.mock(CompteCourantFiscalRepositoryPort.class);
        declarationRepositoryPort = Mockito.mock(DeclarationRepositoryPort.class);
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);

        when(compteRepositoryPort.save(any(CompteCourantFiscal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock des parametres fiscaux pour penalites
        when(parametreFiscalService.getValeur("PENALITE_SEUIL_GRANDE_ENTREPRISE"))
                .thenReturn(new BigDecimal("400000000"));
        when(parametreFiscalService.getValeur("PENALITE_SEUIL_MOYENNE_ENTREPRISE"))
                .thenReturn(new BigDecimal("100000000"));
        when(parametreFiscalService.getValeur("PENALITE_TAUX_INITIAL_PETITE"))
                .thenReturn(new BigDecimal("0.01"));
        when(parametreFiscalService.getValeur("PENALITE_TAUX_INITIAL_MOYENNE"))
                .thenReturn(new BigDecimal("0.02"));
        when(parametreFiscalService.getValeur("PENALITE_TAUX_INITIAL_GRANDE"))
                .thenReturn(new BigDecimal("0.03"));
        when(parametreFiscalService.getValeur("PENALITE_TAUX_INCREMENT_MENSUEL"))
                .thenReturn(new BigDecimal("0.01"));
        when(parametreFiscalService.getValeur("PENALITE_PLANCHER"))
                .thenReturn(new BigDecimal("10000"));

        useCase = new CalculerPenaliteUseCase(compteRepositoryPort, declarationRepositoryPort, parametreFiscalService);
    }

    private void preparerContexte(BigDecimal chiffreAffaires, BigDecimal principal) {
        CompteCourantFiscal compte = CompteCourantFiscal.builder()
                .idCompte(1L)
                .idDeclaration(10L)
                .nif("NIF0000001")
                .montantPrincipal(principal)
                .montantPenalites(BigDecimal.ZERO)
                .montantPaye(BigDecimal.ZERO)
                .statutRecouvrement(StatutRecouvrement.NON_SOLDE)
                .build();
        when(compteRepositoryPort.findById(1L)).thenReturn(Optional.of(compte));

        Declaration declaration = Declaration.builder()
                .idDeclaration(10L)
                .chiffreAffairesDeclare(chiffreAffaires)
                .build();
        when(declarationRepositoryPort.findById(10L)).thenReturn(Optional.of(declaration));
    }

    @Test
    @DisplayName("CA >= 400M : 3% au 1er mois puis +1% par mois suivant, sur 3 mois = 500 000")
    void grandeEntreprise() {
        preparerContexte(new BigDecimal("500000000"), new BigDecimal("10000000"));
        LocalDate dateLimite = LocalDate.now().minusMonths(3);
        BigDecimal penalites = useCase.execute(1L, dateLimite);

        // 10M * (0.03 + 0.01*2) = 10M * 0.05 = 500 000
        assertEquals(0, penalites.compareTo(new BigDecimal("500000")));
    }

    @Test
    @DisplayName("CA = 400M : seuil de borne grande, sur 3 mois = 500 000")
    void seuilGrande() {
        preparerContexte(new BigDecimal("400000000"), new BigDecimal("10000000"));
        LocalDate dateLimite = LocalDate.now().minusMonths(3);
        BigDecimal penalites = useCase.execute(1L, dateLimite);

        // 10M * (0.03 + 0.01*2) = 500 000
        assertEquals(0, penalites.compareTo(new BigDecimal("500000")));
    }

    @Test
    @DisplayName("CA = 100M : seuil de borne moyenne, sur 3 mois = 400 000")
    void seuilMoyenne() {
        preparerContexte(new BigDecimal("100000000"), new BigDecimal("10000000"));
        LocalDate dateLimite = LocalDate.now().minusMonths(3);
        BigDecimal penalites = useCase.execute(1L, dateLimite);

        // 10M * (0.02 + 0.01*2) = 10M * 0.04 = 400 000
        assertEquals(0, penalites.compareTo(new BigDecimal("400000")));
    }

    @Test
    @DisplayName("CA entre 100M et 400M : 2% au 1er mois puis +1% par mois suivant, sur 3 mois = 400 000")
    void moyenneEntreprise() {
        preparerContexte(new BigDecimal("200000000"), new BigDecimal("10000000"));
        LocalDate dateLimite = LocalDate.now().minusMonths(3);
        BigDecimal penalites = useCase.execute(1L, dateLimite);

        // 10M * (0.02 + 0.01*2) = 400 000
        assertEquals(0, penalites.compareTo(new BigDecimal("400000")));
    }

    @Test
    @DisplayName("CA < 100M : 1% par mois, sur 3 mois = 300 000")
    void petiteEntreprise() {
        preparerContexte(new BigDecimal("50000000"), new BigDecimal("10000000"));
        LocalDate dateLimite = LocalDate.now().minusMonths(3);
        BigDecimal penalites = useCase.execute(1L, dateLimite);

        // 10M * (0.01 + 0.01*2) = 10M * 0.03 = 300 000
        assertEquals(0, penalites.compareTo(new BigDecimal("300000")));
    }

    @Test
    @DisplayName("Le plancher de 10 000 Ar s'applique au total de la pénalité")
    void plancherApplique() {
        preparerContexte(new BigDecimal("50000000"), new BigDecimal("100000"));
        LocalDate dateLimite = LocalDate.now().minusDays(15);
        BigDecimal penalites = useCase.execute(1L, dateLimite);

        // 100 000 * 0.01 = 1 000, mais plancher a 10 000
        assertEquals(0, penalites.compareTo(new BigDecimal("10000")));
    }

    @Test
    @DisplayName("Pas de penalite si la date limite n'est pas depassee")
    void pasDePenalite() {
        preparerContexte(new BigDecimal("500000000"), new BigDecimal("10000000"));
        LocalDate dateFuture = LocalDate.now().plusMonths(1);
        BigDecimal penalites = useCase.execute(1L, dateFuture);

        assertEquals(0, penalites.compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Une fraction de mois entamee compte pour 1 mois complet")
    void fractionDeMois() {
        preparerContexte(new BigDecimal("500000000"), new BigDecimal("10000000"));
        LocalDate dateLimite = LocalDate.now().minusDays(15);
        BigDecimal penalites = useCase.execute(1L, dateLimite);

        // 10 000 000 x 0.03 x 1 mois = 300 000
        assertEquals(0, penalites.compareTo(new BigDecimal("300000")));
    }

    @Test
    @DisplayName("Les penalites sont arrondies a 2 decimales maximum")
    void arrondiDeuxDecimales() {
        preparerContexte(new BigDecimal("500000000"), new BigDecimal("333333"));
        LocalDate dateLimite = LocalDate.now().minusMonths(1);
        BigDecimal penalites = useCase.execute(1L, dateLimite);

        assertTrue(penalites.scale() <= 2);
    }

    @Test
    @DisplayName("Test aux bornes 100M et 400M avec 1 mois de retard")
    void testBornesUnMois() {
        // Test a 100M (devrait utiliser taux 2%)
        preparerContexte(new BigDecimal("100000000"), new BigDecimal("1000000"));
        LocalDate dateLimite = LocalDate.now().minusMonths(1);
        BigDecimal penalites = useCase.execute(1L, dateLimite);
        assertEquals(0, penalites.compareTo(new BigDecimal("20000"))); // 1M * 0.02 = 20 000

        // Test a 400M (devrait utiliser taux 3%)
        preparerContexte(new BigDecimal("400000000"), new BigDecimal("1000000"));
        penalites = useCase.execute(1L, dateLimite);
        assertEquals(0, penalites.compareTo(new BigDecimal("30000"))); // 1M * 0.03 = 30 000
    }
}

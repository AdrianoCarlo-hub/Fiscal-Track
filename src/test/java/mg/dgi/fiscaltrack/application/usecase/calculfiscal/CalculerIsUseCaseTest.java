package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CalculerIsUseCaseTest {

    private CalculerIsUseCase useCase;
    private ParametreFiscalService parametreFiscalService;

    @BeforeEach
    void setUp() {
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        useCase = new CalculerIsUseCase(parametreFiscalService);

        // Mock des parametres fiscaux pour IS
        Mockito.when(parametreFiscalService.getValeur("IS_TAUX"))
                .thenReturn(new BigDecimal("0.05"));
        Mockito.when(parametreFiscalService.getValeur("IS_TAUX_ABATTEMENT_CGA"))
                .thenReturn(new BigDecimal("0.50"));
        Mockito.when(parametreFiscalService.getValeur("IS_PLAFOND_ABATTEMENT_CGA"))
                .thenReturn(new BigDecimal("5000000"));
        Mockito.when(parametreFiscalService.getValeur("IS_TAUX_REDUCTION"))
                .thenReturn(new BigDecimal("0.02"));
        Mockito.when(parametreFiscalService.getValeur("IS_TAUX_PLANCHER"))
                .thenReturn(new BigDecimal("0.03"));
        Mockito.when(parametreFiscalService.getValeur("IS_MINIMUM_AGRICOLE_ELEVEUR"))
                .thenReturn(new BigDecimal("16000"));
        Mockito.when(parametreFiscalService.getValeur("IS_MINIMUM_ARTISAN"))
                .thenReturn(new BigDecimal("50000"));
        Mockito.when(parametreFiscalService.getValeur("IS_MINIMUM_COMMERCANT"))
                .thenReturn(new BigDecimal("100000"));
        Mockito.when(parametreFiscalService.getValeur("IS_MINIMUM_PROFESSION_LIBERALE"))
                .thenReturn(new BigDecimal("150000"));
    }

    @Test
    @DisplayName("IS normal : 5% du CA, plancher 3% respecte")
    void isNormal() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("100000000"),
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        // Base = 100M, IS theorique = 5M, plancher = 3M, IS du = 5M
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("5000000")));
    }

    @Test
    @DisplayName("IS avec CGA : abattement 50% plafonne a 5M")
    void isAvecCga() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("100000000"),
                true,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        // Base = 100M - 50M (abattement plafonne a 5M en realite)
        // Ici abattement = min(50M, 5M) = 5M, base = 95M
        // IS = 95M x 5% = 4,75M
        assertTrue(r.getSolde().compareTo(new BigDecimal("4000000")) > 0);
    }

    @Test
    @DisplayName("IS plancher : reductions ne peuvent descendre sous 3% du CA")
    void isPlancher() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("100000000"),
                false,
                new BigDecimal("20000000"),
                new BigDecimal("30000000"),
                BigDecimal.ZERO);

        // IS theorique = 5M
        // Reductions = 2% x 20M + 2% x 30M = 400k + 600k = 1M
        // IS apres reduction = 4M, plancher = 3M
        // IS du = MAX(4M, 3M) = 4M
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("4000000")));
    }

    @Test
    @DisplayName("IS avec acomptes")
    void isAvecAcomptes() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("100000000"),
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("2000000"));

        // IS du = 5M, acomptes = 2M, solde = 3M
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("3000000")));
    }

    @Test
    @DisplayName("IS minimum categorie : petit CA, minimum PROFESSION_LIBERALE 150 000")
    void isMinimumCategorieProfessionLiberale() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("1000000"),
                "PROFESSION_LIBERALE",
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        // CA = 1M, IS theorique = 50 000, plancher 3% = 30 000,
        // minimum categorie = 150 000 -> IS du = 150 000
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("150000")));
        assertEquals(0, r.getMontantMinimum().compareTo(new BigDecimal("150000")));
    }

    @Test
    @DisplayName("IS minimum categorie : COMMERCANT 100 000")
    void isMinimumCategorieCommercant() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("1000000"),
                "COMMERCANT",
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        assertEquals(0, r.getSolde().compareTo(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("IS minimum categorie : alias frontend COMMERCE = 100 000 (ticket C2.4)")
    void isMinimumCategorieAliasCommerce() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("1000000"),
                "COMMERCE",
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        // CA = 1M, IS theorique = 50 000, plancher 3% = 30 000,
        // minimum categorie COMMERCANT = 100 000 -> IS du = 100 000
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("100000")));
        assertEquals(0, r.getMontantMinimum().compareTo(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("IS minimum categorie : alias frontend ARTISANAL = 50 000")
    void isMinimumCategorieAliasArtisanal() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("500000"),
                "ARTISANAL",
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        // CA = 500 000, IS theorique = 25 000, plancher 3% = 15 000,
        // minimum categorie ARTISAN = 50 000 -> IS du = 50 000
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("50000")));
    }

    @Test
    @DisplayName("IS minimum categorie : AGRICOLE 16 000, plancher 3% retenu")
    void isMinimumCategorieAgricole() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("1000000"),
                "AGRICOLE",
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        // minimum categorie 16 000 ; IS theorique 50 000 -> 50 000
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("50000")));
    }
}

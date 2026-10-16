package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CalculerAcompteUseCaseTest {

    private CalculerAcompteUseCase useCase;
    private ParametreFiscalService parametreFiscalService;

    @BeforeEach
    void setUp() {
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        useCase = new CalculerAcompteUseCase(parametreFiscalService);

        // Mock des parametres fiscaux pour acomptes
        Mockito.when(parametreFiscalService.getValeur("ACOMPTE_TAUX_PROVISIONNEL"))
                .thenReturn(new BigDecimal("0.50"));
        Mockito.when(parametreFiscalService.getValeur("ACOMPTE_IS_AGRICOLE_ELEVEUR"))
                .thenReturn(new BigDecimal("16000"));
        Mockito.when(parametreFiscalService.getValeur("ACOMPTE_IS_ARTISAN"))
                .thenReturn(new BigDecimal("50000"));
        Mockito.when(parametreFiscalService.getValeur("ACOMPTE_IS_COMMERCANT"))
                .thenReturn(new BigDecimal("100000"));
        Mockito.when(parametreFiscalService.getValeur("ACOMPTE_IS_PROFESSION_LIBERALE"))
                .thenReturn(new BigDecimal("150000"));
        Mockito.when(parametreFiscalService.getValeur("ACOMPTE_IR_INDUSTRIE_ETC"))
                .thenReturn(new BigDecimal("500000"));
        Mockito.when(parametreFiscalService.getValeur("ACOMPTE_IR_AUTRES"))
                .thenReturn(new BigDecimal("1000000"));
    }

    @Test
    @DisplayName("Acompte provisionnel : 50% de l'impot N-1")
    void acompteProvisionnel() {
        BigDecimal acompte = useCase.calculerAcompteProvisionnel(new BigDecimal("1000000"));
        // Document IR 2026 : 2 acomptes de 25% chacun de l'impot N-1.
        // Un acompte = 25% x 1 000 000 = 250 000.
        assertEquals(0, acompte.compareTo(new BigDecimal("250000")));
    }

    @Test
    @DisplayName("Acompte debut activite IS : AGRICOLE")
    void acompteDebutIsAgricole() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIs("AGRICOLE");
        assertEquals(0, acompte.compareTo(new BigDecimal("16000")));
    }

    @Test
    @DisplayName("Acompte debut activite IS : ELEVEUR")
    void acompteDebutIsEleveur() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIs("ELEVEUR");
        assertEquals(0, acompte.compareTo(new BigDecimal("16000")));
    }

    @Test
    @DisplayName("Acompte debut activite IS : ARTISAN")
    void acompteDebutIsArtisan() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIs("ARTISAN");
        assertEquals(0, acompte.compareTo(new BigDecimal("50000")));
    }

    @Test
    @DisplayName("Acompte debut activite IS : GARGOTIER")
    void acompteDebutIsGargotier() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIs("GARGOTIER");
        assertEquals(0, acompte.compareTo(new BigDecimal("50000")));
    }

    @Test
    @DisplayName("Acompte debut activite IS : PETIT_COMMERCANT")
    void acompteDebutIsPetitCommercant() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIs("PETIT_COMMERCANT");
        assertEquals(0, acompte.compareTo(new BigDecimal("50000")));
    }

    @Test
    @DisplayName("Acompte debut activite IS : COMMERCANT")
    void acompteDebutIsCommercant() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIs("COMMERCANT");
        assertEquals(0, acompte.compareTo(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("Acompte debut activite IS : HOTELIER")
    void acompteDebutIsHotelier() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIs("HOTELIER");
        assertEquals(0, acompte.compareTo(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("Acompte debut activite IS : PRESTATAIRE")
    void acompteDebutIsPrestataire() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIs("PRESTATAIRE");
        assertEquals(0, acompte.compareTo(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("Acompte debut activite IS : PROFESSION_LIBERALE")
    void acompteDebutIsProfessionLiberale() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIs("PROFESSION_LIBERALE");
        assertEquals(0, acompte.compareTo(new BigDecimal("150000")));
    }

    @Test
    @DisplayName("Acompte debut activite IS : ACTIVITES_MULTIPLES")
    void acompteDebutIsActivitesMultiples() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIs("ACTIVITES_MULTIPLES");
        assertEquals(0, acompte.compareTo(new BigDecimal("150000")));
    }

    @Test
    @DisplayName("Acompte debut activite IS : categorie inconnue -> default 50K")
    void acompteDebutIsDefault() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIs("INCONNUE");
        assertEquals(0, acompte.compareTo(new BigDecimal("50000")));
    }

    @Test
    @DisplayName("Acompte debut activite IR : INDUSTRIE")
    void acompteDebutIrIndustrie() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIr("INDUSTRIE");
        assertEquals(0, acompte.compareTo(new BigDecimal("500000")));
    }

    @Test
    @DisplayName("Acompte debut activite IR : ARTISANAT")
    void acompteDebutIrArtisanat() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIr("ARTISANAT");
        assertEquals(0, acompte.compareTo(new BigDecimal("500000")));
    }

    @Test
    @DisplayName("Acompte debut activite IR : AGRICULTURE")
    void acompteDebutIrAgriculture() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIr("AGRICULTURE");
        assertEquals(0, acompte.compareTo(new BigDecimal("500000")));
    }

    @Test
    @DisplayName("Acompte debut activite IR : TOURISME")
    void acompteDebutIrTourisme() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIr("TOURISME");
        assertEquals(0, acompte.compareTo(new BigDecimal("500000")));
    }

    @Test
    @DisplayName("Acompte debut activite IR : COMMERCE -> default 1M")
    void acompteDebutIrCommerce() {
        BigDecimal acompte = useCase.calculerAcompteDebutActiviteIr("COMMERCE");
        assertEquals(0, acompte.compareTo(new BigDecimal("1000000")));
    }

    @Test
    @DisplayName("Acompte provisionnel : impot N-1 null -> exception")
    void acompteProvisionnelNull() {
        assertThrows(CalculFiscalInvalideException.class, () ->
                useCase.calculerAcompteProvisionnel(null));
    }

    @Test
    @DisplayName("Acompte provisionnel : impot N-1 negatif -> exception")
    void acompteProvisionnelNegatif() {
        assertThrows(CalculFiscalInvalideException.class, () ->
                useCase.calculerAcompteProvisionnel(new BigDecimal("-1000")));
    }

    @Test
    @DisplayName("Acompte debut activite IS : categorie null -> exception")
    void acompteDebutIsNull() {
        assertThrows(CalculFiscalInvalideException.class, () ->
                useCase.calculerAcompteDebutActiviteIs(null));
    }

    @Test
    @DisplayName("Acompte debut activite IR : categorie null -> exception")
    void acompteDebutIrNull() {
        assertThrows(CalculFiscalInvalideException.class, () ->
                useCase.calculerAcompteDebutActiviteIr(null));
    }
}

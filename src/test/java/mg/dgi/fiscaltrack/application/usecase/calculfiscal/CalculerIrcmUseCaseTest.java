package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class CalculerIrcmUseCaseTest {

    private CalculerIrcmUseCase useCase;
    private ParametreFiscalService parametreFiscalService;

    @BeforeEach
    void setUp() {
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        
        // Mock des parametres IRCM
        when(parametreFiscalService.getValeur("IRCM_TAUX_NORMAL"))
                .thenReturn(new BigDecimal("0.20"));
        when(parametreFiscalService.getValeur("IRCM_TAUX_COOPERATIVE"))
                .thenReturn(new BigDecimal("0.10"));
        
        useCase = new CalculerIrcmUseCase(parametreFiscalService);
    }

    @Test
    @DisplayName("IRCM normal (non cooperative) : CA 10 000 000 -> IRCM = 2 000 000 (20%)")
    void ircmNormal() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("10000000"),
                false,
                BigDecimal.ZERO);

        // IRCM = 10M x 20% = 2M
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("2000000")));
        assertEquals(0, r.getTauxApplique().compareTo(new BigDecimal("0.20")));
    }

    @Test
    @DisplayName("IRCM cooperative : CA 10 000 000 -> IRCM = 1 000 000 (10%)")
    void ircmCooperative() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("10000000"),
                true,
                BigDecimal.ZERO);

        // IRCM = 10M x 10% = 1M
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("1000000")));
        assertEquals(0, r.getTauxApplique().compareTo(new BigDecimal("0.10")));
    }

    @Test
    @DisplayName("IRCM avec acomptes")
    void ircmAvecAcomptes() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("10000000"),
                false,
                new BigDecimal("500000"));

        // IRCM = 2M - 500K = 1.5M
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("1500000")));
    }

    @Test
    @DisplayName("IRCM : CA null -> exception")
    void ircmCaNull() {
        assertThrows(CalculFiscalInvalideException.class, () ->
                useCase.execute(null, false, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("IRCM : CA negatif -> exception")
    void ircmCaNegatif() {
        assertThrows(CalculFiscalInvalideException.class, () ->
                useCase.execute(new BigDecimal("-1000"), false, BigDecimal.ZERO));
    }
}

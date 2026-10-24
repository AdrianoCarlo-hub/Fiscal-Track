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

class CalculerIpviUseCaseTest {

    private CalculerIpviUseCase useCase;
    private ParametreFiscalService parametreFiscalService;

    @BeforeEach
    void setUp() {
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        
        // Mock des parametres IPVI
        when(parametreFiscalService.getValeur("IPVI_TAUX_PRIX_REVIENT"))
                .thenReturn(new BigDecimal("0.75"));
        when(parametreFiscalService.getValeur("IPVI_TAUX"))
                .thenReturn(new BigDecimal("0.20"));
        
        useCase = new CalculerIpviUseCase(parametreFiscalService);
    }

    @Test
    @DisplayName("IPVI : prix de vente 10 000 000 -> IPVI = 500 000 (25% x 20%)")
    void ipviNormal() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("10000000"),
                BigDecimal.ZERO);

        // Prix de revient = 75% x 10M = 7.5M
        // Base imposable = 10M - 7.5M = 2.5M
        // IPVI = 2.5M x 20% = 500 000
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("500000")));
        assertEquals(0, r.getTauxApplique().compareTo(new BigDecimal("0.20")));
    }

    @Test
    @DisplayName("IPVI avec acomptes")
    void ipviAvecAcomptes() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("10000000"),
                new BigDecimal("200000"));

        // IPVI = 500 000 - 200 000 = 300 000
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("300000")));
    }

    @Test
    @DisplayName("IPVI avec credit")
    void ipviAvecCredit() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("1000000"),
                new BigDecimal("1000000"));

        // Prix de revient forfaitaire = 75% x 1M = 750 000
        // Base imposable = 1M - 750 000 = 250 000
        // IPVI = 250 000 x 20% = 50 000
        // IPVI = 50 000 - 1 000 000 = -950 000 -> credit de 950 000
        assertEquals(0, r.getCreditFiscal().compareTo(new BigDecimal("950000")));
        assertEquals(0, r.getSolde().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("IPVI : prix de vente null -> exception")
    void ipviPrixVenteNull() {
        assertThrows(CalculFiscalInvalideException.class, () ->
                useCase.execute(null, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("IPVI : prix de vente negatif -> exception")
    void ipviPrixVenteNegatif() {
        assertThrows(CalculFiscalInvalideException.class, () ->
                useCase.execute(new BigDecimal("-1000"), BigDecimal.ZERO));
    }
}

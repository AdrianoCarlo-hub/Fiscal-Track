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

import static org.junit.jupiter.api.Assertions.*;

class CalculerImpUseCaseTest {

    private CalculerImpUseCase useCase;
    private ParametreFiscalService parametreFiscalService;

    @BeforeEach
    void setUp() {
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        
        // Mock des parametres IMP
        when(parametreFiscalService.getValeur("IMP_TAUX"))
                .thenReturn(new BigDecimal("0.08"));
        when(parametreFiscalService.getValeur("IMP_TAUX_ACOMPTE"))
                .thenReturn(new BigDecimal("0.005"));
        
        useCase = new CalculerImpUseCase(parametreFiscalService);
    }

    @Test
    @DisplayName("IMP : montant marche 10 000 000 -> IMP = 800 000 (8%)")
    void impNormal() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("10000000"),
                BigDecimal.ZERO);

        // IMP = 10M x 8% = 800 000
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("800000")));
        assertEquals(0, r.getTauxApplique().compareTo(new BigDecimal("0.08")));
    }

    @Test
    @DisplayName("IMP avec acomptes")
    void impAvecAcomptes() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("10000000"),
                new BigDecimal("200000"));

        // IMP = 800 000 - 200 000 = 600 000
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("600000")));
    }

    @Test
    @DisplayName("IMP : montant marche null -> exception")
    void impMontantNull() {
        assertThrows(CalculFiscalInvalideException.class, () ->
                useCase.execute(null, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("IMP : montant marche negatif -> exception")
    void impMontantNegatif() {
        assertThrows(CalculFiscalInvalideException.class, () ->
                useCase.execute(new BigDecimal("-1000"), BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Acompte IMP : 5 pour mille de 10 000 000 = 50 000")
    void acompteImp() {
        BigDecimal acompte = useCase.calculerAcompteImp(new BigDecimal("10000000"));
        assertEquals(0, acompte.compareTo(new BigDecimal("50000")));
    }

    @Test
    @DisplayName("Acompte IMP : montant null -> exception")
    void acompteImpNull() {
        assertThrows(CalculFiscalInvalideException.class, () ->
                useCase.calculerAcompteImp(null));
    }
}

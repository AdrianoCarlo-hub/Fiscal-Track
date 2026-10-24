package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.domain.enums.CategorieActivite;
import mg.dgi.fiscaltrack.domain.exception.TypeImpotNonSupporteException;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculerFiscalDispatcherUseCaseTest {

    private CalculerFiscalDispatcherUseCase useCase;
    private CalculerIrUseCase calculerIrUseCase;
    private CalculerIsUseCase calculerIsUseCase;
    private CalculerIrsaUseCase calculerIrsaUseCase;
    private CalculerTvaUseCase calculerTvaUseCase;
    private CalculerIpviUseCase calculerIpviUseCase;
    private CalculerImpUseCase calculerImpUseCase;
    private CalculerIrcmUseCase calculerIrcmUseCase;

    @BeforeEach
    void setUp() {
        calculerIrUseCase = Mockito.mock(CalculerIrUseCase.class);
        calculerIsUseCase = Mockito.mock(CalculerIsUseCase.class);
        calculerIrsaUseCase = Mockito.mock(CalculerIrsaUseCase.class);
        calculerTvaUseCase = Mockito.mock(CalculerTvaUseCase.class);
        calculerIpviUseCase = Mockito.mock(CalculerIpviUseCase.class);
        calculerImpUseCase = Mockito.mock(CalculerImpUseCase.class);
        calculerIrcmUseCase = Mockito.mock(CalculerIrcmUseCase.class);

        useCase = new CalculerFiscalDispatcherUseCase(
                calculerIrUseCase, calculerIsUseCase, calculerIrsaUseCase,
                calculerTvaUseCase, calculerIpviUseCase, calculerImpUseCase, calculerIrcmUseCase);
    }

    @Test
    @DisplayName("Dispatch IR")
    void dispatchIr() {
        Map<String, Object> params = new HashMap<>();
        ResultatCalculFiscal mockResult = ResultatCalculFiscal.builder()
                .typeImpot("IR")
                .solde(new BigDecimal("1000000"))
                .build();
        Mockito.when(calculerIrUseCase.execute(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(),
                Mockito.anyInt(), Mockito.anyBoolean(), Mockito.anyBoolean()))
                .thenReturn(mockResult);

        ResultatCalculFiscal result = useCase.execute("IR", params);
        assertEquals("IR", result.getTypeImpot());
    }

    @Test
    @DisplayName("Dispatch IS")
    void dispatchIs() {
        Map<String, Object> params = new HashMap<>();
        ResultatCalculFiscal mockResult = ResultatCalculFiscal.builder()
                .typeImpot("IS")
                .solde(new BigDecimal("5000000"))
                .build();
        Mockito.when(calculerIsUseCase.execute(
                Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any(),
                Mockito.any(), Mockito.any()))
                .thenReturn(mockResult);

        ResultatCalculFiscal result = useCase.execute("IS", params);
        assertEquals("IS", result.getTypeImpot());
    }

    @Test
    @DisplayName("Dispatch IRSA")
    void dispatchIrsa() {
        Map<String, Object> params = new HashMap<>();
        ResultatCalculFiscal mockResult = ResultatCalculFiscal.builder()
                .typeImpot("IRSA")
                .solde(new BigDecimal("3000"))
                .build();
        Mockito.when(calculerIrsaUseCase.execute(
                Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.anyInt()))
                .thenReturn(mockResult);

        ResultatCalculFiscal result = useCase.execute("IRSA", params);
        assertEquals("IRSA", result.getTypeImpot());
    }

    @Test
    @DisplayName("Dispatch TVA")
    void dispatchTva() {
        Map<String, Object> params = new HashMap<>();
        ResultatCalculFiscal mockResult = ResultatCalculFiscal.builder()
                .typeImpot("TVA")
                .solde(new BigDecimal("600000"))
                .build();
        Mockito.when(calculerTvaUseCase.execute(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(),
                Mockito.anyBoolean(), Mockito.any()))
                .thenReturn(mockResult);

        ResultatCalculFiscal result = useCase.execute("TVA", params);
        assertEquals("TVA", result.getTypeImpot());
    }

    @Test
    @DisplayName("Dispatch IPVI")
    void dispatchIpvi() {
        Map<String, Object> params = new HashMap<>();
        ResultatCalculFiscal mockResult = ResultatCalculFiscal.builder()
                .typeImpot("IPVI")
                .solde(new BigDecimal("500000"))
                .build();
        Mockito.when(calculerIpviUseCase.execute(
                Mockito.any(), Mockito.any()))
                .thenReturn(mockResult);

        ResultatCalculFiscal result = useCase.execute("IPVI", params);
        assertEquals("IPVI", result.getTypeImpot());
    }

    @Test
    @DisplayName("Dispatch IMP")
    void dispatchImp() {
        Map<String, Object> params = new HashMap<>();
        ResultatCalculFiscal mockResult = ResultatCalculFiscal.builder()
                .typeImpot("IMP")
                .solde(new BigDecimal("800000"))
                .build();
        Mockito.when(calculerImpUseCase.execute(
                Mockito.any(), Mockito.any()))
                .thenReturn(mockResult);

        ResultatCalculFiscal result = useCase.execute("IMP", params);
        assertEquals("IMP", result.getTypeImpot());
    }

    @Test
    @DisplayName("Dispatch IRCM")
    void dispatchIrcm() {
        Map<String, Object> params = new HashMap<>();
        ResultatCalculFiscal mockResult = ResultatCalculFiscal.builder()
                .typeImpot("IRCM")
                .solde(new BigDecimal("2000000"))
                .build();
        Mockito.when(calculerIrcmUseCase.execute(
                Mockito.any(), Mockito.anyBoolean(), Mockito.any()))
                .thenReturn(mockResult);

        ResultatCalculFiscal result = useCase.execute("IRCM", params);
        assertEquals("IRCM", result.getTypeImpot());
    }

    @Test
    @DisplayName("Code impost invalide -> exception")
    void codeInvalide() {
        Map<String, Object> params = new HashMap<>();
        assertThrows(TypeImpotNonSupporteException.class, () ->
                useCase.execute("INVALID", params));
    }

    @Test
    @DisplayName("Code impost null -> exception")
    void codeNull() {
        Map<String, Object> params = new HashMap<>();
        assertThrows(TypeImpotNonSupporteException.class, () ->
                useCase.execute(null, params));
    }
}

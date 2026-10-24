package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.domain.exception.ObligationIntrouvableException;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PrevisualiserDeclarationUseCaseTest {

    private ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private CalculerImpotDeclarationUseCase calculerImpotDeclarationUseCase;
    private PrevisualiserDeclarationUseCase useCase;

    @BeforeEach
    void setUp() {
        obligationRepositoryPort = Mockito.mock(ObligationFiscaleRepositoryPort.class);
        calculerImpotDeclarationUseCase = Mockito.mock(CalculerImpotDeclarationUseCase.class);
        useCase = new PrevisualiserDeclarationUseCase(obligationRepositoryPort, calculerImpotDeclarationUseCase);
    }

    @Test
    @DisplayName("Impôt recalculable : le montant du serveur est renvoye")
    void montantServeurRetenu() {
        when(obligationRepositoryPort.findById(1L)).thenReturn(Optional.of(
                ObligationFiscale.builder().idObligationFiscale(1L).nif("NIF0000001").codeImpot("IR").build()));
        when(calculerImpotDeclarationUseCase.execute(eq("IR"), any(), eq("NIF0000001")))
                .thenReturn(Optional.of(ResultatCalculFiscal.builder()
                        .typeImpot("IR")
                        .solde(new BigDecimal("1200"))
                        .etapesCalcul(List.of("etape 1"))
                        .avertissements(List.of("attention"))
                        .build()));

        Map<String, Object> resultat = useCase.execute(1L, new BigDecimal("5000"));

        assertEquals(true, resultat.get("disponible"));
        assertEquals(0, ((BigDecimal) resultat.get("impotRetenu")).compareTo(new BigDecimal("1200")));
        assertEquals(List.of("etape 1"), resultat.get("etapesCalcul"));
        assertEquals(List.of("attention"), resultat.get("avertissements"));
    }

    @Test
    @DisplayName("Type non recalculable : disponible=false et montant zero")
    void typeNonRecalculable() {
        when(obligationRepositoryPort.findById(2L)).thenReturn(Optional.of(
                ObligationFiscale.builder().idObligationFiscale(2L).nif("NIF0000001").codeImpot("IPVI").build()));
        when(calculerImpotDeclarationUseCase.execute(eq("IPVI"), any(), eq("NIF0000001")))
                .thenReturn(Optional.empty());

        Map<String, Object> resultat = useCase.execute(2L, new BigDecimal("5000"));

        assertEquals(false, resultat.get("disponible"));
        assertEquals(0, ((BigDecimal) resultat.get("impotRetenu")).compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Obligation inexistante : exception")
    void obligationInexistante() {
        when(obligationRepositoryPort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ObligationIntrouvableException.class,
                () -> useCase.execute(99L, new BigDecimal("1000")));
    }

    @Test
    @DisplayName("Chiffre d'affaires negatif : refus")
    void chiffreAffairesNegatif() {
        when(obligationRepositoryPort.findById(3L)).thenReturn(Optional.of(
                ObligationFiscale.builder().idObligationFiscale(3L).nif("NIF0000001").codeImpot("IR").build()));

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(3L, new BigDecimal("-1")));
    }
}

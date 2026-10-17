package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerAcompteUseCase;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerFiscalDispatcherUseCase;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class CalculerImpotDeclarationUseCaseTest {

    private CalculerFiscalDispatcherUseCase dispatcher;
    private CalculerAcompteUseCase calculerAcompteUseCase;
    private DeclarationRepositoryPort declarationRepositoryPort;
    private CalculerImpotDeclarationUseCase useCase;

    @BeforeEach
    void setUp() {
        dispatcher = Mockito.mock(CalculerFiscalDispatcherUseCase.class);
        calculerAcompteUseCase = Mockito.mock(CalculerAcompteUseCase.class);
        declarationRepositoryPort = Mockito.mock(DeclarationRepositoryPort.class);
        useCase = new CalculerImpotDeclarationUseCase(dispatcher, calculerAcompteUseCase, declarationRepositoryPort);
    }

    @Test
    @DisplayName("IR : delegue au dispatcher avec le chiffre d'affaires en caHT")
    void irDelegueAuDispatcher() {
        when(dispatcher.execute(eq("IR"), anyMap()))
                .thenReturn(ResultatCalculFiscal.builder().typeImpot("IR").solde(new BigDecimal("1000")).build());

        Optional<ResultatCalculFiscal> resultat = useCase.execute("IR", new BigDecimal("5000"), "NIF0000001");

        assertTrue(resultat.isPresent());
        assertEquals(0, resultat.get().getSolde().compareTo(new BigDecimal("1000")));
    }

    @Test
    @DisplayName("Type non recalculable : Optional vide")
    void typeNonRecalculable() {
        assertTrue(useCase.execute("IRSA", new BigDecimal("5000"), "NIF0000001").isEmpty());
        assertTrue(useCase.execute("IPVI", new BigDecimal("5000"), "NIF0000001").isEmpty());
        assertTrue(useCase.execute("IMP", new BigDecimal("5000"), "NIF0000001").isEmpty());
    }

    @Test
    @DisplayName("ACOMPTE_IR : 25 % de l'IR N-1 via CalculerAcompteUseCase")
    void acompteIr() {
        Declaration irN1 = Declaration.builder()
                .idDeclaration(1L)
                .impotPrincipalCalcule(new BigDecimal("1000000"))
                .build();
        when(declarationRepositoryPort.findDerniereDeclarationByNifEtImpot("NIF0000001", "IR"))
                .thenReturn(Optional.of(irN1));
        when(calculerAcompteUseCase.calculerAcompteProvisionnel(new BigDecimal("1000000")))
                .thenReturn(new BigDecimal("250000"));

        Optional<ResultatCalculFiscal> resultat = useCase.execute("ACOMPTE_IR", new BigDecimal("0"), "NIF0000001");

        assertTrue(resultat.isPresent());
        assertEquals(0, resultat.get().getSolde().compareTo(new BigDecimal("250000")));
    }

    @Test
    @DisplayName("ACOMPTE_IR sans IR N-1 : Optional vide")
    void acompteIrSansHistorique() {
        when(declarationRepositoryPort.findDerniereDeclarationByNifEtImpot("NIF0000001", "IR"))
                .thenReturn(Optional.empty());

        assertTrue(useCase.execute("ACOMPTE_IR", new BigDecimal("0"), "NIF0000001").isEmpty());
    }
}

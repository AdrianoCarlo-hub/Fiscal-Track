package mg.dgi.fiscaltrack.application.usecase.majoration;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.HistoriquePenaliteRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.SituationIrsa;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AppliquerMajorationIrsaUseCaseTest {

    private CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private ParametreFiscalService parametreFiscalService;
    private AppliquerMajorationIrsaUseCase useCase;

    @BeforeEach
    void setUp() {
        compteRepositoryPort = Mockito.mock(CompteCourantFiscalRepositoryPort.class);
        HistoriquePenaliteRepositoryPort historique = Mockito.mock(HistoriquePenaliteRepositoryPort.class);
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        EnregistrerActionUseCase audit = Mockito.mock(EnregistrerActionUseCase.class);
        useCase = new AppliquerMajorationIrsaUseCase(compteRepositoryPort, historique,
                parametreFiscalService, audit);

        when(parametreFiscalService.getValeur("MAJORATION_IRSA_NON_RETENU")).thenReturn(new BigDecimal("0.40"));
        when(parametreFiscalService.getValeur("MAJORATION_IRSA_RETENU_NON_REVERSE")).thenReturn(new BigDecimal("0.80"));
        when(parametreFiscalService.getValeur("MAJORATION_IRSA_INTERMITTENT_TAUX")).thenReturn(new BigDecimal("0.10"));
        when(parametreFiscalService.getValeur("MAJORATION_IRSA_INTERMITTENT_MIN")).thenReturn(new BigDecimal("20000"));
        when(parametreFiscalService.getValeur("MAJORATION_IRSA_INTERMITTENT_RETENU_TAUX")).thenReturn(new BigDecimal("0.40"));
        when(parametreFiscalService.getValeur("MAJORATION_IRSA_INTERMITTENT_RETENU_MIN")).thenReturn(new BigDecimal("100000"));

        when(compteRepositoryPort.save(any(CompteCourantFiscal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void compte() {
        CompteCourantFiscal compte = CompteCourantFiscal.builder()
                .idCompte(1L)
                .nif("NIF0000001")
                .montantPrincipal(new BigDecimal("1000000"))
                .montantPenalites(BigDecimal.ZERO)
                .montantPaye(BigDecimal.ZERO)
                .statutRecouvrement(StatutRecouvrement.NON_SOLDE)
                .build();
        when(compteRepositoryPort.findById(1L)).thenReturn(Optional.of(compte));
    }

    @Test
    @DisplayName("IRSA non retenu : 40 %")
    void nonRetenu() {
        compte();
        assertEquals(0, useCase.execute(1L, new BigDecimal("1000000"), SituationIrsa.NON_RETENU)
                .compareTo(new BigDecimal("400000")));
    }

    @Test
    @DisplayName("IRSA retenu non reverse : 80 %")
    void retenuNonReverse() {
        compte();
        assertEquals(0, useCase.execute(1L, new BigDecimal("1000000"), SituationIrsa.RETENU_NON_REVERSE)
                .compareTo(new BigDecimal("800000")));
    }

    @Test
    @DisplayName("Retenues intermittentes : 10 % avec plancher 20 000")
    void intermittentPlancher() {
        compte();
        // 10 % de 100 000 = 10 000 -> plancher 20 000
        assertEquals(0, useCase.execute(1L, new BigDecimal("100000"), SituationIrsa.INTERMITTENT)
                .compareTo(new BigDecimal("20000")));
    }

    @Test
    @DisplayName("Retenues intermittentes retenues : 40 % avec plancher 100 000")
    void intermittentRetenuPlancher() {
        compte();
        // 40 % de 100 000 = 40 000 -> plancher 100 000
        assertEquals(0, useCase.execute(1L, new BigDecimal("100000"), SituationIrsa.INTERMITTENT_RETENU)
                .compareTo(new BigDecimal("100000")));
    }
}

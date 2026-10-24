package mg.dgi.fiscaltrack.application.usecase.majoration;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.HistoriquePenaliteRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.NiveauMajorationTva;
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

class AppliquerMajorationTvaUseCaseTest {

    private CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private AppliquerMajorationTvaUseCase useCase;

    @BeforeEach
    void setUp() {
        compteRepositoryPort = Mockito.mock(CompteCourantFiscalRepositoryPort.class);
        HistoriquePenaliteRepositoryPort historique = Mockito.mock(HistoriquePenaliteRepositoryPort.class);
        ParametreFiscalService parametres = Mockito.mock(ParametreFiscalService.class);
        EnregistrerActionUseCase audit = Mockito.mock(EnregistrerActionUseCase.class);
        useCase = new AppliquerMajorationTvaUseCase(compteRepositoryPort, historique, parametres, audit);

        when(parametres.getValeur("MAJORATION_TVA_NIVEAU_1")).thenReturn(new BigDecimal("0.10"));
        when(parametres.getValeur("MAJORATION_TVA_NIVEAU_2")).thenReturn(new BigDecimal("0.40"));
        when(parametres.getValeur("MAJORATION_TVA_NIVEAU_3")).thenReturn(new BigDecimal("0.80"));
        when(parametres.getValeur("MAJORATION_TVA_NIVEAU_4")).thenReturn(new BigDecimal("1.50"));

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
    @DisplayName("TVA niveau 1 : 10 %")
    void niveau1() {
        compte();
        assertEquals(0, useCase.execute(1L, new BigDecimal("1000000"), NiveauMajorationTva.NIVEAU_1)
                .compareTo(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("TVA niveau 2 : 40 %")
    void niveau2() {
        compte();
        assertEquals(0, useCase.execute(1L, new BigDecimal("1000000"), NiveauMajorationTva.NIVEAU_2)
                .compareTo(new BigDecimal("400000")));
    }

    @Test
    @DisplayName("TVA niveau 3 : 80 %")
    void niveau3() {
        compte();
        assertEquals(0, useCase.execute(1L, new BigDecimal("1000000"), NiveauMajorationTva.NIVEAU_3)
                .compareTo(new BigDecimal("800000")));
    }

    @Test
    @DisplayName("TVA niveau 4 : 150 %")
    void niveau4() {
        compte();
        assertEquals(0, useCase.execute(1L, new BigDecimal("1000000"), NiveauMajorationTva.NIVEAU_4)
                .compareTo(new BigDecimal("1500000")));
    }
}

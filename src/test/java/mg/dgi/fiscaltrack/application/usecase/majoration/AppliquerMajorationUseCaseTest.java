package mg.dgi.fiscaltrack.application.usecase.majoration;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.HistoriquePenaliteRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.HistoriquePenalite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppliquerMajorationUseCaseTest {

    private CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private HistoriquePenaliteRepositoryPort historiquePenaliteRepositoryPort;
    private ParametreFiscalService parametreFiscalService;
    private EnregistrerActionUseCase enregistrerActionUseCase;
    private AppliquerMajorationUseCase useCase;

    @BeforeEach
    void setUp() {
        compteRepositoryPort = Mockito.mock(CompteCourantFiscalRepositoryPort.class);
        historiquePenaliteRepositoryPort = Mockito.mock(HistoriquePenaliteRepositoryPort.class);
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        enregistrerActionUseCase = Mockito.mock(EnregistrerActionUseCase.class);
        useCase = new AppliquerMajorationUseCase(compteRepositoryPort, historiquePenaliteRepositoryPort,
                parametreFiscalService, enregistrerActionUseCase);

        when(parametreFiscalService.getValeur("MAJORATION_TAUX")).thenReturn(new BigDecimal("0.40"));
        when(compteRepositoryPort.save(any(CompteCourantFiscal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CompteCourantFiscal compte(String penalites) {
        CompteCourantFiscal compte = CompteCourantFiscal.builder()
                .idCompte(1L)
                .nif("NIF0000001")
                .montantPrincipal(new BigDecimal("1000000"))
                .montantPenalites(new BigDecimal(penalites))
                .montantPaye(BigDecimal.ZERO)
                .statutRecouvrement(StatutRecouvrement.NON_SOLDE)
                .build();
        when(compteRepositoryPort.findById(1L)).thenReturn(Optional.of(compte));
        return compte;
    }

    @Test
    @DisplayName("20.01.54 : majoration de 40 % du montant de base")
    void majoration40Pourcent() {
        compte("0");
        BigDecimal montant = useCase.execute(1L, new BigDecimal("1000000"));

        assertEquals(0, montant.compareTo(new BigDecimal("400000")));
    }

    @Test
    @DisplayName("La majoration s'ajoute aux penalites existantes et est tracee")
    void ajoutEtTraceabilite() {
        CompteCourantFiscal compte = compte("100000");
        BigDecimal montant = useCase.execute(1L, new BigDecimal("1000000"));

        assertEquals(0, montant.compareTo(new BigDecimal("400000")));
        assertEquals(0, compte.getMontantPenalites().compareTo(new BigDecimal("500000")));
        verify(historiquePenaliteRepositoryPort, times(1)).save(any(HistoriquePenalite.class));
        verify(enregistrerActionUseCase, times(1)).execute(eq("NIF0000001"), any(), any());
    }
}

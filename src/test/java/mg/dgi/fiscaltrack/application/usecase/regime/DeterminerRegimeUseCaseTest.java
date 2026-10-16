package mg.dgi.fiscaltrack.application.usecase.regime;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.model.ParametreFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeterminerRegimeUseCaseTest {

    private ParametreFiscalService parametreFiscalService;
    private DeterminerRegimeUseCase useCase;

    @BeforeEach
    void setUp() {
        parametreFiscalService = mock(ParametreFiscalService.class);
        when(parametreFiscalService.getValeur("REGIME_TVA_SEUIL"))
                .thenReturn(new BigDecimal("400000000"));
        when(parametreFiscalService.getParametresAConfirmer())
                .thenReturn(List.of());
        useCase = new DeterminerRegimeUseCase(parametreFiscalService);
    }

    @Test
    void seuilTvaProduitUnAvertissementSansAttribuerUnRegime() {
        var avertissements = useCase.verifierAvertissements(new BigDecimal("400000000"));

        assertEquals(1, avertissements.size());
        assertTrue(avertissements.get(0).contains("TVA confirmé"));
        assertTrue(useCase.estAssujettiTva(new BigDecimal("400000000")));
    }

    @Test
    void seuilsDeRegimeAbsentsNeSontPasInventes() {
        var avertissements = useCase.verifierAvertissements(new BigDecimal("250000000"));

        assertEquals(1, avertissements.size());
        assertEquals("Aucun avertissement de seuil.", avertissements.get(0));
        assertFalse(useCase.estAssujettiTva(new BigDecimal("250000000")));
    }

    @Test
    void seuilDeRegimeMarqueAConfirmerNeProduitQuUnAvertissement() {
        when(parametreFiscalService.getParametresAConfirmer())
                .thenReturn(List.of(ParametreFiscal.builder()
                        .code("REGIME_IS_SYNTHETIQUE_SEUIL")
                        .valeur(new BigDecimal("200000000"))
                        .anneeDebut(Year.of(2026))
                        .baseLegale("À CONFIRMER")
                        .build()));

        var avertissements = useCase.verifierAvertissements(new BigDecimal("250000000"));

        assertEquals(1, avertissements.size());
        assertTrue(avertissements.get(0).contains("À CONFIRMER"));
        assertFalse(avertissements.get(0).contains("régime réel"));
    }
}

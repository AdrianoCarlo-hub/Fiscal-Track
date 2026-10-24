package mg.dgi.fiscaltrack.application.usecase.regime;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.enums.RegimeImposition;
import mg.dgi.fiscaltrack.domain.model.ParametreFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
        assertTrue(avertissements.get(0).contains("régime du réel applicable (CDI 01.01.13)"));
        assertTrue(avertissements.get(0).contains("TVA confirmé"));
        assertTrue(useCase.estAssujettiTva(new BigDecimal("400000000")));
    }

    @Test
    void regimeSynthétiqueAuDelaDe200MProduitUnAvertissementI19() {
        var avertissements = useCase.verifierAvertissements(
                new BigDecimal("250000000"), RegimeImposition.SYNTHETIQUE);

        assertEquals(1, avertissements.size());
        assertTrue(avertissements.get(0).contains("régime synthétique et CA > 200000000"));
        assertTrue(avertissements.get(0).contains("CPF I-19"));
        assertTrue(avertissements.get(0).contains("Aucun changement automatique de régime"));
    }

    @Test
    void regimeSynthétiqueAuSeuilExact200MPasDAvertissementI19() {
        var avertissements = useCase.verifierAvertissements(
                new BigDecimal("200000000"), RegimeImposition.SYNTHETIQUE);

        assertEquals(1, avertissements.size());
        assertEquals("Aucun avertissement de seuil.", avertissements.get(0));
    }

    @Test
    void regimeReelAuDelaDe200MPasDAvertissementI19() {
        var avertissements = useCase.verifierAvertissements(
                new BigDecimal("250000000"), RegimeImposition.REEL);

        assertEquals(1, avertissements.size());
        assertEquals("Aucun avertissement de seuil.", avertissements.get(0));
    }

    @Test
    void seulCaEstDemandeAucunRegimeNestModifie() {
        // La methode ne retourne que des avertissements (List<String>) :
        // aucun regime n'est en retour, aucun changement automatique possible.
        var avertissements = useCase.verifierAvertissements(
                new BigDecimal("500000000"), RegimeImposition.SYNTHETIQUE);

        assertEquals(2, avertissements.size());
        assertTrue(avertissements.get(0).contains("régime du réel applicable"));
        assertTrue(avertissements.get(1).contains("CPF I-19"));
    }

    @Test
    void seuilIrvingtMillionsPasCode() {
        // Seuil IR 20 M : ni code ni simule (sources en conflit)
        var avertissements = useCase.verifierAvertissements(
                new BigDecimal("25000000"), RegimeImposition.SYNTHETIQUE);

        assertEquals(1, avertissements.size());
        assertEquals("Aucun avertissement de seuil.", avertissements.get(0));
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

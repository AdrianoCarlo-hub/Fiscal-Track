package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerIrUseCase;
import mg.dgi.fiscaltrack.domain.enums.CategorieActivite;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LiquidationAcomptesCalculTest {

    private CalculerIrUseCase calculerIr;

    @BeforeEach
    void setUp() {
        ParametreFiscalService parametres = Mockito.mock(ParametreFiscalService.class);
        Mockito.when(parametres.getValeur("IR_TAUX_NORMAL")).thenReturn(new BigDecimal("0.20"));
        Mockito.when(parametres.getValeur("IR_PART_FIXE_AGRICOLE"))
                .thenReturn(new BigDecimal("500000"));
        Mockito.when(parametres.getValeur("IR_PART_FIXE_AUTRES"))
                .thenReturn(new BigDecimal("1000000"));
        Mockito.when(parametres.getValeur("IR_TAUX_VARIABLE_AGRICOLE"))
                .thenReturn(new BigDecimal("0.01"));
        calculerIr = new CalculerIrUseCase(parametres);
    }

    private ResultatCalculFiscal ir(String ca, String charges, String acomptes) {
        return calculerIr.execute(
                new BigDecimal(ca),
                new BigDecimal(charges),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                2026,
                CategorieActivite.INDUSTRIEL,
                new BigDecimal(acomptes),
                12,
                false,
                false);
    }

    @Test
    @DisplayName("Liquidation 5 000 000 - 3 000 000 d'acomptes = solde 2 000 000")
    void liquidationSoldeDeuxMillions() {
        ResultatCalculFiscal resultat = ir("100000000", "75000000", "3000000");

        assertEquals(0, new BigDecimal("5000000").compareTo(resultat.getMontantBrut()));
        assertEquals(0, new BigDecimal("2000000").compareTo(resultat.getSolde()));
        assertEquals(0, BigDecimal.ZERO.compareTo(resultat.getCreditFiscal()));
    }

    @Test
    @DisplayName("Liquidation 2 000 000 - 3 000 000 d'acomptes = solde 0 et credit 1 000 000 reporte")
    void liquidationCreditUnMillion() {
        ResultatCalculFiscal resultat = ir("100000000", "90000000", "3000000");

        assertEquals(0, new BigDecimal("2000000").compareTo(resultat.getMontantBrut()));
        assertEquals(0, BigDecimal.ZERO.compareTo(resultat.getSolde()));
        assertEquals(0, new BigDecimal("1000000").compareTo(resultat.getCreditFiscal()));
    }
}

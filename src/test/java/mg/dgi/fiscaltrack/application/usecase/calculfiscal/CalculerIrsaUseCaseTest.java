package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CalculerIrsaUseCaseTest {

    private CalculerIrsaUseCase useCase;
    private ParametreFiscalService parametreFiscalService;

    @BeforeEach
    void setUp() {
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        useCase = new CalculerIrsaUseCase(parametreFiscalService);

        // Mock des parametres fiscaux pour IRSA
        Mockito.when(parametreFiscalService.getValeur("IRSA_SEUIL_1"))
                .thenReturn(new BigDecimal("350000"));
        Mockito.when(parametreFiscalService.getValeur("IRSA_SEUIL_2"))
                .thenReturn(new BigDecimal("400000"));
        Mockito.when(parametreFiscalService.getValeur("IRSA_SEUIL_3"))
                .thenReturn(new BigDecimal("500000"));
        Mockito.when(parametreFiscalService.getValeur("IRSA_SEUIL_4"))
                .thenReturn(new BigDecimal("600000"));
        Mockito.when(parametreFiscalService.getValeur("IRSA_TAUX_0"))
                .thenReturn(new BigDecimal("0.00"));
        Mockito.when(parametreFiscalService.getValeur("IRSA_TAUX_1"))
                .thenReturn(new BigDecimal("0.05"));
        Mockito.when(parametreFiscalService.getValeur("IRSA_TAUX_2"))
                .thenReturn(new BigDecimal("0.10"));
        Mockito.when(parametreFiscalService.getValeur("IRSA_TAUX_3"))
                .thenReturn(new BigDecimal("0.15"));
        Mockito.when(parametreFiscalService.getValeur("IRSA_TAUX_4"))
                .thenReturn(new BigDecimal("0.20"));
        Mockito.when(parametreFiscalService.getValeur("IRSA_MINIMUM"))
                .thenReturn(new BigDecimal("3000"));
        Mockito.when(parametreFiscalService.getValeur("IRSA_REDUCTION_PAR_PERSONNE"))
                .thenReturn(new BigDecimal("2000"));
    }

    @Test
    @DisplayName("IRSA tranche 1 : salaire <= 350 000, taux 0%")
    void irsaTranche1() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("300000"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);

        // Bareme = 0, minimum = 3000
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("3000")));
    }

    @Test
    @DisplayName("IRSA tranche 3 : 400 001 - 500 000, taux progressif")
    void irsaTranche3() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("450000"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);

        // 350 000 x 0% = 0
        // 50 000 x 5% = 2 500
        // 50 000 x 10% = 5 000
        // Total bareme = 7 500 Ar
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("7500")));
    }

    @Test
    @DisplayName("IRSA avec personne a charge : reduction de 2000 Ar par personne")
    void irsaAvecCharge() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("500000"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 2);

        // Bareme :
        //   350 000 x 0%  = 0
        //   50 000  x 5%  = 2 500
        //   100 000 x 10% = 10 000
        // Total = 12 500 Ar
        // Reduction = 2 x 2 000 = 4 000 Ar
        // IRSA du = 12 500 - 4 000 = 8 500 Ar
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("8500")));
    }

    @Test
    @DisplayName("IRSA minimum : salaire faible, minimum 3000 Ar")
    void irsaMinimum() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("100000"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);

        assertEquals(0, r.getSolde().compareTo(new BigDecimal("3000")));
    }

    @Test
    @DisplayName("IRSA salaire eleve + plusieurs personnes a charge")
    void irsaSalaireElevePlusieursCharges() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("2000000"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 5);

        // Bareme :
        //   350 000 x 0%   = 0
        //   50 000  x 5%   = 2 500
        //   100 000 x 10%  = 10 000
        //   100 000 x 15%  = 15 000
        //   1 400 000 x 20% = 280 000
        // Total = 307 500 ; reduction = 5 x 2 000 = 10 000
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("297500")));
    }

    @Test
    @DisplayName("IRSA : la reduction est refusee si la base est <= 350 000 Ar")
    void irsaReductionRefuseeBaseEgaleSeuil() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("350000"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 2);

        // Base = 350 000 (non strictement superieure) -> aucune reduction ;
        // bareme = 0 -> plancher legal 3 000 Ar
        assertEquals(0, r.getReductions().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("3000")));
    }

    @Test
    @DisplayName("IRSA : la reduction s'applique des que la base depasse 350 000 Ar")
    void irsaReductionAppliqueeAuDelaSeuil() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("351000"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1);

        // Bareme = 1 000 x 5% = 50 -> plancher 3 000
        // Reduction = 2 000 (base > 350 000) -> 1 000, verrouille a 3 000
        assertEquals(0, r.getReductions().compareTo(new BigDecimal("2000")));
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("3000")));
    }

    @Test
    @DisplayName("IRSA : la reduction ne peut jamais passer sous le plancher de 3 000 Ar")
    void irsaPlancherVerrouilleApresReduction() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("500000"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 10);

        // Bareme = 12 500 ; reduction = 10 x 2 000 = 20 000 -> irsa = -7 500
        // Verrouillage strict : IRSA du = 3 000 Ar
        assertEquals(0, r.getReductions().compareTo(new BigDecimal("20000")));
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("3000")));
    }

    @Test
    @DisplayName("IRSA : arrondi de la base a la centaine inferieure")
    void irsaArrondiCentaineInferieure() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("450099"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);

        // Base arrondie = 450 000 -> bareme 7 500 Ar
        assertEquals(0, r.getBaseImposable().compareTo(new BigDecimal("450000")));
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("7500")));
    }
}

package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.acompte.AcomptesObligatoiresService;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.domain.model.SaisieDeclaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreparerDonneesDeclarationAcomptesTest {

    private ContribuableRepositoryPort contribuableRepositoryPort;
    private ParametreFiscalService parametreFiscalService;
    private AcomptesObligatoiresService acomptesService;
    private PreparerDonneesDeclarationUseCase useCase;

    @BeforeEach
    void setUp() {
        contribuableRepositoryPort = Mockito.mock(ContribuableRepositoryPort.class);
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        acomptesService = Mockito.mock(AcomptesObligatoiresService.class);
        useCase = new PreparerDonneesDeclarationUseCase(contribuableRepositoryPort,
                parametreFiscalService, acomptesService);
        when(contribuableRepositoryPort.findByNif(anyString())).thenReturn(java.util.Optional.empty());
    }

    private ObligationFiscale obligation(String code, String periode) {
        return ObligationFiscale.builder()
                .idObligationFiscale(1L)
                .nif("NIF0000001")
                .codeImpot(code)
                .periodeFiscale(periode)
                .build();
    }

    private AcomptesObligatoiresService.Liquidation liquidation(String total,
                                                               String payes,
                                                               String credit) {
        Map<String, Object> detail = Map.of(
                "codeImpot", "IS",
                "acomptesPayes", new BigDecimal(payes),
                "autresAcomptes", BigDecimal.ZERO,
                "creditAnterieur", new BigDecimal(credit),
                "totalDeduit", new BigDecimal(total),
                "avertissements", List.of());
        return new AcomptesObligatoiresService.Liquidation(
                new BigDecimal(payes), BigDecimal.ZERO, new BigDecimal(credit),
                new BigDecimal(total), List.of(), detail);
    }

    @Test
    @DisplayName("IS : le champ acomptes envoye par le client est ignore, le serveur fait foi")
    void isAcomptesClientIgnores() {
        when(acomptesService.liquider("NIF0000001", "IS", 2025, null, null))
                .thenReturn(liquidation("3000000", "2000000", "1000000"));

        PreparerDonneesDeclarationUseCase.DonneesDeclaration donnees = useCase.execute(
                SaisieDeclaration.builder()
                        .caBrut(new BigDecimal("100000000"))
                        .acomptes(new BigDecimal("999999999"))
                        .build(),
                obligation("IS", "2025"));

        assertEquals(new BigDecimal("3000000"), donnees.params().get("acomptes"));
        assertEquals(new BigDecimal("3000000"), donnees.donneesSaisies().get("acomptes"));
        assertEquals(0, new BigDecimal("1000000").compareTo(donnees.creditAnterieur()));
    }

    @Test
    @DisplayName("IR : le champ acomptes envoye par le client est ignore")
    void irAcomptesClientIgnores() {
        when(acomptesService.liquider("NIF0000001", "IR", 2025, null, null))
                .thenReturn(liquidation("750000", "750000", "0"));

        PreparerDonneesDeclarationUseCase.DonneesDeclaration donnees = useCase.execute(
                SaisieDeclaration.builder()
                        .caHT(new BigDecimal("50000000"))
                        .acomptes(new BigDecimal("50000000"))
                        .build(),
                obligation("IR", "2025"));

        assertEquals(new BigDecimal("750000"), donnees.params().get("acomptes"));
        assertEquals(new BigDecimal("750000"), donnees.donneesSaisies().get("acomptes"));
    }

    @Test
    @DisplayName("IS : acomptes de douane negatifs refuses")
    void isDouaneNegativeRefusee() {
        CalculFiscalInvalideException e = assertThrows(CalculFiscalInvalideException.class,
                () -> useCase.execute(SaisieDeclaration.builder()
                        .caBrut(new BigDecimal("100000000"))
                        .acomptesDouane(new BigDecimal("-1"))
                        .build(), obligation("IS", "2025")));

        assertTrue(e.getMessage().contains("douane"));
        verify(acomptesService, never()).liquider(anyString(), anyString(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("IS : acomptes marches publics negatifs refuses")
    void isMarchesPublicsNegatifsRefuses() {
        CalculFiscalInvalideException e = assertThrows(CalculFiscalInvalideException.class,
                () -> useCase.execute(SaisieDeclaration.builder()
                        .caBrut(new BigDecimal("100000000"))
                        .acomptesMarchesPublics(new BigDecimal("-5"))
                        .build(), obligation("IS", "2025")));

        assertTrue(e.getMessage().contains("marches publics"));
    }

    @Test
    @DisplayName("IR : acomptes de douane negatifs refuses")
    void irDouaneNegativeRefusee() {
        CalculFiscalInvalideException e = assertThrows(CalculFiscalInvalideException.class,
                () -> useCase.execute(SaisieDeclaration.builder()
                        .caHT(new BigDecimal("50000000"))
                        .acomptesDouane(new BigDecimal("-100"))
                        .build(), obligation("IR", "2025")));

        assertTrue(e.getMessage().contains("douane"));
    }

    @Test
    @DisplayName("Acompte : le montant est celui calcule par le serveur")
    void acompteMontantServeur() {
        AcomptesObligatoiresService.CalculAcompte calcul =
                new AcomptesObligatoiresService.CalculAcompte(true,
                        new BigDecimal("312500"), new BigDecimal("1250000"),
                        new BigDecimal("0.25"), 2022, "IS", null, List.of(), Map.of());
        when(acomptesService.calculerPourObligation(any(ObligationFiscale.class)))
                .thenReturn(calcul);

        PreparerDonneesDeclarationUseCase.DonneesDeclaration donnees = useCase.execute(
                SaisieDeclaration.builder().build(), obligation("ACOMPTE_IS", "2026-AC1"));

        assertEquals(new BigDecimal("312500"), donnees.params().get("acompteMontant"));
        assertEquals(new BigDecimal("1250000"), donnees.params().get("baseAcompte"));
        assertEquals(new BigDecimal("0.25"), donnees.params().get("tauxAcompte"));
        assertEquals(2022, donnees.params().get("exerciceBase"));
    }

    @Test
    @DisplayName("Acompte : sans base disponible, le depot est refuse avec le message serveur")
    void acompteIndisponibleRefuse() {
        AcomptesObligatoiresService.CalculAcompte calcul =
                new AcomptesObligatoiresService.CalculAcompte(false,
                        BigDecimal.ZERO, null, null, null, "IR",
                        "Aucune declaration IR validee anterieure", List.of("message"), Map.of());
        when(acomptesService.calculerPourObligation(any(ObligationFiscale.class)))
                .thenReturn(calcul);

        CalculFiscalInvalideException e = assertThrows(CalculFiscalInvalideException.class,
                () -> useCase.execute(SaisieDeclaration.builder().build(),
                        obligation("ACOMPTE_IR", "2026-AC1")));

        assertTrue(e.getMessage().contains("Aucune declaration IR validee"));
    }
}

package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.acompte.AcomptesObligatoiresService;
import mg.dgi.fiscaltrack.domain.enums.FormeJuridique;
import mg.dgi.fiscaltrack.domain.enums.StatutActivite;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenererObligationsAcomptesObligatoiresTest {

    private ContribuableRepositoryPort contribuableRepositoryPort;
    private GenererObligationsUseCase genererObligationsUseCase;
    private AcomptesObligatoiresService acomptesService;
    private ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private GenererObligationsAcomptesUseCase useCase;

    @BeforeEach
    void setUp() {
        contribuableRepositoryPort = Mockito.mock(ContribuableRepositoryPort.class);
        genererObligationsUseCase = Mockito.mock(GenererObligationsUseCase.class);
        acomptesService = Mockito.mock(AcomptesObligatoiresService.class);
        obligationRepositoryPort = Mockito.mock(ObligationFiscaleRepositoryPort.class);
        useCase = new GenererObligationsAcomptesUseCase(contribuableRepositoryPort,
                genererObligationsUseCase, acomptesService, obligationRepositoryPort);
    }

    private Contribuable contribuable(FormeJuridique forme) {
        return Contribuable.builder()
                .nif("NIF0000001")
                .formeJuridique(forme)
                .statutActivite(StatutActivite.ACTIF)
                .categorieActivite("COMMERCE")
                .build();
    }

    private AcomptesObligatoiresService.CalculAcompte calculDisponible(String code, String montant) {
        return new AcomptesObligatoiresService.CalculAcompte(true,
                new BigDecimal(montant), new BigDecimal("6000000"),
                new BigDecimal("0.25"), 2022, code, null, List.of(), Map.of());
    }

    @Test
    @DisplayName("Generation obligatoire : deux acomptes IR et deux acomptes IS avec apercu")
    void executeObligatoiresCreeQuatreObligations() {
        when(contribuableRepositoryPort.findAll()).thenReturn(List.of(contribuable(FormeJuridique.SARL)));
        when(acomptesService.calculerPeriodique("NIF0000001", "IR", 2026))
                .thenReturn(calculDisponible("IR", "1500000"));
        when(acomptesService.calculerPeriodique("NIF0000001", "IS", 2026))
                .thenReturn(calculDisponible("IS", "1500000"));
        when(genererObligationsUseCase.execute(any(), any(), any())).thenAnswer(invocation ->
                ObligationFiscale.builder()
                        .nif(invocation.getArgument(0))
                        .codeImpot(invocation.getArgument(1))
                        .periodeFiscale(invocation.getArgument(2))
                        .build());

        int crees = useCase.executeObligatoires(2026);

        assertEquals(4, crees);
        verify(acomptesService, Mockito.times(4))
                .mettreAJourApercu(any(ObligationFiscale.class), any());
        verify(obligationRepositoryPort, Mockito.times(4)).save(any(ObligationFiscale.class));
    }

    @Test
    @DisplayName("Sans declaration validee, aucun acompte periodique n'est genere")
    void sansReferenceAucuneGeneration() {
        when(contribuableRepositoryPort.findAll()).thenReturn(List.of(contribuable(FormeJuridique.SARL)));
        AcomptesObligatoiresService.CalculAcompte indisponible =
                new AcomptesObligatoiresService.CalculAcompte(false, BigDecimal.ZERO,
                        null, null, null, "IR", "Aucune declaration validee", List.of(), Map.of());
        when(acomptesService.calculerPeriodique("NIF0000001", "IR", 2026)).thenReturn(indisponible);
        when(acomptesService.calculerPeriodique("NIF0000001", "IS", 2026)).thenReturn(indisponible);

        int crees = useCase.executeObligatoires(2026);

        assertEquals(0, crees);
        verify(genererObligationsUseCase, never()).execute(any(), any(), any());
    }

    @Test
    @DisplayName("Idempotence : un doublon existant est ignore sans erreur")
    void idempotenceDoublonIgnore() {
        when(contribuableRepositoryPort.findAll()).thenReturn(List.of(contribuable(FormeJuridique.SARL)));
        when(acomptesService.calculerPeriodique("NIF0000001", "IR", 2026))
                .thenReturn(calculDisponible("IR", "1500000"));
        when(acomptesService.calculerPeriodique("NIF0000001", "IS", 2026))
                .thenReturn(calculDisponible("IS", "1500000"));
        when(genererObligationsUseCase.execute(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Obligation deja existante"));

        int crees = useCase.executeObligatoires(2026);

        assertEquals(0, crees);
        verify(obligationRepositoryPort, never()).save(any(ObligationFiscale.class));
    }

    @Test
    @DisplayName("Debut d'activite societe : acompte IS forfaitaire AAAA-DEBUT")
    void debutActiviteSocieteGenereIs() {
        Contribuable societe = contribuable(FormeJuridique.SARL);
        AcomptesObligatoiresService.CalculAcompte calcul =
                new AcomptesObligatoiresService.CalculAcompte(true,
                        new BigDecimal("100000"), null, null, null,
                        "IS", null, List.of(), Map.of());
        when(acomptesService.calculerDebutActivite("IS", "COMMERCE")).thenReturn(calcul);
        ObligationFiscale obligation = ObligationFiscale.builder()
                .nif("NIF0000001").codeImpot("ACOMPTE_IS")
                .periodeFiscale(LocalDate.now().getYear() + "-DEBUT").build();
        when(genererObligationsUseCase.execute("NIF0000001", "ACOMPTE_IS",
                LocalDate.now().getYear() + "-DEBUT")).thenReturn(obligation);

        useCase.genererDebutActivite(societe);

        verify(acomptesService).mettreAJourApercu(eq(obligation), eq(calcul));
        verify(obligationRepositoryPort).save(obligation);
    }

    @Test
    @DisplayName("Debut d'activite entreprise individuelle : acompte IR forfaitaire")
    void debutActiviteEntrepriseIndividuelleGenereIr() {
        Contribuable ei = contribuable(FormeJuridique.EI);
        AcomptesObligatoiresService.CalculAcompte calcul =
                new AcomptesObligatoiresService.CalculAcompte(true,
                        new BigDecimal("1000000"), null, null, null,
                        "IR", null, List.of(), Map.of());
        when(acomptesService.calculerDebutActivite("IR", "COMMERCE")).thenReturn(calcul);
        ObligationFiscale obligation = ObligationFiscale.builder()
                .nif("NIF0000001").codeImpot("ACOMPTE_IR")
                .periodeFiscale(LocalDate.now().getYear() + "-DEBUT").build();
        when(genererObligationsUseCase.execute("NIF0000001", "ACOMPTE_IR",
                LocalDate.now().getYear() + "-DEBUT")).thenReturn(obligation);

        useCase.genererDebutActivite(ei);

        verify(acomptesService).calculerDebutActivite("IR", "COMMERCE");
        verify(obligationRepositoryPort).save(obligation);
    }
}

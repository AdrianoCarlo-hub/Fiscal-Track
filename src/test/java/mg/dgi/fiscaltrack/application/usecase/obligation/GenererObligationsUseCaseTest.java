package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.TypeImpotRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.enums.Periodicite;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.domain.model.TypeImpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class GenererObligationsUseCaseTest {

    private ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private TypeImpotRepositoryPort typeImpotRepositoryPort;
    private ContribuableRepositoryPort contribuableRepositoryPort;
    private CalculerEcheanceReelleUseCase calculerEcheanceReelleUseCase;
    private ParametreFiscalService parametreFiscalService;
    private GenererObligationsUseCase useCase;

    @BeforeEach
    void setUp() {
        obligationRepositoryPort = Mockito.mock(ObligationFiscaleRepositoryPort.class);
        typeImpotRepositoryPort = Mockito.mock(TypeImpotRepositoryPort.class);
        contribuableRepositoryPort = Mockito.mock(ContribuableRepositoryPort.class);
        calculerEcheanceReelleUseCase = Mockito.mock(CalculerEcheanceReelleUseCase.class);
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);

        when(typeImpotRepositoryPort.findByCodeImpot(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            return Optional.of(TypeImpot.builder()
                    .codeImpot(code)
                    .nomImpot(code)
                    .periodicite(Periodicite.ANNUELLE)
                    .build());
        });

        when(obligationRepositoryPort.findByNifEtPeriode(anyString(), anyString()))
                .thenReturn(List.of());
        when(obligationRepositoryPort.save(any(ObligationFiscale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(contribuableRepositoryPort.findByNif(anyString())).thenReturn(Optional.empty());

        // Le report reel est teste separement : ici on renvoie la date theorique inchangee
        when(calculerEcheanceReelleUseCase.calculer(any(LocalDate.class)))
                .thenAnswer(invocation -> new CalculerEcheanceReelleUseCase.EcheanceReelle(
                        invocation.getArgument(0), null));

        when(parametreFiscalService.getValeur("ECHEANCE_IR_EXERCICE_CIVIL_JOUR")).thenReturn(new BigDecimal("15"));
        when(parametreFiscalService.getValeur("ECHEANCE_IR_EXERCICE_CIVIL_MOIS")).thenReturn(new BigDecimal("5"));
        when(parametreFiscalService.getValeur("ECHEANCE_IR_CLOTURE_30_JUIN_JOUR")).thenReturn(new BigDecimal("15"));
        when(parametreFiscalService.getValeur("ECHEANCE_IR_CLOTURE_30_JUIN_MOIS")).thenReturn(new BigDecimal("11"));
        when(parametreFiscalService.getValeur("ECHEANCE_IR_AUTRE_CLOTURE_JOUR")).thenReturn(new BigDecimal("15"));
        when(parametreFiscalService.getValeur("ECHEANCE_IR_AUTRE_CLOTURE_DELAI_MOIS")).thenReturn(new BigDecimal("4"));
        when(parametreFiscalService.getValeur("ECHEANCE_IS_JOUR")).thenReturn(new BigDecimal("31"));
        when(parametreFiscalService.getValeur("ECHEANCE_IS_MOIS")).thenReturn(new BigDecimal("3"));
        when(parametreFiscalService.getValeur("TVA_ECHEANCE_JOUR")).thenReturn(new BigDecimal("15"));
        when(parametreFiscalService.getValeur("ECHEANCE_IRSA_JOUR")).thenReturn(new BigDecimal("15"));

        useCase = new GenererObligationsUseCase(obligationRepositoryPort, typeImpotRepositoryPort,
                contribuableRepositoryPort, calculerEcheanceReelleUseCase, parametreFiscalService);
    }

    @Test
    @DisplayName("TVA mensuelle : echeance le 15 du mois suivant (CDI 06.01.16)")
    void tvaEcheanceMoisSuivant() {
        ObligationFiscale o = useCase.execute("NIF0000001", "TVA", "2026-01");
        assertEquals(LocalDate.of(2026, 2, 15), o.getDateLimiteTheorique());
    }

    @Test
    @DisplayName("TVA decembre : echeance le 15 janvier N+1")
    void tvaDecembre() {
        ObligationFiscale o = useCase.execute("NIF0000001", "TVA", "2026-12");
        assertEquals(LocalDate.of(2027, 1, 15), o.getDateLimiteTheorique());
    }

    @Test
    @DisplayName("IS : echeance au 31 mars N+1 (CPF I-17)")
    void isEcheance() {
        ObligationFiscale o = useCase.execute("NIF0000001", "IS", "2026");
        assertEquals(LocalDate.of(2027, 3, 31), o.getDateLimiteTheorique());
    }

    @Test
    @DisplayName("IR exercice civil (date de cloture absente) : 15 mai N+1")
    void irExerciceCivil() {
        ObligationFiscale o = useCase.execute("NIF0000001", "IR", "2026");
        assertEquals(LocalDate.of(2027, 5, 15), o.getDateLimiteTheorique());
    }

    @Test
    @DisplayName("IR cloture au 31 decembre : 15 mai N+1")
    void irCloture31Decembre() {
        Contribuable c = Contribuable.builder()
                .nif("NIF0000001")
                .dateCloture(LocalDate.of(2026, 12, 31))
                .build();
        when(contribuableRepositoryPort.findByNif("NIF0000001")).thenReturn(Optional.of(c));

        ObligationFiscale o = useCase.execute("NIF0000001", "IR", "2026");
        assertEquals(LocalDate.of(2027, 5, 15), o.getDateLimiteTheorique());
    }

    @Test
    @DisplayName("IR cloture au 30 juin : 15 novembre de la meme annee (CPF I-11)")
    void irCloture30Juin() {
        Contribuable c = Contribuable.builder()
                .nif("NIF0000001")
                .dateCloture(LocalDate.of(2026, 6, 30))
                .build();
        when(contribuableRepositoryPort.findByNif("NIF0000001")).thenReturn(Optional.of(c));

        ObligationFiscale o = useCase.execute("NIF0000001", "IR", "2026");
        assertEquals(LocalDate.of(2026, 11, 15), o.getDateLimiteTheorique());
    }

    @Test
    @DisplayName("IR autre cloture (31 mars) : 15e jour du 4e mois suivant -> 15 juillet")
    void irAutreCloture() {
        Contribuable c = Contribuable.builder()
                .nif("NIF0000001")
                .dateCloture(LocalDate.of(2026, 3, 31))
                .build();
        when(contribuableRepositoryPort.findByNif("NIF0000001")).thenReturn(Optional.of(c));

        ObligationFiscale o = useCase.execute("NIF0000001", "IR", "2026");
        assertEquals(LocalDate.of(2026, 7, 15), o.getDateLimiteTheorique());
    }

    @Test
    @DisplayName("IR autre cloture (30 septembre) : 15e jour du 4e mois suivant -> 15 janvier N+1 (CPF I-11)")
    void irAutreCloture30Septembre() {
        Contribuable c = Contribuable.builder()
                .nif("NIF0000001")
                .dateCloture(LocalDate.of(2026, 9, 30))
                .build();
        when(contribuableRepositoryPort.findByNif("NIF0000001")).thenReturn(Optional.of(c));

        ObligationFiscale o = useCase.execute("NIF0000001", "IR", "2026");
        assertEquals(LocalDate.of(2027, 1, 15), o.getDateLimiteTheorique());
    }

    @Test
    @DisplayName("Les regles codees priment sur la configuration echeanceTheorique du TypeImpot")
    void reglesCodeesPrime() {
        when(typeImpotRepositoryPort.findByCodeImpot("IR")).thenReturn(Optional.of(
                TypeImpot.builder()
                        .codeImpot("IR")
                        .nomImpot("Impôt sur les revenus")
                        .periodicite(Periodicite.ANNUELLE)
                        .echeanceTheoriqueJour(1)
                        .echeanceTheoriqueMois(1)
                        .build()));

        ObligationFiscale o = useCase.execute("NIF0000001", "IR", "2026");
        assertEquals(LocalDate.of(2027, 5, 15), o.getDateLimiteTheorique());
    }

    @Test
    @DisplayName("L'identifiant du jour ferie est propage sur l'obligation")
    void idJourFeriePropage() {
        when(calculerEcheanceReelleUseCase.calculer(any(LocalDate.class)))
                .thenAnswer(invocation -> new CalculerEcheanceReelleUseCase.EcheanceReelle(
                        invocation.getArgument(0), 99L));

        ObligationFiscale o = useCase.execute("NIF0000001", "TVA", "2026-01");
        assertEquals(99L, o.getIdJourFerie());
    }

    @Test
    @DisplayName("Une obligation deja existante leve une exception")
    void obligationDejaExistante() {
        ObligationFiscale existante = ObligationFiscale.builder()
                .nif("NIF0000001")
                .codeImpot("TVA")
                .periodeFiscale("2026-01")
                .build();
        when(obligationRepositoryPort.findByNifEtPeriode("NIF0000001", "2026-01"))
                .thenReturn(List.of(existante));

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute("NIF0000001", "TVA", "2026-01"));
    }

    @Test
    @DisplayName("Un type d'impot inconnu leve une exception")
    void typeImpotInconnu() {
        when(typeImpotRepositoryPort.findByCodeImpot("INCONNU")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute("NIF0000001", "INCONNU", "2026"));
    }
}

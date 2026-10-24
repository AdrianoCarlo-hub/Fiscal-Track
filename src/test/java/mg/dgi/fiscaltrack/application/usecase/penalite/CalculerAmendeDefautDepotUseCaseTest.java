package mg.dgi.fiscaltrack.application.usecase.penalite;

import mg.dgi.fiscaltrack.application.port.out.AmendeDefautDepotRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.model.AmendeDefautDepot;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CalculerAmendeDefautDepotUseCaseTest {

    private ObligationFiscaleRepositoryPort obligationFiscaleRepositoryPort;
    private DeclarationRepositoryPort declarationRepositoryPort;
    private AmendeDefautDepotRepositoryPort amendeDefautDepotRepositoryPort;
    private ParametreFiscalService parametreFiscalService;
    private CalculerAmendeDefautDepotUseCase useCase;

    @BeforeEach
    void setUp() {
        obligationFiscaleRepositoryPort = Mockito.mock(ObligationFiscaleRepositoryPort.class);
        declarationRepositoryPort = Mockito.mock(DeclarationRepositoryPort.class);
        amendeDefautDepotRepositoryPort = Mockito.mock(AmendeDefautDepotRepositoryPort.class);
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        useCase = new CalculerAmendeDefautDepotUseCase(
                obligationFiscaleRepositoryPort,
                declarationRepositoryPort,
                amendeDefautDepotRepositoryPort,
                parametreFiscalService);

        when(parametreFiscalService.getValeur("AMENDE_SEUIL_GRANDE_ENTREPRISE"))
                .thenReturn(new BigDecimal("400000000"));
        when(parametreFiscalService.getValeur("AMENDE_SEUIL_MOYENNE_ENTREPRISE"))
                .thenReturn(new BigDecimal("200000000"));
        when(parametreFiscalService.getValeur("AMENDE_SEUIL_PETITE_ENTREPRISE"))
                .thenReturn(new BigDecimal("50000000"));
        when(parametreFiscalService.getValeur("DEFAUT_DEPOT_GRANDE_ENTREPRISE"))
                .thenReturn(new BigDecimal("200000"));
        when(parametreFiscalService.getValeur("DEFAUT_DEPOT_MOYENNE_ENTREPRISE"))
                .thenReturn(new BigDecimal("100000"));
        when(parametreFiscalService.getValeur("DEFAUT_DEPOT_PETITE_ENTREPRISE"))
                .thenReturn(new BigDecimal("50000"));
        when(parametreFiscalService.getValeur("DEFAUT_DEPOT_MICRO_ENTREPRISE"))
                .thenReturn(new BigDecimal("20000"));
    }

    private void preparer(String nif, BigDecimal chiffreAffaires) {
        ObligationFiscale obligation = ObligationFiscale.builder()
                .idObligationFiscale(1L)
                .nif(nif)
                .build();
        when(obligationFiscaleRepositoryPort.findById(1L)).thenReturn(Optional.of(obligation));
        when(amendeDefautDepotRepositoryPort.findByIdObligationFiscale(1L)).thenReturn(Optional.empty());

        if (chiffreAffaires == null) {
            when(declarationRepositoryPort.findDerniereDeclarationByNif(nif)).thenReturn(Optional.empty());
        } else {
            Declaration declaration = Declaration.builder()
                    .idDeclaration(1L)
                    .chiffreAffairesDeclare(chiffreAffaires)
                    .build();
            when(declarationRepositoryPort.findDerniereDeclarationByNif(nif)).thenReturn(Optional.of(declaration));
        }
        when(amendeDefautDepotRepositoryPort.save(any(AmendeDefautDepot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("CA >= 400M : amende de 200 000 Ar")
    void testGrandeEntreprise() {
        preparer("NIF0000001", new BigDecimal("500000000"));
        AmendeDefautDepot amende = useCase.execute(1L);
        assertEquals(0, amende.getMontant().compareTo(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("CA = 400M : seuil grande entreprise")
    void testSeuilGrande() {
        preparer("NIF0000001", new BigDecimal("400000000"));
        AmendeDefautDepot amende = useCase.execute(1L);
        assertEquals(0, amende.getMontant().compareTo(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("200M <= CA < 400M : amende de 100 000 Ar")
    void testMoyenneEntreprise() {
        preparer("NIF0000001", new BigDecimal("200000000"));
        AmendeDefautDepot amende = useCase.execute(1L);
        assertEquals(0, amende.getMontant().compareTo(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("CA = 200M : seuil moyenne entreprise")
    void testSeuilMoyenne() {
        preparer("NIF0000001", new BigDecimal("200000000"));
        AmendeDefautDepot amende = useCase.execute(1L);
        assertEquals(0, amende.getMontant().compareTo(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("50M <= CA < 200M : amende de 50 000 Ar")
    void testPetiteEntreprise() {
        preparer("NIF0000001", new BigDecimal("100000000"));
        AmendeDefautDepot amende = useCase.execute(1L);
        assertEquals(0, amende.getMontant().compareTo(new BigDecimal("50000")));
    }

    @Test
    @DisplayName("CA = 50M : seuil petite entreprise")
    void testSeuilPetite() {
        preparer("NIF0000001", new BigDecimal("50000000"));
        AmendeDefautDepot amende = useCase.execute(1L);
        assertEquals(0, amende.getMontant().compareTo(new BigDecimal("50000")));
    }

    @Test
    @DisplayName("CA < 50M : amende de 20 000 Ar")
    void testMicroEntreprise() {
        preparer("NIF0000001", new BigDecimal("10000000"));
        AmendeDefautDepot amende = useCase.execute(1L);
        assertEquals(0, amende.getMontant().compareTo(new BigDecimal("20000")));
    }

    @Test
    @DisplayName("CA null ou inexistant : amende tranche basse 20 000 Ar (A CONFIRMER)")
    void testZero() {
        preparer("NIF0000001", null);
        AmendeDefautDepot amende = useCase.execute(1L);
        assertEquals(0, amende.getMontant().compareTo(new BigDecimal("20000")));
    }

    @Test
    @DisplayName("Obligation introuvable : exception")
    void testObligationIntrouvable() {
        when(obligationFiscaleRepositoryPort.findById(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(999L));
    }
}
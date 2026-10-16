package mg.dgi.fiscaltrack.application.usecase.penalite;

import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CalculerAmendeDefautDepotUseCaseTest {

    private DeclarationRepositoryPort declarationRepositoryPort;
    private ParametreFiscalService parametreFiscalService;
    private CalculerAmendeDefautDepotUseCase useCase;

    @BeforeEach
    void setUp() {
        declarationRepositoryPort = Mockito.mock(DeclarationRepositoryPort.class);
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        useCase = new CalculerAmendeDefautDepotUseCase(declarationRepositoryPort, parametreFiscalService);

        // Mock des parametres fiscaux (codes dedies a l'amende, distincts
        // des seuils de penalite de retard)
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

    private void preparerDeclaration(BigDecimal chiffreAffaires) {
        Declaration declaration = Declaration.builder()
                .idDeclaration(1L)
                .chiffreAffairesDeclare(chiffreAffaires)
                .build();
        when(declarationRepositoryPort.findById(1L)).thenReturn(Optional.of(declaration));
    }

    @Test
    @DisplayName("CA >= 400M : amende de 200 000 Ar")
    void testGrandeEntreprise() {
        preparerDeclaration(new BigDecimal("500000000"));
        BigDecimal amende = useCase.execute(1L);
        assertEquals(0, amende.compareTo(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("CA = 400M : seuil grande entreprise, amende de 200 000 Ar")
    void testSeuilGrande() {
        preparerDeclaration(new BigDecimal("400000000"));
        BigDecimal amende = useCase.execute(1L);
        assertEquals(0, amende.compareTo(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("200M <= CA < 400M : amende de 100 000 Ar")
    void testMoyenneEntreprise() {
        preparerDeclaration(new BigDecimal("200000000"));
        BigDecimal amende = useCase.execute(1L);
        assertEquals(0, amende.compareTo(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("CA = 200M : seuil moyenne entreprise, amende de 100 000 Ar")
    void testSeuilMoyenne() {
        preparerDeclaration(new BigDecimal("200000000"));
        BigDecimal amende = useCase.execute(1L);
        assertEquals(0, amende.compareTo(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("50M <= CA < 200M : amende de 50 000 Ar")
    void testPetiteEntreprise() {
        preparerDeclaration(new BigDecimal("100000000"));
        BigDecimal amende = useCase.execute(1L);
        assertEquals(0, amende.compareTo(new BigDecimal("50000")));
    }

    @Test
    @DisplayName("CA = 50M : seuil petite entreprise, amende de 50 000 Ar")
    void testSeuilPetite() {
        preparerDeclaration(new BigDecimal("50000000"));
        BigDecimal amende = useCase.execute(1L);
        assertEquals(0, amende.compareTo(new BigDecimal("50000")));
    }

    @Test
    @DisplayName("CA < 50M : amende de 20 000 Ar")
    void testMicroEntreprise() {
        preparerDeclaration(new BigDecimal("10000000"));
        BigDecimal amende = useCase.execute(1L);
        assertEquals(0, amende.compareTo(new BigDecimal("20000")));
    }

    @Test
    @DisplayName("CA = 0 : amende de 20 000 Ar (micro entreprise)")
    void testZero() {
        preparerDeclaration(BigDecimal.ZERO);
        BigDecimal amende = useCase.execute(1L);
        assertEquals(0, amende.compareTo(new BigDecimal("20000")));
    }

    @Test
    @DisplayName("Declaration introuvable : exception")
    void testDeclarationIntrouvable() {
        when(declarationRepositoryPort.findById(999L)).thenReturn(Optional.empty());
        try {
            useCase.execute(999L);
            // Si on arrive ici, le test echoue
            assertEquals(true, false, "Doit lever une exception");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }
}

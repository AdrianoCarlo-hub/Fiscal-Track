package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.StatutValidation;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class VerifierDeclarationUseCaseTest {

    private DeclarationRepositoryPort repositoryPort;
    private EnregistrerActionUseCase enregistrerActionUseCase;
    private VerifierDeclarationUseCase useCase;

    @BeforeEach
    void setUp() {
        repositoryPort = Mockito.mock(DeclarationRepositoryPort.class);
        enregistrerActionUseCase = Mockito.mock(EnregistrerActionUseCase.class);
        useCase = new VerifierDeclarationUseCase(repositoryPort, enregistrerActionUseCase);

        when(repositoryPort.findById(1L)).thenReturn(Optional.of(Declaration.builder()
                .idDeclaration(1L)
                .idObligationFiscale(10L)
                .statutValidation(StatutValidation.EN_ATTENTE_VALIDATION)
                .build()));
        when(repositoryPort.save(Mockito.any(Declaration.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Rejet : le motif est obligatoire")
    void motifObligatoire() {
        assertThrows(IllegalArgumentException.class, () -> useCase.rejeter(1L, null));
        assertThrows(IllegalArgumentException.class, () -> useCase.rejeter(1L, "   "));
    }

    @Test
    @DisplayName("Rejet : le motif est enregistre sur la declaration")
    void motifEnregistre() {
        Declaration d = useCase.rejeter(1L, " Montant incoherent avec le calcul du serveur ");

        assertEquals(StatutValidation.REJETEE, d.getStatutValidation());
        assertEquals("Montant incoherent avec le calcul du serveur", d.getMotifRejet());
    }

    @Test
    @DisplayName("Validation : aucune demande de motif")
    void validationSansMotif() {
        Declaration d = useCase.valider(1L);

        assertEquals(StatutValidation.VALIDEE, d.getStatutValidation());
        assertTrue(d.getMotifRejet() == null || d.getMotifRejet().isBlank());
    }
}

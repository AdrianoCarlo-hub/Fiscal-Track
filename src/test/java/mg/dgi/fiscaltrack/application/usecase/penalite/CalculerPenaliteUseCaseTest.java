package mg.dgi.fiscaltrack.application.usecase.penalite;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CalculerPenaliteUseCaseTest {

    private CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private DeclarationRepositoryPort declarationRepositoryPort;
    private CalculerPenaliteUseCase useCase;

    @BeforeEach
    void setUp() {
        compteRepositoryPort = Mockito.mock(CompteCourantFiscalRepositoryPort.class);
        declarationRepositoryPort = Mockito.mock(DeclarationRepositoryPort.class);

        when(compteRepositoryPort.save(any(CompteCourantFiscal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase = new CalculerPenaliteUseCase(compteRepositoryPort, declarationRepositoryPort);
    }

    private void preparerContexte(BigDecimal chiffreAffaires, BigDecimal principal) {
        CompteCourantFiscal compte = CompteCourantFiscal.builder()
                .idCompte(1L)
                .idDeclaration(10L)
                .nif("NIF0000001")
                .montantPrincipal(principal)
                .montantPenalites(BigDecimal.ZERO)
                .montantPaye(BigDecimal.ZERO)
                .statutRecouvrement(StatutRecouvrement.NON_SOLDE)
                .build();
        when(compteRepositoryPort.findById(1L)).thenReturn(Optional.of(compte));

        Declaration declaration = Declaration.builder()
                .idDeclaration(10L)
                .chiffreAffairesDeclare(chiffreAffaires)
                .build();
        when(declarationRepositoryPort.findById(10L)).thenReturn(Optional.of(declaration));
    }

    @Test
    @DisplayName("Grande entreprise (CA >= 400M) : taux 3% par mois")
    void grandeEntreprise() {
        preparerContexte(new BigDecimal("500000000"), new BigDecimal("10000000"));
        LocalDate dateLimite = LocalDate.now().minusMonths(3);
        BigDecimal penalites = useCase.execute(1L, dateLimite);

        // 10 000 000 x 0.03 x 3 mois = 900 000
        assertEquals(0, penalites.compareTo(new BigDecimal("900000")));
    }

    @Test
    @DisplayName("Moyenne entreprise (CA entre 100M et 400M) : taux 2% par mois")
    void moyenneEntreprise() {
        preparerContexte(new BigDecimal("200000000"), new BigDecimal("10000000"));
        LocalDate dateLimite = LocalDate.now().minusMonths(2);
        BigDecimal penalites = useCase.execute(1L, dateLimite);

        // 10 000 000 x 0.02 x 2 mois = 400 000
        assertEquals(0, penalites.compareTo(new BigDecimal("400000")));
    }

    @Test
    @DisplayName("Petite entreprise (CA < 100M) : taux 1% par mois")
    void petiteEntreprise() {
        preparerContexte(new BigDecimal("50000000"), new BigDecimal("10000000"));
        LocalDate dateLimite = LocalDate.now().minusMonths(1);
        BigDecimal penalites = useCase.execute(1L, dateLimite);

        // 10 000 000 x 0.01 x 1 mois = 100 000
        assertEquals(0, penalites.compareTo(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("Pas de penalite si la date limite n'est pas depassee")
    void pasDePenalite() {
        preparerContexte(new BigDecimal("500000000"), new BigDecimal("10000000"));
        LocalDate dateFuture = LocalDate.now().plusMonths(1);
        BigDecimal penalites = useCase.execute(1L, dateFuture);

        assertEquals(0, penalites.compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Une fraction de mois entamee compte pour 1 mois complet")
    void fractionDeMois() {
        preparerContexte(new BigDecimal("500000000"), new BigDecimal("10000000"));
        LocalDate dateLimite = LocalDate.now().minusDays(15);
        BigDecimal penalites = useCase.execute(1L, dateLimite);

        // 10 000 000 x 0.03 x 1 mois = 300 000
        assertEquals(0, penalites.compareTo(new BigDecimal("300000")));
    }

    @Test
    @DisplayName("Les penalites sont arrondies a 2 decimales maximum")
    void arrondiDeuxDecimales() {
        preparerContexte(new BigDecimal("500000000"), new BigDecimal("333333"));
        LocalDate dateLimite = LocalDate.now().minusMonths(1);
        BigDecimal penalites = useCase.execute(1L, dateLimite);

        assertTrue(penalites.scale() <= 2);
    }
}

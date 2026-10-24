package mg.dgi.fiscaltrack.application.usecase.majoration;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.HistoriquePenaliteRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.HistoriquePenalite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccorderRemiseGracieuseUseCaseTest {

    private CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private HistoriquePenaliteRepositoryPort historiquePenaliteRepositoryPort;
    private EnregistrerActionUseCase enregistrerActionUseCase;
    private AccorderRemiseGracieuseUseCase useCase;

    @BeforeEach
    void setUp() {
        compteRepositoryPort = Mockito.mock(CompteCourantFiscalRepositoryPort.class);
        historiquePenaliteRepositoryPort = Mockito.mock(HistoriquePenaliteRepositoryPort.class);
        enregistrerActionUseCase = Mockito.mock(EnregistrerActionUseCase.class);
        useCase = new AccorderRemiseGracieuseUseCase(compteRepositoryPort,
                historiquePenaliteRepositoryPort, enregistrerActionUseCase);
        when(compteRepositoryPort.save(any(CompteCourantFiscal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CompteCourantFiscal compte(BigDecimal penalites, StatutRecouvrement statut) {
        CompteCourantFiscal compte = CompteCourantFiscal.builder()
                .idCompte(1L)
                .nif("NIF0000001")
                .montantPrincipal(new BigDecimal("1000000"))
                .montantPenalites(penalites)
                .montantPaye(BigDecimal.ZERO)
                .statutRecouvrement(statut)
                .build();
        when(compteRepositoryPort.findById(1L)).thenReturn(Optional.of(compte));
        return compte;
    }

    @Test
    @DisplayName("Remise gracieuse valide : reduit les penalites et trace l'audit")
    void remiseValide() {
        CompteCourantFiscal compte = compte(new BigDecimal("500000"), StatutRecouvrement.NON_SOLDE);
        BigDecimal remise = useCase.execute(1L, new BigDecimal("200000"), "Demande du contribuable", "RESP");

        assertEquals(0, remise.compareTo(new BigDecimal("200000")));
        assertEquals(0, compte.getMontantPenalites().compareTo(new BigDecimal("300000")));
        verify(historiquePenaliteRepositoryPort, times(1)).save(any(HistoriquePenalite.class));
        verify(enregistrerActionUseCase, times(1)).execute(eq("RESP"), eq("REMISE_GRACIEUSE_20_01_51"), contains("Justification"));
    }

    @Test
    @DisplayName("Justification obligatoire")
    void justificationObligatoire() {
        compte(new BigDecimal("500000"), StatutRecouvrement.NON_SOLDE);
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(1L, new BigDecimal("100000"), "  ", "RESP"));
    }

    @Test
    @DisplayName("Montant de remise strictement positif")
    void montantPositif() {
        compte(new BigDecimal("500000"), StatutRecouvrement.NON_SOLDE);
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(1L, BigDecimal.ZERO, "Motif", "RESP"));
    }

    @Test
    @DisplayName("La remise est plafonnee aux penalites restantes")
    void remisePlafonnee() {
        CompteCourantFiscal compte = compte(new BigDecimal("100000"), StatutRecouvrement.NON_SOLDE);
        BigDecimal remise = useCase.execute(1L, new BigDecimal("999999"), "Motif", "RESP");

        assertEquals(0, remise.compareTo(new BigDecimal("100000")));
        assertEquals(0, compte.getMontantPenalites().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Remise interdite sur un compte solde")
    void remiseCompteSolde() {
        compte(BigDecimal.ZERO, StatutRecouvrement.SOLDE);
        assertThrows(IllegalStateException.class,
                () -> useCase.execute(1L, new BigDecimal("100000"), "Motif", "RESP"));
    }
}

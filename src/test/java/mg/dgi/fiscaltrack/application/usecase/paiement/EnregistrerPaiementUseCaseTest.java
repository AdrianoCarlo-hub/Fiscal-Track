package mg.dgi.fiscaltrack.application.usecase.paiement;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.PaiementRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.ModePaiement;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.exception.PaiementInvalideException;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Paiement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class EnregistrerPaiementUseCaseTest {

    private PaiementRepositoryPort paiementRepositoryPort;
    private CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private EnregistrerActionUseCase enregistrerActionUseCase;
    private EnregistrerPaiementUseCase useCase;

    @BeforeEach
    void setUp() {
        paiementRepositoryPort = Mockito.mock(PaiementRepositoryPort.class);
        compteRepositoryPort = Mockito.mock(CompteCourantFiscalRepositoryPort.class);
        enregistrerActionUseCase = Mockito.mock(EnregistrerActionUseCase.class);

        when(paiementRepositoryPort.save(any(Paiement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase = new EnregistrerPaiementUseCase(
                paiementRepositoryPort, compteRepositoryPort, enregistrerActionUseCase);
    }

    private CompteCourantFiscal compteInitial(BigDecimal principal, BigDecimal paye) {
        return CompteCourantFiscal.builder()
                .idCompte(1L)
                .nif("NIF0000001")
                .montantPrincipal(principal)
                .montantPenalites(BigDecimal.ZERO)
                .montantPaye(paye)
                .resteARecouvrer(principal.subtract(paye))
                .statutRecouvrement(StatutRecouvrement.NON_SOLDE)
                .build();
    }

    @Test
    @DisplayName("Un paiement partiel passe le compte en PARTIEL")
    void paiementPartiel() {
        CompteCourantFiscal compte = compteInitial(new BigDecimal("1000000"), BigDecimal.ZERO);
        when(compteRepositoryPort.findById(1L)).thenReturn(Optional.of(compte));

        Paiement resultat = useCase.execute(1L,
                new BigDecimal("400000"), ModePaiement.ESPECES, "REF-PARTIEL");

        assertNotNull(resultat);
        assertEquals(0, compte.getMontantPaye().compareTo(new BigDecimal("400000")));
        assertEquals(StatutRecouvrement.PARTIEL, compte.getStatutRecouvrement());
    }

    @Test
    @DisplayName("Un paiement soldant totalement passe le compte en SOLDE")
    void paiementSolde() {
        CompteCourantFiscal compte = compteInitial(new BigDecimal("1000000"), BigDecimal.ZERO);
        when(compteRepositoryPort.findById(1L)).thenReturn(Optional.of(compte));

        useCase.execute(1L, new BigDecimal("1000000"),
                ModePaiement.VIREMENT, "REF-SOLDE");

        assertEquals(StatutRecouvrement.SOLDE, compte.getStatutRecouvrement());
    }

    @Test
    @DisplayName("Un montant superieur au reste a recouvrer leve PaiementInvalideException")
    void montantDepasseReste() {
        CompteCourantFiscal compte = compteInitial(new BigDecimal("1000000"), BigDecimal.ZERO);
        when(compteRepositoryPort.findById(1L)).thenReturn(Optional.of(compte));

        assertThrows(PaiementInvalideException.class, () ->
                useCase.execute(1L, new BigDecimal("2000000"),
                        ModePaiement.ESPECES, "REF-DEPASSE"));
    }

    @Test
    @DisplayName("Un montant negatif leve PaiementInvalideException")
    void montantNegatif() {
        assertThrows(PaiementInvalideException.class, () ->
                useCase.execute(1L, new BigDecimal("-100"),
                        ModePaiement.ESPECES, "REF-NEG"));
    }

    @Test
    @DisplayName("Une reference transaction deja existante leve une exception")
    void referenceDupliquee() {
        when(paiementRepositoryPort.existsByReferenceTransaction("REF-DUP")).thenReturn(true);

        assertThrows(PaiementInvalideException.class, () ->
                useCase.execute(1L, new BigDecimal("100000"),
                        ModePaiement.ESPECES, "REF-DUP"));
    }

    @Test
    @DisplayName("Un compte deja SOLDE refuse tout nouveau paiement")
    void compteDejaSolde() {
        CompteCourantFiscal compte = compteInitial(new BigDecimal("1000000"), new BigDecimal("1000000"));
        compte.setStatutRecouvrement(StatutRecouvrement.SOLDE);
        when(compteRepositoryPort.findById(1L)).thenReturn(Optional.of(compte));

        assertThrows(PaiementInvalideException.class, () ->
                useCase.execute(1L, new BigDecimal("1000"),
                        ModePaiement.ESPECES, "REF-SOLDE-2"));
    }

    @Test
    @DisplayName("Un compte inexistant leve PaiementInvalideException")
    void compteIntrouvable() {
        when(compteRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PaiementInvalideException.class, () ->
                useCase.execute(999L, new BigDecimal("100000"),
                        ModePaiement.ESPECES, "REF-INTROUVABLE"));
    }
}

package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.AmendeDefautDepotRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.exception.AccesRefuseException;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import mg.dgi.fiscaltrack.domain.model.SaisieDeclaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeposerDeclarationAcomptesTest {

    private DeclarationRepositoryPort declarationRepositoryPort;
    private ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private AmendeDefautDepotRepositoryPort amendeRepositoryPort;
    private EnregistrerActionUseCase enregistrerActionUseCase;
    private CalculerImpotDeclarationUseCase calculerImpotDeclarationUseCase;
    private PreparerDonneesDeclarationUseCase preparerDonneesDeclarationUseCase;
    private DeposerDeclarationUseCase useCase;

    private ObligationFiscale obligation;

    @BeforeEach
    void setUp() {
        declarationRepositoryPort = Mockito.mock(DeclarationRepositoryPort.class);
        obligationRepositoryPort = Mockito.mock(ObligationFiscaleRepositoryPort.class);
        compteRepositoryPort = Mockito.mock(CompteCourantFiscalRepositoryPort.class);
        amendeRepositoryPort = Mockito.mock(AmendeDefautDepotRepositoryPort.class);
        enregistrerActionUseCase = Mockito.mock(EnregistrerActionUseCase.class);
        calculerImpotDeclarationUseCase = Mockito.mock(CalculerImpotDeclarationUseCase.class);
        preparerDonneesDeclarationUseCase = Mockito.mock(PreparerDonneesDeclarationUseCase.class);
        useCase = new DeposerDeclarationUseCase(declarationRepositoryPort,
                obligationRepositoryPort, compteRepositoryPort, amendeRepositoryPort,
                enregistrerActionUseCase, calculerImpotDeclarationUseCase,
                preparerDonneesDeclarationUseCase);

        obligation = ObligationFiscale.builder()
                .idObligationFiscale(1L)
                .nif("NIF0000001")
                .codeImpot("IR")
                .periodeFiscale("2025")
                .build();
        when(obligationRepositoryPort.findById(1L)).thenReturn(Optional.of(obligation));
        when(declarationRepositoryPort.existsByIdObligationFiscale(1L)).thenReturn(false);
        when(amendeRepositoryPort.findByIdObligationFiscale(1L)).thenReturn(Optional.empty());
        when(declarationRepositoryPort.save(any(Declaration.class))).thenAnswer(invocation -> {
            Declaration declaration = invocation.getArgument(0);
            declaration.setIdDeclaration(99L);
            return declaration;
        });
        when(compteRepositoryPort.save(any(CompteCourantFiscal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private SaisieDeclaration saisie() {
        return SaisieDeclaration.builder()
                .idObligationFiscale(1L)
                .impotPrincipalCalcule(BigDecimal.ZERO)
                .build();
    }

    private void preparer(String solde, String brut, String creditAnterieur,
                          String acomptesPayes, String creditFiscal) {
        Map<String, Object> detail = Map.of(
                "codeImpot", "IR",
                "acomptesPayes", new BigDecimal(acomptesPayes),
                "autresAcomptes", BigDecimal.ZERO,
                "creditAnterieur", new BigDecimal(creditAnterieur),
                "totalDeduit", new BigDecimal(acomptesPayes).add(new BigDecimal(creditAnterieur)),
                "avertissements", java.util.List.of());
        PreparerDonneesDeclarationUseCase.DonneesDeclaration donnees =
                new PreparerDonneesDeclarationUseCase.DonneesDeclaration(
                        Map.of("caHT", new BigDecimal("100000000")),
                        new BigDecimal("100000000"),
                        Map.of(),
                        detail,
                        new BigDecimal(creditAnterieur));
        when(preparerDonneesDeclarationUseCase.execute(any(SaisieDeclaration.class), any(ObligationFiscale.class)))
                .thenReturn(donnees);
        when(calculerImpotDeclarationUseCase.executeAvecParametres(eq("IR"), anyMap(), anyString()))
                .thenReturn(Optional.of(ResultatCalculFiscal.builder()
                        .typeImpot("IR")
                        .montantBrut(new BigDecimal(brut))
                        .montantMinimum(BigDecimal.ZERO)
                        .solde(new BigDecimal(solde))
                        .creditFiscal(new BigDecimal(creditFiscal))
                        .build()));
    }

    @Test
    @DisplayName("Depot inter-contribuables : 403 avant tout calcul")
    void depotInterContribuablesRefuse() {
        AccesRefuseException e = assertThrows(AccesRefuseException.class,
                () -> useCase.deposer(saisie(), "{}", "NIF0000002"));

        assertTrue(e.getMessage().contains("Acces refuse"));
        verify(preparerDonneesDeclarationUseCase, never())
                .execute(any(SaisieDeclaration.class), any(ObligationFiscale.class));
    }

    @Test
    @DisplayName("Credit anterieur non consomme quand les acomptes payes couvrent deja l'impot")
    void creditNonConsommeSiAcomptesSuffisants() {
        preparer("0", "2000000", "1000000", "3000000", "2000000");

        Declaration declaration = useCase.deposer(saisie(), "{}", "NIF0000001");

        assertEquals(0, BigDecimal.ZERO.compareTo(declaration.getCreditUtilise()));
    }

    @Test
    @DisplayName("Credit anterieur impute a hauteur de l'impot restant apres acomptes")
    void creditImputeSurImpotRestant() {
        preparer("1000000", "5000000", "1000000", "3000000", "0");

        Declaration declaration = useCase.deposer(saisie(), "{}", "NIF0000001");

        assertEquals(0, new BigDecimal("1000000").compareTo(declaration.getCreditUtilise()));
    }

    @Test
    @DisplayName("Le credit est trace dans le detail de la declaration")
    void creditTraceDansDetail() {
        preparer("1000000", "5000000", "1000000", "3000000", "0");

        Declaration declaration = useCase.deposer(saisie(), "{}", "NIF0000001");

        assertTrue(declaration.getDetailCalcul().contains("crédit antérieur"));
        assertTrue(declaration.getDetailCalcul().contains("Acomptes déduits"));
    }
}

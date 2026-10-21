package mg.dgi.fiscaltrack.application.usecase.acompte;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcomptesObligatoiresServiceTest {

    private DeclarationRepositoryPort declarationRepositoryPort;
    private ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private ContribuableRepositoryPort contribuableRepositoryPort;
    private ParametreFiscalService parametreFiscalService;
    private AcomptesObligatoiresService service;

    @BeforeEach
    void setUp() {
        declarationRepositoryPort = Mockito.mock(DeclarationRepositoryPort.class);
        obligationRepositoryPort = Mockito.mock(ObligationFiscaleRepositoryPort.class);
        compteRepositoryPort = Mockito.mock(CompteCourantFiscalRepositoryPort.class);
        contribuableRepositoryPort = Mockito.mock(ContribuableRepositoryPort.class);
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        service = new AcomptesObligatoiresService(declarationRepositoryPort,
                obligationRepositoryPort, compteRepositoryPort,
                contribuableRepositoryPort, parametreFiscalService);

        when(parametreFiscalService.getValeur(anyString(), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("0.50"));
        when(parametreFiscalService.getValeur(eq("ACOMPTE_IR_TAUX_PAR_ACOMPTE"), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("0.25"));
        when(parametreFiscalService.getValeur(eq("ACOMPTE_IS_TAUX_PAR_ACOMPTE"), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("0.25"));
    }

    private void reference(String code, String periodeReference, String impotRetenu) {
        Declaration declaration = Declaration.builder()
                .idDeclaration(10L)
                .idObligationFiscale(100L)
                .impotPrincipalCalcule(new BigDecimal(impotRetenu))
                .build();
        when(declarationRepositoryPort.findDerniereDeclarationValideeAvantExercice(
                "NIF0000001", code, 2026)).thenReturn(Optional.of(declaration));
        when(obligationRepositoryPort.findById(100L)).thenReturn(Optional.of(
                ObligationFiscale.builder()
                        .idObligationFiscale(100L)
                        .nif("NIF0000001")
                        .codeImpot(code)
                        .periodeFiscale(periodeReference)
                        .build()));
    }

    private void forfait(String code, String montant) {
        when(parametreFiscalService.getValeur(code)).thenReturn(new BigDecimal(montant));
    }

    @Test
    @DisplayName("IR periodique : 25 % de l'IR N-1 valide (6 000 000 -> 1 500 000)")
    void irPeriodiqueSixMillions() {
        reference("IR", "2022", "6000000");

        AcomptesObligatoiresService.CalculAcompte calcul =
                service.calculerPeriodique("NIF0000001", "IR", 2026);

        assertTrue(calcul.disponible());
        assertEquals(0, new BigDecimal("1500000").compareTo(calcul.montant()));
        assertEquals(0, new BigDecimal("6000000").compareTo(calcul.base()));
        assertEquals(0, new BigDecimal("0.25").compareTo(calcul.taux()));
        assertEquals(2022, calcul.exerciceBase());
        assertEquals("Base : IR 2022 = 6000000 Ar", calcul.detail().get("baseLibelle"));
    }

    @Test
    @DisplayName("IR periodique : 25 % de 3 000 000 = 750 000")
    void irPeriodiqueTroisMillions() {
        reference("IR", "2021", "3000000");

        AcomptesObligatoiresService.CalculAcompte calcul =
                service.calculerPeriodique("NIF0000001", "ir", 2026);

        assertEquals(0, new BigDecimal("750000").compareTo(calcul.montant()));
        assertEquals(2021, calcul.exerciceBase());
    }

    @Test
    @DisplayName("IS periodique : 25 % de 1 250 000 = 312 500")
    void isPeriodique() {
        reference("IS", "2022", "1250000");

        AcomptesObligatoiresService.CalculAcompte calcul =
                service.calculerPeriodique("NIF0000001", "IS", 2026);

        assertTrue(calcul.disponible());
        assertEquals(0, new BigDecimal("312500").compareTo(calcul.montant()));
        assertEquals(0, new BigDecimal("1250000").compareTo(calcul.base()));
        assertEquals(2022, calcul.exerciceBase());
    }

    @Test
    @DisplayName("Aucune declaration validee anterieure : aucun acompte et message explicite")
    void aucunAcompteSansReference() {
        when(declarationRepositoryPort.findDerniereDeclarationValideeAvantExercice(
                "NIF0000001", "IR", 2026)).thenReturn(Optional.empty());

        AcomptesObligatoiresService.CalculAcompte calcul =
                service.calculerPeriodique("NIF0000001", "IR", 2026);

        assertFalse(calcul.disponible());
        assertEquals(0, BigDecimal.ZERO.compareTo(calcul.montant()));
        assertTrue(calcul.message().contains("Aucune declaration IR validee"));
    }

    @Test
    @DisplayName("La reference est demandee pour un exercice strictement anterieur")
    void referenceExerciceStrictementAnterieur() {
        when(declarationRepositoryPort.findDerniereDeclarationValideeAvantExercice(
                "NIF0000001", "IS", 2026)).thenReturn(Optional.empty());

        service.calculerPeriodique("NIF0000001", "IS", 2026);

        verify(declarationRepositoryPort)
                .findDerniereDeclarationValideeAvantExercice("NIF0000001", "IS", 2026);
    }

    @Test
    @DisplayName("Forfait IR debut : industrie 500 000, commerce 1 000 000, export 100 000, sante 200 000")
    void forfaitsIrDebutActivite() {
        forfait("ACOMPTE_IR_INDUSTRIE_ETC", "500000");
        forfait("ACOMPTE_IR_AUTRES", "1000000");
        forfait("ACOMPTE_IR_EXPORTATEUR_AGRICOLE", "100000");
        forfait("ACOMPTE_IR_EDUCATION_SANTE", "200000");

        assertEquals(0, new BigDecimal("500000").compareTo(
                service.calculerDebutActivite("IR", "INDUSTRIE").montant()));
        assertEquals(0, new BigDecimal("1000000").compareTo(
                service.calculerDebutActivite("IR", "COMMERCE").montant()));
        assertEquals(0, new BigDecimal("100000").compareTo(
                service.calculerDebutActivite("IR", "EXPORTATEUR_AGRICOLE").montant()));
        assertEquals(0, new BigDecimal("200000").compareTo(
                service.calculerDebutActivite("IR", "SANTE").montant()));
    }

    @Test
    @DisplayName("Forfait IR categorie inconnue : 1 000 000 et avertissement")
    void forfaitIrInconnu() {
        forfait("ACOMPTE_IR_AUTRES", "1000000");

        AcomptesObligatoiresService.CalculAcompte calcul =
                service.calculerDebutActivite("IR", "CATEGORIE_INCONNUE");

        assertEquals(0, new BigDecimal("1000000").compareTo(calcul.montant()));
        assertFalse(calcul.avertissements().isEmpty());
        assertTrue(calcul.avertissements().get(0).contains("inconnue"));
        assertEquals(Boolean.TRUE, calcul.detail().get("categorieInconnue"));
    }

    @Test
    @DisplayName("Forfait IS debut : agricole 16 000, artisan 50 000, commercant 100 000, liberal 150 000")
    void forfaitsIsDebutActivite() {
        forfait("ACOMPTE_IS_AGRICOLE_ELEVEUR", "16000");
        forfait("ACOMPTE_IS_ARTISAN", "50000");
        forfait("ACOMPTE_IS_COMMERCANT", "100000");
        forfait("ACOMPTE_IS_PROFESSION_LIBERALE", "150000");

        assertEquals(0, new BigDecimal("16000").compareTo(
                service.calculerDebutActivite("IS", "AGRICOLE").montant()));
        assertEquals(0, new BigDecimal("50000").compareTo(
                service.calculerDebutActivite("IS", "ARTISAN").montant()));
        assertEquals(0, new BigDecimal("100000").compareTo(
                service.calculerDebutActivite("IS", "COMMERCANT").montant()));
        assertEquals(0, new BigDecimal("150000").compareTo(
                service.calculerDebutActivite("IS", "PROFESSION_LIBERALE").montant()));
    }

    @Test
    @DisplayName("Forfait IS categorie inconnue : forfait le plus eleve 150 000 et avertissement")
    void forfaitIsInconnu() {
        forfait("ACOMPTE_IS_PROFESSION_LIBERALE", "150000");

        AcomptesObligatoiresService.CalculAcompte calcul =
                service.calculerDebutActivite("IS", "INCONNUE");

        assertEquals(0, new BigDecimal("150000").compareTo(calcul.montant()));
        assertFalse(calcul.avertissements().isEmpty());
        assertEquals(Boolean.TRUE, calcul.detail().get("categorieInconnue"));
    }

    @Test
    @DisplayName("Liquidation : acomptes payes 3 000 000 + credit 1 000 000 = 4 000 000 deduits")
    void liquidationCumuleAcomptesEtCredit() {
        ObligationFiscale ac1 = ObligationFiscale.builder().idObligationFiscale(51L)
                .nif("NIF0000001").codeImpot("ACOMPTE_IR").periodeFiscale("2026-AC1").build();
        ObligationFiscale ac2 = ObligationFiscale.builder().idObligationFiscale(52L)
                .nif("NIF0000001").codeImpot("ACOMPTE_IR").periodeFiscale("2026-AC2").build();
        when(obligationRepositoryPort.findByNif("NIF0000001")).thenReturn(List.of(ac1, ac2));
        when(declarationRepositoryPort.findByIdObligationFiscale(51L)).thenReturn(Optional.of(
                Declaration.builder().idDeclaration(1L).impotPrincipalCalcule(new BigDecimal("1500000")).build()));
        when(declarationRepositoryPort.findByIdObligationFiscale(52L)).thenReturn(Optional.of(
                Declaration.builder().idDeclaration(2L).impotPrincipalCalcule(new BigDecimal("1500000")).build()));
        when(compteRepositoryPort.findByIdDeclaration(1L)).thenReturn(Optional.of(
                CompteCourantFiscal.builder().montantPaye(new BigDecimal("1500000")).build()));
        when(compteRepositoryPort.findByIdDeclaration(2L)).thenReturn(Optional.of(
                CompteCourantFiscal.builder().montantPaye(new BigDecimal("1500000")).build()));
        when(compteRepositoryPort.sommeMontantPayeAcomptes("NIF0000001", "ACOMPTE_IR", 2026))
                .thenReturn(new BigDecimal("3000000"));
        when(declarationRepositoryPort.findDerniereDeclarationValideeAvantExercice(
                "NIF0000001", "IR", 2026)).thenReturn(Optional.of(
                Declaration.builder().creditFiscal(new BigDecimal("1000000")).build()));

        AcomptesObligatoiresService.Liquidation liquidation =
                service.liquider("NIF0000001", "IR", 2026, null, null);

        assertEquals(0, new BigDecimal("3000000").compareTo(liquidation.acomptesPayes()));
        assertEquals(0, new BigDecimal("1000000").compareTo(liquidation.creditAnterieur()));
        assertEquals(0, new BigDecimal("4000000").compareTo(liquidation.totalDeduit()));
        assertTrue(liquidation.avertissements().isEmpty());
        assertEquals(BigDecimal.ZERO, liquidation.autresAcomptes());
    }

    @Test
    @DisplayName("Liquidation : un acompte non depose n'est pas deduit et un avertissement est emis")
    void liquidationAcompteImpayeNonDeduit() {
        ObligationFiscale ac1 = ObligationFiscale.builder().idObligationFiscale(51L)
                .nif("NIF0000001").codeImpot("ACOMPTE_IR").periodeFiscale("2026-AC1").build();
        ObligationFiscale ac2 = ObligationFiscale.builder().idObligationFiscale(52L)
                .nif("NIF0000001").codeImpot("ACOMPTE_IR").periodeFiscale("2026-AC2").build();
        when(obligationRepositoryPort.findByNif("NIF0000001")).thenReturn(List.of(ac1, ac2));
        when(declarationRepositoryPort.findByIdObligationFiscale(51L)).thenReturn(Optional.of(
                Declaration.builder().idDeclaration(1L).impotPrincipalCalcule(new BigDecimal("1500000")).build()));
        when(compteRepositoryPort.findByIdDeclaration(1L)).thenReturn(Optional.of(
                CompteCourantFiscal.builder().montantPaye(new BigDecimal("1500000")).build()));
        when(declarationRepositoryPort.findByIdObligationFiscale(52L)).thenReturn(Optional.empty());
        when(compteRepositoryPort.sommeMontantPayeAcomptes("NIF0000001", "ACOMPTE_IR", 2026))
                .thenReturn(new BigDecimal("1500000"));

        AcomptesObligatoiresService.Liquidation liquidation =
                service.liquider("NIF0000001", "IR", 2026, null, null);

        assertEquals(0, new BigDecimal("1500000").compareTo(liquidation.totalDeduit()));
        assertEquals(1, liquidation.avertissements().size());
        assertTrue(liquidation.avertissements().get(0).contains("non déposé"));
    }

    @Test
    @DisplayName("Liquidation : douane et marches publics sont cumules")
    void liquidationAutresAcomptes() {
        when(obligationRepositoryPort.findByNif("NIF0000001")).thenReturn(List.of());
        when(compteRepositoryPort.sommeMontantPayeAcomptes("NIF0000001", "ACOMPTE_IS", 2026))
                .thenReturn(BigDecimal.ZERO);

        AcomptesObligatoiresService.Liquidation liquidation =
                service.liquider("NIF0000001", "IS", 2026,
                        new BigDecimal("200000"), new BigDecimal("300000"));

        assertEquals(0, new BigDecimal("500000").compareTo(liquidation.autresAcomptes()));
        assertEquals(0, new BigDecimal("500000").compareTo(liquidation.totalDeduit()));
        assertEquals(new BigDecimal("200000"), liquidation.detail().get("acomptesDouane"));
        assertEquals(new BigDecimal("300000"), liquidation.detail().get("acomptesMarchesPublics"));
    }

    @Test
    @DisplayName("Apercu : les colonnes de montant, base, taux et exercice sont renseignees")
    void mettreAJourApercu() {
        AcomptesObligatoiresService.CalculAcompte calcul = new AcomptesObligatoiresService.CalculAcompte(
                true, new BigDecimal("312500"), new BigDecimal("1250000"),
                new BigDecimal("0.25"), 2022, "IS", null, List.of(), Map.of());
        ObligationFiscale obligation = ObligationFiscale.builder().build();

        service.mettreAJourApercu(obligation, calcul);

        assertEquals(0, new BigDecimal("312500").compareTo(obligation.getMontantAcompte()));
        assertEquals(0, new BigDecimal("1250000").compareTo(obligation.getBaseAcompte()));
        assertEquals(0, new BigDecimal("0.25").compareTo(obligation.getTauxAcompte()));
        assertEquals(2022, obligation.getExerciceBaseAcompte());
    }
}

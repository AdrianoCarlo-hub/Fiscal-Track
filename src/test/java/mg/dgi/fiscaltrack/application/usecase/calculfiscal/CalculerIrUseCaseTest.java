package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.enums.CategorieActivite;
import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import mg.dgi.fiscaltrack.domain.model.DeficitReportable;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculerIrUseCaseTest {

    private CalculerIrUseCase useCase;
    private ParametreFiscalService parametreFiscalService;

    @BeforeEach
    void setUp() {
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        useCase = new CalculerIrUseCase(parametreFiscalService);

        // Mock des parametres fiscaux pour IR
        Mockito.when(parametreFiscalService.getValeur("IR_TAUX_NORMAL"))
                .thenReturn(new BigDecimal("0.20"));
        Mockito.when(parametreFiscalService.getValeur("IR_TAUX_EDUCATION_SANTE"))
                .thenReturn(new BigDecimal("0.10"));
        Mockito.when(parametreFiscalService.getValeur("IR_TAUX_OSBL"))
                .thenReturn(new BigDecimal("0.10"));
        Mockito.when(parametreFiscalService.getValeur("IR_TAUX_CARBURANT"))
                .thenReturn(new BigDecimal("0.002"));
        Mockito.when(parametreFiscalService.getValeur("IR_PART_FIXE_AGRICOLE"))
                .thenReturn(new BigDecimal("500000"));
        Mockito.when(parametreFiscalService.getValeur("IR_PART_FIXE_AUTRES"))
                .thenReturn(new BigDecimal("1000000"));
        Mockito.when(parametreFiscalService.getValeur("IR_PART_FIXE_EDUCATION_SANTE"))
                .thenReturn(new BigDecimal("200000"));
        Mockito.when(parametreFiscalService.getValeur("IR_TAUX_VARIABLE_AGRICOLE"))
                .thenReturn(new BigDecimal("0.01"));
        Mockito.when(parametreFiscalService.getValeur("IR_TAUX_VARIABLE_EDUCATION"))
                .thenReturn(new BigDecimal("0.002"));
        Mockito.when(parametreFiscalService.getValeur("IR_MINIMUM_SARL"))
                .thenReturn(new BigDecimal("320000"));
        Mockito.when(parametreFiscalService.getValeur("IR_ABATTEMENT_SARL"))
                .thenReturn(new BigDecimal("4200000"));
    }

    @Test
    @DisplayName("IR normal : benefice > minimum, taux 20%")
    void irNormal() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("500000000"),
                new BigDecimal("450000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                2026,
                CategorieActivite.INDUSTRIEL,
                BigDecimal.ZERO,
                12,
                false,
                false);

        // Base = 50M, IR = 50M x 20% = 10M
        // Minimum = 500 000 + 1% x 500M = 5 500 000
        assertEquals(0, r.getBaseImposable().compareTo(new BigDecimal("50000000")));
        assertEquals(0, r.getMontantBrut().compareTo(new BigDecimal("10000000")));
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("10000000")));
    }

    @Test
    @DisplayName("IR minimum : benefice < minimum, minimum retenu")
    void irMinimum() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("300000000"),
                new BigDecimal("290000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                2026,
                CategorieActivite.COMMERCE,
                BigDecimal.ZERO,
                12,
                false,
                false);

        // Base = 10M, IR theorique = 2M
        // Minimum commerce = 1 000 000 + 1% x 300M = 4 000 000
        // IR du = MAX(2M, 4M) = 4M
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("4000000")));
    }

    @Test
    @DisplayName("IR education avec autorisation d'ouverture : taux 10% et minimum reduit")
    void irEducationAvecAutorisation() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("500000000"),
                new BigDecimal("400000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                2026,
                CategorieActivite.EDUCATION,
                BigDecimal.ZERO,
                12,
                true,
                false);

        // Base = 100M, IR = 100M x 10% = 10M
        // Minimum = 200 000 + 0,2% x 500M = 1 200 000
        assertEquals(0, r.getTauxApplique().compareTo(new BigDecimal("0.10")));
        assertEquals(0, r.getMontantBrut().compareTo(new BigDecimal("10000000")));
    }

    @Test
    @DisplayName("IR education SANS autorisation : regime general (20% et minimum AUTRES)")
    void irEducationSansAutorisation() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("500000000"),
                new BigDecimal("400000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                2026,
                CategorieActivite.EDUCATION,
                BigDecimal.ZERO,
                12,
                false,
                false);

        // Sans autorisation : taux 20% (regime general)
        assertEquals(0, r.getTauxApplique().compareTo(new BigDecimal("0.20")));
        assertEquals(0, r.getMontantBrut().compareTo(new BigDecimal("20000000")));
    }

    @Test
    @DisplayName("IR sante : 10% seulement si autorisation ET option sante")
    void irSanteOption() {
        // Autorisation seule : pas d'option -> regime general 20%
        ResultatCalculFiscal sansOption = useCase.execute(
                new BigDecimal("500000000"), new BigDecimal("400000000"), BigDecimal.ZERO,
                BigDecimal.ZERO, List.of(), 2026, CategorieActivite.SANTE, BigDecimal.ZERO,
                12, true, false);
        assertEquals(0, sansOption.getTauxApplique().compareTo(new BigDecimal("0.20")));

        // Autorisation + option -> 10%
        ResultatCalculFiscal avecOption = useCase.execute(
                new BigDecimal("500000000"), new BigDecimal("400000000"), BigDecimal.ZERO,
                BigDecimal.ZERO, List.of(), 2026, CategorieActivite.SANTE, BigDecimal.ZERO,
                12, true, true);
        assertEquals(0, avecOption.getTauxApplique().compareTo(new BigDecimal("0.10")));
    }

    @Test
    @DisplayName("IR avec credit : acomptes > IR du")
    void irAvecCredit() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("300000000"),
                new BigDecimal("280000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                2026,
                CategorieActivite.INDUSTRIEL,
                new BigDecimal("30000000"),
                6,
                false,
                false);

        // Base = 20M, IR = 4M, Minimum proratise = 500k x 6/12 + 1% x 300M = 3,25M
        // IR du = 4M. Solde = 4M - 30M = -26M -> credit 26M
        assertEquals(0, r.getCreditFiscal().compareTo(new BigDecimal("26000000")));
        assertEquals(0, r.getSolde().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("IR carburant au detail : 20% sur le benefice, minimum 2 pour mille du CA")
    void irCarburantDetail() {
        // Benefice impose a 20% (et non 0,2% du benefice)
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("100000000"),
                new BigDecimal("40000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                2026,
                CategorieActivite.CARBURANT_DETAIL,
                BigDecimal.ZERO,
                12,
                false,
                false);

        assertEquals(0, r.getTauxApplique().compareTo(new BigDecimal("0.20")));
        // Base = 60M, IR = 12M ; minimum = 2 pour mille x 100M = 200 000
        assertEquals(0, r.getMontantBrut().compareTo(new BigDecimal("12000000")));
        assertEquals(0, r.getMontantMinimum().compareTo(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("IR carburant : benefice eleve, 20% du benefice l'emporte sur le minimum 2 pour mille")
    void irCarburantBeneficeEleveVingtPourCentLEmporte() {
        // CA 100M, charges 20M -> benefice 80M arrondi au millier = 80M
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("100000000"),
                new BigDecimal("20000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                2026,
                CategorieActivite.CARBURANT_DETAIL,
                BigDecimal.ZERO,
                12,
                false,
                false);

        // IR theo = 80M x 20% = 16M ; minimum = 0,002 x 100M = 200 000
        // IR du = MAX(16M ; 200 000) = 16M (20% l'emporte)
        assertEquals(0, r.getTauxApplique().compareTo(new BigDecimal("0.20")));
        assertEquals(0, r.getMontantBrut().compareTo(new BigDecimal("16000000")));
        assertEquals(0, r.getMontantMinimum().compareTo(new BigDecimal("200000")));
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("16000000")));
    }

    @Test
    @DisplayName("IR carburant : benefice faible, le minimum 2 pour mille du CA l'emporte")
    void irCarburantBeneficeFaibleMinimumDeuxPourMilleLEmporte() {
        // CA 100M, charges 99,5M -> benefice 500 000
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("100000000"),
                new BigDecimal("99500000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                2026,
                CategorieActivite.CARBURANT_DETAIL,
                BigDecimal.ZERO,
                12,
                false,
                false);

        // IR theo = 500 000 x 20% = 100 000 ; minimum = 0,002 x 100M = 200 000
        // IR du = MAX(100 000 ; 200 000) = 200 000 (le 2 pour mille l'emporte,
        // sans jamais etre applique comme taux sur le benefice)
        assertEquals(0, r.getTauxApplique().compareTo(new BigDecimal("0.20")));
        assertEquals(0, r.getMontantBrut().compareTo(new BigDecimal("100000")));
        assertEquals(0, r.getMontantMinimum().compareTo(new BigDecimal("200000")));
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("IR : le minimum de perception reste du en cas de deficit")
    void irMinimumEnCasDeDeficit() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("100000000"),
                new BigDecimal("150000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                2026,
                CategorieActivite.COMMERCE,
                BigDecimal.ZERO,
                12,
                false,
                false);

        // Base nulle (aucun benefice) ; minimum AUTRES = 1 000 000 + 1% x 100M = 2 000 000
        assertEquals(0, r.getBaseImposable().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("2000000")));
    }

    @Test
    @DisplayName("IR OSBL : taux reduit 10%")
    void irOsbl() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("500000000"),
                new BigDecimal("400000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                2026,
                CategorieActivite.OSBL,
                BigDecimal.ZERO,
                12,
                false,
                false);

        assertEquals(0, r.getTauxApplique().compareTo(new BigDecimal("0.10")));
        assertEquals(0, r.getMontantBrut().compareTo(new BigDecimal("10000000")));
    }

    @Test
    @DisplayName("IR associe-gerant SARL : abattement 4,2M puis minimum 320 000")
    void irAssocieGerantSarl() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("100000000"),
                new BigDecimal("90000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                2026,
                CategorieActivite.ASSOCIE_GERANT_SARL,
                BigDecimal.ZERO,
                12,
                false,
                false);

        // Base = 10M - 4,2M = 5,8M ; IR = 5,8M x 20% = 1 160 000 (min 320 000)
        assertEquals(0, r.getBaseImposable().compareTo(new BigDecimal("5800000")));
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("1160000")));
    }

    @Test
    @DisplayName("IR associe-gerant SARL : base epuisee -> minimum 320 000")
    void irAssocieGerantSarlMinimum() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("100000000"),
                new BigDecimal("100000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                2026,
                CategorieActivite.ASSOCIE_GERANT_SARL,
                BigDecimal.ZERO,
                12,
                false,
                false);

        assertEquals(0, r.getBaseImposable().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("320000")));
    }

    @Test
    @DisplayName("IR deficits : imputation des plus anciens d'abord")
    void irDeficitsPlusAnciensDAbord() {
        // Deux deficits valides : 2024 (30M) et 2022 (10M). Le plus ancien (2022)
        // est impute d'abord, puis 2024.
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("200000000"),
                new BigDecimal("150000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(new DeficitReportable(2024, new BigDecimal("30000000")),
                        new DeficitReportable(2022, new BigDecimal("10000000"))),
                2026,
                CategorieActivite.COMMERCE,
                BigDecimal.ZERO,
                12,
                false,
                false);

        // Base avant deficits = 50M ; -10M (2022) ; -30M (2024) = 10M
        assertEquals(0, r.getBaseImposable().compareTo(new BigDecimal("10000000")));
    }

    @Test
    @DisplayName("IR deficits : deficit de plus de 5 ans ignore et signale")
    void irDeficitTropAncienIgnore() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("200000000"),
                new BigDecimal("150000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(new DeficitReportable(2020, new BigDecimal("30000000"))),
                2026,
                CategorieActivite.COMMERCE,
                BigDecimal.ZERO,
                12,
                false,
                false);

        // 2026 - 2020 = 6 > 5 -> ignore ; base = 50M
        assertEquals(0, r.getBaseImposable().compareTo(new BigDecimal("50000000")));
        assertFalse(r.getAvertissements().isEmpty());
    }

    @Test
    @DisplayName("IR deficits : base epuisee, reliquat signale mais non impute")
    void irDeficitBaseEpuisee() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("100000000"),
                new BigDecimal("90000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(new DeficitReportable(2024, new BigDecimal("30000000")),
                        new DeficitReportable(2025, new BigDecimal("5000000"))),
                2026,
                CategorieActivite.COMMERCE,
                BigDecimal.ZERO,
                12,
                false,
                false);

        // Base = 10M ; deficit 2024 (30M) impute a hauteur de 10M ; 2025 non impute
        assertEquals(0, r.getBaseImposable().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("IR CA negatif : exception")
    void irCaNegatif() {
        assertThrows(CalculFiscalInvalideException.class, () ->
                useCase.execute(new BigDecimal("-1000"), BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, List.of(), 2026, CategorieActivite.AUTRE,
                        BigDecimal.ZERO, 12, false, false));
    }
}

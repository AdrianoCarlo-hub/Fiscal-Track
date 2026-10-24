package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import mg.dgi.fiscaltrack.domain.model.SaisieDeclaration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Formulaires adaptes a chaque impot : la validation des champs et la
 * construction des parametres sont testees ici, le calcul restant assure
 * par le dispatcher (test separe).
 */
class PreparerDonneesDeclarationUseCaseTest {

    private final ContribuableRepositoryPort contribuableRepositoryPort =
            Mockito.mock(ContribuableRepositoryPort.class);
    private final ParametreFiscalService parametreFiscalService =
            Mockito.mock(ParametreFiscalService.class);
    private final PreparerDonneesDeclarationUseCase useCase =
            new PreparerDonneesDeclarationUseCase(contribuableRepositoryPort, parametreFiscalService);

    /** Stub du dossier contribuable (le code impot n'intervient pas ici). */
    private void base(String code) {
        when(contribuableRepositoryPort.findByNif(Mockito.anyString())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("IS : les champs du formulaire sont transmis au dispatcher")
    void isTransmetLesChamps() {
        base("IS");
        PreparerDonneesDeclarationUseCase.DonneesDeclaration d = useCase.execute(
                SaisieDeclaration.builder()
                        .caBrut(new BigDecimal("30000000"))
                        .adherentCga(true)
                        .achatsConformes(new BigDecimal("1000000"))
                        .chargesSalariales(new BigDecimal("2000000"))
                        .acomptes(new BigDecimal("0"))
                        .build(),
                "IS", "NIF0000001");

        assertEquals(0, d.chiffreAffaires().compareTo(new BigDecimal("30000000")));
        assertEquals(new BigDecimal("30000000"), d.params().get("caBrut"));
        assertEquals(Boolean.TRUE, d.params().get("adherentCga"));
        assertEquals(new BigDecimal("1000000"), d.params().get("achatsConformes"));
        assertEquals(new BigDecimal("2000000"), d.params().get("chargesSalariales"));
    }

    @Test
    @DisplayName("IS : chiffre d'affaires absent -> message en francais")
    void isSansChiffreAffaires() {
        base("IS");
        CalculFiscalInvalideException e = assertThrows(CalculFiscalInvalideException.class,
                () -> useCase.execute(SaisieDeclaration.builder().build(), "IS", "NIF0000001"));
        assertTrue(e.getMessage().contains("chiffre d'affaires brut encaisse"));
    }

    @Test
    @DisplayName("TVA : TVA collectee calculee avec le taux, deductible et export transmis")
    void tvaCalculeTvaCollectee() {
        base("TVA");
        when(parametreFiscalService.getValeur(anyString())).thenReturn(new BigDecimal("0.2"));

        PreparerDonneesDeclarationUseCase.DonneesDeclaration d = useCase.execute(
                SaisieDeclaration.builder()
                        .caLocal(new BigDecimal("50000000"))
                        .caExportation(new BigDecimal("20000000"))
                        .tvaDeductibleBiens(new BigDecimal("4000000"))
                        .build(),
                "TVA", "NIF0000001");

        assertEquals(0, new BigDecimal("10000000").compareTo((BigDecimal) d.params().get("tvaCollectee")));
        assertEquals(0, new BigDecimal("4000000").compareTo((BigDecimal) d.params().get("tvaDeductible")));
        assertEquals(Boolean.FALSE, d.params().get("exportation"));
        assertEquals(0, d.chiffreAffaires().compareTo(new BigDecimal("70000000")));
    }

    @Test
    @DisplayName("TVA : export uniquement -> pas de TVA collectee, drapeau exportation")
    void tvaExportUniquement() {
        base("TVA");
        when(parametreFiscalService.getValeur(anyString())).thenReturn(BigDecimal.ZERO);

        PreparerDonneesDeclarationUseCase.DonneesDeclaration d = useCase.execute(
                SaisieDeclaration.builder()
                        .caLocal(BigDecimal.ZERO)
                        .caExportation(new BigDecimal("20000000"))
                        .tvaDeductibleBiens(new BigDecimal("5000000"))
                        .build(),
                "TVA", "NIF0000001");

        assertEquals(Boolean.TRUE, d.params().get("exportation"));
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) d.params().get("tvaCollectee")));
    }

    @Test
    @DisplayName("IRSA : les trois avantages en nature sont transmis un a un")
    void irsaTroisAvantagesSepares() {
        base("IRSA");

        PreparerDonneesDeclarationUseCase.DonneesDeclaration d = useCase.execute(
                SaisieDeclaration.builder()
                        .salaries(List.of(new SaisieDeclaration.LigneSalarie(
                                "Rabe", new BigDecimal("500000"), BigDecimal.ZERO,
                                new BigDecimal("100000"), new BigDecimal("200000"),
                                new BigDecimal("30000"), 1)))
                        .build(),
                "IRSA", "NIF0000001");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lignes = (List<Map<String, Object>>) d.params().get("salaries");
        assertEquals(1, lignes.size());
        Map<String, Object> ligne = lignes.get(0);
        assertEquals(0, new BigDecimal("100000").compareTo((BigDecimal) ligne.get("avantageLogement")));
        assertEquals(0, new BigDecimal("200000").compareTo((BigDecimal) ligne.get("avantageVehicule")));
        assertEquals(0, new BigDecimal("30000").compareTo((BigDecimal) ligne.get("avantageTelephone")));
        assertEquals(1, ligne.get("personnesCharge"));
        @SuppressWarnings("unchecked")
        Map<String, Object> parametresLigne = (Map<String, Object>) ligne.get("parametres");
        assertEquals(1, parametresLigne.get("nombrePersonnesCharge"));
        assertEquals(0, d.chiffreAffaires().compareTo(new BigDecimal("500000")));
    }

    @Test
    @DisplayName("IRSA : salaire brut absent -> message en francais")
    void irsaSansSalaire() {
        base("IRSA");
        CalculFiscalInvalideException e = assertThrows(CalculFiscalInvalideException.class,
                () -> useCase.execute(SaisieDeclaration.builder()
                        .salaries(List.of(new SaisieDeclaration.LigneSalarie(
                                "Rabe", null, null, null, null, null, null)))
                        .build(), "IRSA", "NIF0000001"));
        assertTrue(e.getMessage().contains("Salaire brut mensuel obligatoire"));
    }

    @Test
    @DisplayName("IRSA : plus de 50 salaries -> refus")
    void irsaTropDeSalaries() {
        base("IRSA");
        List<SaisieDeclaration.LigneSalarie> lignes = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            lignes.add(new SaisieDeclaration.LigneSalarie("S" + i, BigDecimal.ONE,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0));
        }
        CalculFiscalInvalideException e = assertThrows(CalculFiscalInvalideException.class,
                () -> useCase.execute(SaisieDeclaration.builder().salaries(lignes).build(),
                        "IRSA", "NIF0000001"));
        assertTrue(e.getMessage().contains("Au plus 50 salaries"));
    }

    @Test
    @DisplayName("IR : chiffre d'affaires HT inférieur à 400 000 000 Ar refusé")
    void irCaSousLeSeuil() {
        base("IR");
        CalculFiscalInvalideException e = assertThrows(CalculFiscalInvalideException.class,
                () -> useCase.execute(SaisieDeclaration.builder()
                        .caHT(new BigDecimal("399999999"))
                        .build(), "IR", "NIF0000001"));
        assertTrue(e.getMessage().contains("400 000 000"));
    }

    @Test
    @DisplayName("IR : chiffre d'affaires HT obligatoire")
    void irSansCaHt() {
        base("IR");
        CalculFiscalInvalideException e = assertThrows(CalculFiscalInvalideException.class,
                () -> useCase.execute(SaisieDeclaration.builder().build(), "IR", "NIF0000001"));
        assertTrue(e.getMessage().contains("chiffre d'affaires HT"));
    }

    @Test
    @DisplayName("Type sans formulaire -> message explicite, jamais de reponse vide")
    void typeSansFormulaire() {
        base("IPVI");
        CalculFiscalInvalideException e = assertThrows(CalculFiscalInvalideException.class,
                () -> useCase.execute(SaisieDeclaration.builder().build(), "IPVI", "NIF0000001"));
        assertTrue(e.getMessage().contains("Formulaire de declaration non disponible"));
    }

    @Test
    @DisplayName("Acompte IR : aucun champ saisi, la source est tracee")
    void acompteIrsansChamp() {
        base("ACOMPTE_IR");
        PreparerDonneesDeclarationUseCase.DonneesDeclaration d = useCase.execute(
                SaisieDeclaration.builder().build(), "ACOMPTE_IR", "NIF0000001");
        assertTrue(d.params().isEmpty());
        assertEquals("Derniere declaration IR du dossier (IR N-1)",
                d.donneesSaisies().get("source"));
    }
}

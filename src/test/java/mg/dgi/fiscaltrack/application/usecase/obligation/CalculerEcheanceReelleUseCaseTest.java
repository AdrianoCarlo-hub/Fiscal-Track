package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.JourFerieRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.JourFerie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CalculerEcheanceReelleUseCaseTest {

    private JourFerieRepositoryPort jourFerieRepositoryPort;
    private CalculerEcheanceReelleUseCase useCase;

    @BeforeEach
    void setUp() {
        jourFerieRepositoryPort = Mockito.mock(JourFerieRepositoryPort.class);
        when(jourFerieRepositoryPort.existsByDate(any(LocalDate.class))).thenReturn(false);
        useCase = new CalculerEcheanceReelleUseCase(jourFerieRepositoryPort);
    }

    @Test
    @DisplayName("Un jour ouvrable normal reste inchange")
    void jourOuvrableNormal() {
        LocalDate mardi = LocalDate.of(2026, 9, 22);
        assertEquals(mardi, useCase.execute(mardi));
    }

    @Test
    @DisplayName("Un samedi est reporte au lundi suivant")
    void samediReporteAuLundi() {
        LocalDate samedi = LocalDate.of(2026, 9, 19);
        LocalDate lundi = LocalDate.of(2026, 9, 21);
        assertEquals(lundi, useCase.execute(samedi));
    }

    @Test
    @DisplayName("Un dimanche est reporte au lundi suivant")
    void dimancheReporteAuLundi() {
        LocalDate dimanche = LocalDate.of(2026, 9, 20);
        LocalDate lundi = LocalDate.of(2026, 9, 21);
        assertEquals(lundi, useCase.execute(dimanche));
    }

    @Test
    @DisplayName("Un jour ferie en semaine est reporte au lendemain ouvrable")
    void jourFerieEnSemaine() {
        LocalDate vendrediFerie = LocalDate.of(2026, 5, 1);
        LocalDate lundi = LocalDate.of(2026, 5, 4);
        when(jourFerieRepositoryPort.existsByDate(vendrediFerie)).thenReturn(true);
        assertEquals(lundi, useCase.execute(vendrediFerie));
    }

    @Test
    @DisplayName("Deux jours feries consecutifs sont geres par la boucle")
    void deuxJoursFeriesConsecutifs() {
        LocalDate lundi = LocalDate.of(2026, 6, 1);
        LocalDate mardi = LocalDate.of(2026, 6, 2);
        when(jourFerieRepositoryPort.existsByDate(lundi)).thenReturn(true);
        when(jourFerieRepositoryPort.existsByDate(mardi)).thenReturn(true);
        LocalDate mercredi = LocalDate.of(2026, 6, 3);
        assertEquals(mercredi, useCase.execute(lundi));
    }

    @Test
    @DisplayName("Une date null leve une IllegalArgumentException")
    void dateNullLeveException() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
    }

    @Test
    @DisplayName("calculer rapporte l'identifiant du jour ferie responsable du report")
    void calculerRapporteIdJourFerie() {
        LocalDate ferie = LocalDate.of(2026, 5, 1);
        LocalDate lundi = LocalDate.of(2026, 5, 4);
        when(jourFerieRepositoryPort.existsByDate(ferie)).thenReturn(true);
        when(jourFerieRepositoryPort.findByDate(ferie))
                .thenReturn(Optional.of(JourFerie.builder().idJourFerie(42L).dateFerie(ferie).build()));

        CalculerEcheanceReelleUseCase.EcheanceReelle echeance = useCase.calculer(ferie);

        assertEquals(lundi, echeance.date());
        assertEquals(42L, echeance.idJourFerie());
    }

    @Test
    @DisplayName("calculer ne rapporte aucun jour ferie si seul un week-end decale la date")
    void calculerSansJourFerieIdNull() {
        LocalDate samedi = LocalDate.of(2026, 9, 19);
        LocalDate lundi = LocalDate.of(2026, 9, 21);

        CalculerEcheanceReelleUseCase.EcheanceReelle echeance = useCase.calculer(samedi);

        assertEquals(lundi, echeance.date());
        assertNull(echeance.idJourFerie());
    }
}

package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.StatutActivite;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenererObligationsAcomptesUseCaseTest {

    private ContribuableRepositoryPort contribuableRepositoryPort;
    private GenererObligationsUseCase genererObligationsUseCase;
    private GenererObligationsAcomptesUseCase useCase;

    @BeforeEach
    void setUp() {
        contribuableRepositoryPort = Mockito.mock(ContribuableRepositoryPort.class);
        genererObligationsUseCase = Mockito.mock(GenererObligationsUseCase.class);
        useCase = new GenererObligationsAcomptesUseCase(contribuableRepositoryPort, genererObligationsUseCase);
    }

    private Contribuable contribuable(String nif, StatutActivite statut) {
        return Contribuable.builder().nif(nif).statutActivite(statut).build();
    }

    @Test
    @DisplayName("Genere AC1 et AC2 pour chaque contribuable actif")
    void genereDeuxAcomptesParActif() {
        when(contribuableRepositoryPort.findAll())
                .thenReturn(List.of(contribuable("NIF0000001", StatutActivite.ACTIF)));

        int compteur = useCase.execute(2026);

        assertEquals(2, compteur);
        verify(genererObligationsUseCase).execute("NIF0000001", "ACOMPTE_IR", "2026-AC1");
        verify(genererObligationsUseCase).execute("NIF0000001", "ACOMPTE_IR", "2026-AC2");
    }

    @Test
    @DisplayName("Ignore les contribuables non actifs")
    void ignoreNonActifs() {
        when(contribuableRepositoryPort.findAll())
                .thenReturn(List.of(contribuable("NIF0000002", StatutActivite.VEILLEUSE)));

        int compteur = useCase.execute(2026);

        assertEquals(0, compteur);
        verify(genererObligationsUseCase, times(0)).execute(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Idempotent : les doublons existants n'empechent pas le traitement")
    void idempotent() {
        when(contribuableRepositoryPort.findAll())
                .thenReturn(List.of(contribuable("NIF0000001", StatutActivite.ACTIF)));
        when(genererObligationsUseCase.execute(eq("NIF0000001"), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Une obligation existe deja"));

        int compteur = useCase.execute(2026);

        assertEquals(0, compteur);
    }
}

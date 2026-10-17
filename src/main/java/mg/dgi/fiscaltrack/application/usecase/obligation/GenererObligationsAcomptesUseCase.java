package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.StatutActivite;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Generation des obligations d'acompte provisionnel IR (point 2.4).
 *
 * Periode fiscale "AAAA-AC1" et "AAAA-AC2" ; echeances 31 mars et 30 juin
 * (report au jour ouvre via {@link GenererObligationsUseCase}).
 * Chaque acompte vaut 25 % de l'IR N-1, calcule au moment de la declaration
 * (voir CalculerImpotDeclarationUseCase / CalculerAcompteUseCase).
 *
 * Idempotent : les obligations deja existantes ne sont pas recreees.
 */
@Service
public class GenererObligationsAcomptesUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GenererObligationsAcomptesUseCase.class);

    private static final String CODE_ACOMPTE_IR = "ACOMPTE_IR";

    private final ContribuableRepositoryPort contribuableRepositoryPort;
    private final GenererObligationsUseCase genererObligationsUseCase;

    public GenererObligationsAcomptesUseCase(ContribuableRepositoryPort contribuableRepositoryPort,
                                             GenererObligationsUseCase genererObligationsUseCase) {
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.genererObligationsUseCase = genererObligationsUseCase;
    }

    /**
     * Genere les deux acomptes de l'annee pour tous les contribuables actifs.
     *
     * @param annee annee de l'acompte (AAAA)
     * @return le nombre d'obligations effectivement creees
     */
    public int execute(int annee) {
        List<Contribuable> contribuables = contribuableRepositoryPort.findAll().stream()
                .filter(c -> c.getStatutActivite() == StatutActivite.ACTIF)
                .toList();

        int compteur = 0;
        for (Contribuable contribuable : contribuables) {
            for (String periode : List.of(annee + "-AC1", annee + "-AC2")) {
                try {
                    genererObligationsUseCase.execute(contribuable.getNif(), CODE_ACOMPTE_IR, periode);
                    compteur++;
                } catch (IllegalArgumentException e) {
                    // Doublon deja existant : ignore silencieusement
                } catch (Exception e) {
                    logger.error("[ACOMPTE] Erreur generation {}/{} : {}",
                            contribuable.getNif(), periode, e.getMessage());
                }
            }
        }
        return compteur;
    }

    public int executePourAnneeCourante() {
        return execute(LocalDate.now().getYear());
    }
}

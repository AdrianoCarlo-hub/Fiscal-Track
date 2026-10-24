package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.acompte.AcomptesObligatoiresService;
import mg.dgi.fiscaltrack.domain.enums.FormeJuridique;
import mg.dgi.fiscaltrack.domain.enums.StatutActivite;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AcomptesObligatoiresService acomptesService;
    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;

    /** Constructeur historique (tests existants), sans controle de reference. */
    public GenererObligationsAcomptesUseCase(ContribuableRepositoryPort contribuableRepositoryPort,
                                             GenererObligationsUseCase genererObligationsUseCase) {
        this(contribuableRepositoryPort, genererObligationsUseCase, null, null);
    }

    @Autowired
    public GenererObligationsAcomptesUseCase(ContribuableRepositoryPort contribuableRepositoryPort,
                                             GenererObligationsUseCase genererObligationsUseCase,
                                             AcomptesObligatoiresService acomptesService,
                                             ObligationFiscaleRepositoryPort obligationRepositoryPort) {
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.genererObligationsUseCase = genererObligationsUseCase;
        this.acomptesService = acomptesService;
        this.obligationRepositoryPort = obligationRepositoryPort;
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

    /**
     * Generation obligatoire : seules les obligations dont la base est
     * disponible sont creees (IR et IS), avec l'apercu du montant.
     * Sans declaration validee anterieure, aucun acompte periodique n'est
     * genere et un message explicite est journalise.
     */
    public int executeObligatoires(int annee) {
        if (acomptesService == null) {
            return execute(annee);
        }
        List<Contribuable> contribuables = contribuableRepositoryPort.findAll().stream()
                .filter(c -> c.getStatutActivite() == StatutActivite.ACTIF)
                .toList();

        int compteur = 0;
        for (Contribuable contribuable : contribuables) {
            for (String parent : List.of("IR", "IS")) {
                AcomptesObligatoiresService.CalculAcompte calcul =
                        acomptesService.calculerPeriodique(contribuable.getNif(), parent, annee);
                if (!calcul.disponible()) {
                    logger.info("[ACOMPTE] {} : {}", contribuable.getNif(), calcul.message());
                    continue;
                }
                String code = "ACOMPTE_" + parent;
                for (String periode : List.of(annee + "-AC1", annee + "-AC2")) {
                    try {
                        ObligationFiscale obligation = genererObligationsUseCase
                                .execute(contribuable.getNif(), code, periode);
                        acomptesService.mettreAJourApercu(obligation, calcul);
                        obligationRepositoryPort.save(obligation);
                        compteur++;
                    } catch (IllegalArgumentException e) {
                        // Doublon deja existant : ignore silencieusement
                    } catch (Exception e) {
                        logger.error("[ACOMPTE] Erreur generation {}/{} : {}",
                                contribuable.getNif(), periode, e.getMessage());
                    }
                }
            }
        }
        return compteur;
    }

    public int executeObligatoiresPourAnneeCourante() {
        return executeObligatoires(LocalDate.now().getYear());
    }

    /**
     * Genere les acomptes forfaitaires de debut d'activite (un seul acompte)
     * selon la forme juridique : societes -> IS, autres -> IR.
     */
    public void genererDebutActivite(Contribuable contribuable) {
        if (acomptesService == null || contribuable == null || contribuable.getNif() == null) {
            return;
        }
        String parent = estSociete(contribuable.getFormeJuridique()) ? "IS" : "IR";
        AcomptesObligatoiresService.CalculAcompte calcul =
                acomptesService.calculerDebutActivite(parent, contribuable.getCategorieActivite());
        int annee = LocalDate.now().getYear();
        try {
            ObligationFiscale obligation = genererObligationsUseCase.execute(
                    contribuable.getNif(), "ACOMPTE_" + parent, annee + "-DEBUT");
            acomptesService.mettreAJourApercu(obligation, calcul);
            obligationRepositoryPort.save(obligation);
        } catch (IllegalArgumentException e) {
            logger.info("[ACOMPTE] Debut d'activite deja genere pour {} : {}",
                    contribuable.getNif(), e.getMessage());
        }
    }

    private static boolean estSociete(FormeJuridique forme) {
        if (forme == null) {
            return false;
        }
        return switch (forme) {
            case SARL, SARLU, SA, SNC, SCS, SUCCURSALE, ZEF -> true;
            default -> false;
        };
    }
}

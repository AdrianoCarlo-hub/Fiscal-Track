package mg.dgi.fiscaltrack.application.usecase.penalite;

import mg.dgi.fiscaltrack.application.port.out.AmendeDefautDepotRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.model.AmendeDefautDepot;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Calcul et enregistrement de l'amende pour defaut de depot (CDI 20.01.52),
 * distincte de la penalite de retard (CDI 20.01.53).
 *
 * CDI 20.01.52 :
 *   - CA >= 400M : 200 000 Ar
 *   - 200M <= CA < 400M : 100 000 Ar
 *   - 50M <= CA < 200M : 50 000 Ar
 *   - CA < 50M : 20 000 Ar
 *
 * Le CA de reference est celui de la derniere declaration deposee par le
 * NIF (aucune declaration ne peut exister pour l'obligation en defaut).
 * En l'absence d'historique, la tranche la plus basse est retenue
 * (A CONFIRMER, voir QUESTIONS_ENCADREUR.md).
 *
 * Idempotence : une seule amende par obligation (contrainte d'unicite en
 * base + verification ici) ; un second appel ne cree rien.
 */
@Service
public class CalculerAmendeDefautDepotUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CalculerAmendeDefautDepotUseCase.class);

    private final ObligationFiscaleRepositoryPort obligationFiscaleRepositoryPort;
    private final DeclarationRepositoryPort declarationRepositoryPort;
    private final AmendeDefautDepotRepositoryPort amendeDefautDepotRepositoryPort;
    private final ParametreFiscalService parametreFiscalService;

    public CalculerAmendeDefautDepotUseCase(ObligationFiscaleRepositoryPort obligationFiscaleRepositoryPort,
                                            DeclarationRepositoryPort declarationRepositoryPort,
                                            AmendeDefautDepotRepositoryPort amendeDefautDepotRepositoryPort,
                                            ParametreFiscalService parametreFiscalService) {
        this.obligationFiscaleRepositoryPort = obligationFiscaleRepositoryPort;
        this.declarationRepositoryPort = declarationRepositoryPort;
        this.amendeDefautDepotRepositoryPort = amendeDefautDepotRepositoryPort;
        this.parametreFiscalService = parametreFiscalService;
    }

    /**
     * Cree (si absente) l'amende pour defaut de depot de l'obligation.
     *
     * @param idObligationFiscale l'obligation echue non declaree
     * @return l'amende existante ou nouvellement creee
     */
    @Transactional
    public AmendeDefautDepot execute(Long idObligationFiscale) {
        ObligationFiscale obligation = obligationFiscaleRepositoryPort.findById(idObligationFiscale)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Obligation fiscale introuvable : " + idObligationFiscale));

        Optional<AmendeDefautDepot> existante =
                amendeDefautDepotRepositoryPort.findByIdObligationFiscale(idObligationFiscale);
        if (existante.isPresent()) {
            return existante.get();
        }

        String nif = obligation.getNif();
        BigDecimal chiffreAffaires = declarationRepositoryPort.findDerniereDeclarationByNif(nif)
                .map(d -> d.getChiffreAffairesDeclare())
                .orElse(null);

        if (chiffreAffaires == null) {
            logger.warn("[AMENDE] Aucune declaration anterieure pour le NIF {} ; "
                    + "amende calculee sur la tranche la plus basse (A CONFIRMER)",
                    nif);
        }

        BigDecimal montant = determinerAmende(chiffreAffaires);

        AmendeDefautDepot amende = AmendeDefautDepot.builder()
                .idObligationFiscale(idObligationFiscale)
                .nif(nif)
                .chiffreAffaires(chiffreAffaires)
                .montant(montant)
                .dateCalcul(OffsetDateTime.now())
                .build();

        AmendeDefautDepot saved = amendeDefautDepotRepositoryPort.save(amende);
        logger.info("[AMENDE] Amende de defaut de depot {} Ar enregistree pour l'obligation {} (NIF {})",
                montant, idObligationFiscale, nif);
        return saved;
    }

    /**
     * Calcule le montant de l'amende selon le CA de reference.
     * Un CA nul ou inconnu releve de la tranche la plus basse (20 000 Ar),
     * conformement a la decision projet (A CONFIRMER, QUESTIONS_ENCADREUR.md).
     */
    public BigDecimal determinerAmende(BigDecimal chiffreAffaires) {
        BigDecimal ca = chiffreAffaires == null ? BigDecimal.ZERO : chiffreAffaires;

        BigDecimal seuil400M = parametreFiscalService.getValeur("AMENDE_SEUIL_GRANDE_ENTREPRISE");
        BigDecimal seuil200M = parametreFiscalService.getValeur("AMENDE_SEUIL_MOYENNE_ENTREPRISE");
        BigDecimal seuil50M = parametreFiscalService.getValeur("AMENDE_SEUIL_PETITE_ENTREPRISE");

        if (ca.compareTo(seuil400M) >= 0) {
            return parametreFiscalService.getValeur("DEFAUT_DEPOT_GRANDE_ENTREPRISE");
        }
        if (ca.compareTo(seuil200M) >= 0) {
            return parametreFiscalService.getValeur("DEFAUT_DEPOT_MOYENNE_ENTREPRISE");
        }
        if (ca.compareTo(seuil50M) >= 0) {
            return parametreFiscalService.getValeur("DEFAUT_DEPOT_PETITE_ENTREPRISE");
        }
        return parametreFiscalService.getValeur("DEFAUT_DEPOT_MICRO_ENTREPRISE");
    }

    @Transactional(readOnly = true)
    public boolean amendeExiste(Long idObligationFiscale) {
        return amendeDefautDepotRepositoryPort.existsByIdObligationFiscale(idObligationFiscale);
    }
}

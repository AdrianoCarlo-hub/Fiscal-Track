package mg.dgi.fiscaltrack.application.usecase.majoration;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.HistoriquePenaliteRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.SituationIrsa;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.HistoriquePenalite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

/**
 * Majorations IRSA (CDI 20.01.54.1) :
 *   - non retenu : 40 %
 *   - retenu non reverse : 80 %
 *   - retenues intermittentes : 10 % (plancher 20 000 Ar)
 *   - retenues intermittentes retenues : 40 % (plancher 100 000 Ar)
 *
 * Les conditions exactes de declenchement restent A CONFIRMER
 * (voir QUESTIONS_ENCADREUR.md) ; les taux et planchers sont, eux, fournis.
 */
@Service
public class AppliquerMajorationIrsaUseCase {

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private final HistoriquePenaliteRepositoryPort historiquePenaliteRepositoryPort;
    private final ParametreFiscalService parametreFiscalService;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public AppliquerMajorationIrsaUseCase(CompteCourantFiscalRepositoryPort compteRepositoryPort,
                                          HistoriquePenaliteRepositoryPort historiquePenaliteRepositoryPort,
                                          ParametreFiscalService parametreFiscalService,
                                          EnregistrerActionUseCase enregistrerActionUseCase) {
        this.compteRepositoryPort = compteRepositoryPort;
        this.historiquePenaliteRepositoryPort = historiquePenaliteRepositoryPort;
        this.parametreFiscalService = parametreFiscalService;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    @Transactional
    public BigDecimal execute(Long idCompte, BigDecimal base, SituationIrsa situation) {
        if (situation == null) {
            throw new IllegalArgumentException("La situation IRSA est obligatoire");
        }

        CompteCourantFiscal compte = compteRepositoryPort.findById(idCompte)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Compte courant fiscal introuvable : " + idCompte));

        BigDecimal taux = tauxPour(situation);
        BigDecimal montant = base.multiply(taux).setScale(2, RoundingMode.HALF_UP);

        BigDecimal plancher = plancherPour(situation);
        if (plancher != null) {
            montant = montant.max(plancher).setScale(2, RoundingMode.HALF_UP);
        }

        MajorationSupport.appliquer(compte, montant);
        compteRepositoryPort.save(compte);

        historiquePenaliteRepositoryPort.save(HistoriquePenalite.builder()
                .idCompte(idCompte)
                .typeLigne(HistoriquePenalite.TYPE_MAJORATION)
                .dateCalcul(OffsetDateTime.now())
                .taux(taux)
                .base(base)
                .montant(montant)
                .build());

        enregistrerActionUseCase.execute(compte.getNif(),
                "MAJORATION_IRSA_20_01_54_1",
                "Majoration IRSA (" + situation + ") de " + montant + " Ar sur le compte #" + idCompte);

        return montant;
    }

    private BigDecimal tauxPour(SituationIrsa situation) {
        return switch (situation) {
            case NON_RETENU -> parametreFiscalService.getValeur("MAJORATION_IRSA_NON_RETENU");
            case RETENU_NON_REVERSE -> parametreFiscalService.getValeur("MAJORATION_IRSA_RETENU_NON_REVERSE");
            case INTERMITTENT -> parametreFiscalService.getValeur("MAJORATION_IRSA_INTERMITTENT_TAUX");
            case INTERMITTENT_RETENU -> parametreFiscalService.getValeur("MAJORATION_IRSA_INTERMITTENT_RETENU_TAUX");
        };
    }

    private BigDecimal plancherPour(SituationIrsa situation) {
        return switch (situation) {
            case INTERMITTENT -> parametreFiscalService.getValeur("MAJORATION_IRSA_INTERMITTENT_MIN");
            case INTERMITTENT_RETENU -> parametreFiscalService.getValeur("MAJORATION_IRSA_INTERMITTENT_RETENU_MIN");
            default -> null;
        };
    }
}

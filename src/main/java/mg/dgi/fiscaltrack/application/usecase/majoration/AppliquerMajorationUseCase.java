package mg.dgi.fiscaltrack.application.usecase.majoration;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.HistoriquePenaliteRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.HistoriquePenalite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

/**
 * Majoration de droit commun (CDI 20.01.54) : 40 %.
 *
 * Applique la majoration a un compte courant fiscal (ajout au montant des
 * penalites), trace une ligne d'historique et une action d'audit.
 *
 * La condition exacte de declenchement reste A CONFIRMER
 * (voir QUESTIONS_ENCADREUR.md) : seule la valeur 40 % est fournie.
 */
@Service
public class AppliquerMajorationUseCase {

    private static final String CODE_TAUX = "MAJORATION_TAUX";

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private final HistoriquePenaliteRepositoryPort historiquePenaliteRepositoryPort;
    private final ParametreFiscalService parametreFiscalService;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public AppliquerMajorationUseCase(CompteCourantFiscalRepositoryPort compteRepositoryPort,
                                      HistoriquePenaliteRepositoryPort historiquePenaliteRepositoryPort,
                                      ParametreFiscalService parametreFiscalService,
                                      EnregistrerActionUseCase enregistrerActionUseCase) {
        this.compteRepositoryPort = compteRepositoryPort;
        this.historiquePenaliteRepositoryPort = historiquePenaliteRepositoryPort;
        this.parametreFiscalService = parametreFiscalService;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    @Transactional
    public BigDecimal execute(Long idCompte, BigDecimal base) {
        CompteCourantFiscal compte = compteRepositoryPort.findById(idCompte)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Compte courant fiscal introuvable : " + idCompte));

        BigDecimal taux = parametreFiscalService.getValeur(CODE_TAUX);
        BigDecimal montant = base.multiply(taux).setScale(2, RoundingMode.HALF_UP);

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
                "MAJORATION_20_01_54",
                "Majoration 40% de " + montant + " Ar sur le compte #" + idCompte);

        return montant;
    }
}

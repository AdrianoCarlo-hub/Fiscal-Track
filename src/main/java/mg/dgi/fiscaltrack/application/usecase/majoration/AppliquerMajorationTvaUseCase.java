package mg.dgi.fiscaltrack.application.usecase.majoration;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.HistoriquePenaliteRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.NiveauMajorationTva;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.HistoriquePenalite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

/**
 * Majorations TVA (CDI 20.01.54.2) : niveaux 10 / 40 / 80 / 150 %.
 *
 * La correspondance niveau <-> infraction reste A CONFIRMER
 * (voir QUESTIONS_ENCADREUR.md) ; seuls les taux sont fournis.
 */
@Service
public class AppliquerMajorationTvaUseCase {

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private final HistoriquePenaliteRepositoryPort historiquePenaliteRepositoryPort;
    private final ParametreFiscalService parametreFiscalService;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public AppliquerMajorationTvaUseCase(CompteCourantFiscalRepositoryPort compteRepositoryPort,
                                         HistoriquePenaliteRepositoryPort historiquePenaliteRepositoryPort,
                                         ParametreFiscalService parametreFiscalService,
                                         EnregistrerActionUseCase enregistrerActionUseCase) {
        this.compteRepositoryPort = compteRepositoryPort;
        this.historiquePenaliteRepositoryPort = historiquePenaliteRepositoryPort;
        this.parametreFiscalService = parametreFiscalService;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    @Transactional
    public BigDecimal execute(Long idCompte, BigDecimal base, NiveauMajorationTva niveau) {
        if (niveau == null) {
            throw new IllegalArgumentException("Le niveau de majoration TVA est obligatoire");
        }

        CompteCourantFiscal compte = compteRepositoryPort.findById(idCompte)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Compte courant fiscal introuvable : " + idCompte));

        BigDecimal taux = parametreFiscalService.getValeur(codeTaux(niveau));
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
                "MAJORATION_TVA_20_01_54_2",
                "Majoration TVA (" + niveau + ") de " + montant + " Ar sur le compte #" + idCompte);

        return montant;
    }

    private String codeTaux(NiveauMajorationTva niveau) {
        return switch (niveau) {
            case NIVEAU_1 -> "MAJORATION_TVA_NIVEAU_1";
            case NIVEAU_2 -> "MAJORATION_TVA_NIVEAU_2";
            case NIVEAU_3 -> "MAJORATION_TVA_NIVEAU_3";
            case NIVEAU_4 -> "MAJORATION_TVA_NIVEAU_4";
        };
    }
}

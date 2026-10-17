package mg.dgi.fiscaltrack.application.usecase.majoration;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.HistoriquePenaliteRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.HistoriquePenalite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

/**
 * Remise gracieuse (CDI 20.01.51).
 *
 * Action manuelle reservee a RESPONSABLE ou ADMIN (verifie au niveau de la
 * securite/endpoint), justification obligatoire, tracee dans l'historique
 * d'audit et dans l'historique des penalites. La remise est plafonnee aux
 * penalites/majorations restant dues du compte.
 *
 * Les conditions d'eligibilite exactes restent A CONFIRMER
 * (voir QUESTIONS_ENCADREUR.md).
 */
@Service
public class AccorderRemiseGracieuseUseCase {

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private final HistoriquePenaliteRepositoryPort historiquePenaliteRepositoryPort;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public AccorderRemiseGracieuseUseCase(CompteCourantFiscalRepositoryPort compteRepositoryPort,
                                          HistoriquePenaliteRepositoryPort historiquePenaliteRepositoryPort,
                                          EnregistrerActionUseCase enregistrerActionUseCase) {
        this.compteRepositoryPort = compteRepositoryPort;
        this.historiquePenaliteRepositoryPort = historiquePenaliteRepositoryPort;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    @Transactional
    public BigDecimal execute(Long idCompte, BigDecimal montantRemise,
                              String justification, String auteur) {
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException("Une justification est obligatoire pour une remise gracieuse");
        }
        if (montantRemise == null || montantRemise.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant de la remise doit etre strictement positif");
        }

        CompteCourantFiscal compte = compteRepositoryPort.findById(idCompte)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Compte courant fiscal introuvable : " + idCompte));

        if (compte.getStatutRecouvrement() == StatutRecouvrement.SOLDE) {
            throw new IllegalStateException("Impossible d'accorder une remise sur un compte solde");
        }

        BigDecimal penalites = compte.getMontantPenalites() == null
                ? BigDecimal.ZERO
                : compte.getMontantPenalites();
        BigDecimal remiseEffective = montantRemise.min(penalites).setScale(2, RoundingMode.HALF_UP);

        compte.setMontantPenalites(penalites.subtract(remiseEffective));
        compte.setUpdatedAt(OffsetDateTime.now());
        compteRepositoryPort.save(compte);

        historiquePenaliteRepositoryPort.save(HistoriquePenalite.builder()
                .idCompte(idCompte)
                .typeLigne(HistoriquePenalite.TYPE_REMISE_GRACIEUSE)
                .dateCalcul(OffsetDateTime.now())
                .base(penalites)
                .montant(remiseEffective)
                .build());

        enregistrerActionUseCase.execute(
                auteur == null ? compte.getNif() : auteur,
                "REMISE_GRACIEUSE_20_01_51",
                "Remise gracieuse de " + remiseEffective + " Ar sur le compte #" + idCompte
                        + " | Justification : " + justification);

        return remiseEffective;
    }
}

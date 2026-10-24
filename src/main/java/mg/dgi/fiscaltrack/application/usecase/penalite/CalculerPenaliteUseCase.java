package mg.dgi.fiscaltrack.application.usecase.penalite;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.HistoriquePenaliteRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.application.usecase.notification.ContexteNotification;
import mg.dgi.fiscaltrack.application.usecase.notification.ContexteObligationService;
import mg.dgi.fiscaltrack.application.usecase.notification.NotifierEvenementUseCase;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.domain.model.HistoriquePenalite;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class CalculerPenaliteUseCase {

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private final DeclarationRepositoryPort declarationRepositoryPort;
    private final HistoriquePenaliteRepositoryPort historiquePenaliteRepositoryPort;
    private final ParametreFiscalService parametreFiscalService;

    private ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private ContexteObligationService contexteObligationService;
    private NotifierEvenementUseCase notifierEvenementUseCase;

    public CalculerPenaliteUseCase(CompteCourantFiscalRepositoryPort compteRepositoryPort,
                                    DeclarationRepositoryPort declarationRepositoryPort,
                                    HistoriquePenaliteRepositoryPort historiquePenaliteRepositoryPort,
                                    ParametreFiscalService parametreFiscalService) {
        this.compteRepositoryPort = compteRepositoryPort;
        this.declarationRepositoryPort = declarationRepositoryPort;
        this.historiquePenaliteRepositoryPort = historiquePenaliteRepositoryPort;
        this.parametreFiscalService = parametreFiscalService;
    }

    @Autowired(required = false)
    public void setObligationRepositoryPort(ObligationFiscaleRepositoryPort obligationRepositoryPort) {
        this.obligationRepositoryPort = obligationRepositoryPort;
    }

    @Autowired(required = false)
    public void setContexteObligationService(ContexteObligationService contexteObligationService) {
        this.contexteObligationService = contexteObligationService;
    }

    @Autowired(required = false)
    public void setNotifierEvenementUseCase(NotifierEvenementUseCase notifierEvenementUseCase) {
        this.notifierEvenementUseCase = notifierEvenementUseCase;
    }

    /**
     * CDI 20.01.53 : taux initial selon le CA, puis +1 % par mois commence.
     * Le taux cumule est applique une seule fois au montant a payer, avec plancher 10 000 Ar.
     * Application :
     *   - CA >= 400M : taux initial 3%
     *   - 100M <= CA < 400M : taux initial 2%
     *   - CA < 100M : taux initial 1%
     *   - Puis +1% par mois de retard supplementaire
     *   - Plancher de 10 000 Ar sur le total
     */
    @Transactional
    public BigDecimal execute(Long idCompte, LocalDate dateLimiteReelle) {
        CompteCourantFiscal compte = compteRepositoryPort.findById(idCompte)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Compte courant fiscal introuvable : " + idCompte));

        if (dateLimiteReelle == null) {
            throw new IllegalArgumentException("La date limite reelle est obligatoire");
        }

        // Un compte deja solde n'est jamais recalcule (point 2.1).
        if (compte.getStatutRecouvrement() == StatutRecouvrement.SOLDE) {
            return compte.getMontantPenalites();
        }

        long moisRetard = calculerMoisRetard(dateLimiteReelle, LocalDate.now());
        if (moisRetard <= 0) {
            return compte.getMontantPenalites();
        }

        Declaration declaration = declarationRepositoryPort
                .findById(compte.getIdDeclaration())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Declaration introuvable pour le compte : " + idCompte));

        BigDecimal tauxCumule = determinerTauxCumule(declaration.getChiffreAffairesDeclare(), moisRetard);
        // La penalite porte sur le principal restant du (point 2.1).
        BigDecimal montantBase = principalRestantDu(compte);

        // Plancher de 10 000 Ar (CDI 20.01.53)
        BigDecimal plancher = parametreFiscalService.getValeur("PENALITE_PLANCHER");
        BigDecimal penalites = montantBase
                .multiply(tauxCumule)
                .max(plancher)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal ancienMontant = compte.getMontantPenalites() == null
                ? BigDecimal.ZERO
                : compte.getMontantPenalites();

        // Tracabilite : une ligne d'historique a chaque changement de montant
        // (point 2.1).
        if (penalites.compareTo(ancienMontant) != 0) {
            historiquePenaliteRepositoryPort.save(HistoriquePenalite.builder()
                    .idCompte(idCompte)
                    .typeLigne(HistoriquePenalite.TYPE_PENALITE_RETARD)
                    .dateCalcul(OffsetDateTime.now())
                    .moisRetard((int) moisRetard)
                    .taux(tauxCumule)
                    .base(montantBase)
                    .montant(penalites)
                    .build());
        }

        compte.setMontantPenalites(penalites);
        compte.setUpdatedAt(OffsetDateTime.now());
        compteRepositoryPort.save(compte);

        return penalites;
    }

    /**
     * Principal restant du = principal non encore couvert par les paiements.
     */
    private BigDecimal principalRestantDu(CompteCourantFiscal compte) {
        BigDecimal principal = compte.getMontantPrincipal() == null
                ? BigDecimal.ZERO
                : compte.getMontantPrincipal();
        BigDecimal paye = compte.getMontantPaye() == null
                ? BigDecimal.ZERO
                : compte.getMontantPaye();
        BigDecimal restant = principal.subtract(paye);
        return restant.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : restant;
    }

    /**
     * Calcul du nombre de mois de retard. Toute fraction de mois
     * entamee compte pour un mois complet (CDI 20.01.53).
     */
    private long calculerMoisRetard(LocalDate dateLimite, LocalDate aujourdHui) {
        if (!aujourdHui.isAfter(dateLimite)) {
            return 0;
        }
        long mois = ChronoUnit.MONTHS.between(dateLimite, aujourdHui);
        // Si la date du jour n'est pas exactement un mois apres, on ajoute 1
        if (dateLimite.plusMonths(mois).isBefore(aujourdHui)) {
            mois++;
        }
        return mois;
    }

    /**
     * Détermine le taux cumulé applicé pour le nombre de mois de retard.
     * CDI 20.01.53 : taux initial + 1 % × (mois - 1), avec plancher 10 000 Ar.
     */
    private BigDecimal determinerTauxCumule(BigDecimal chiffreAffaires, long moisRetard) {
        BigDecimal tauxInitial = determinerTauxInitial(chiffreAffaires);
        // Increment de 1% par mois apres le premier (CDI 20.01.53)
        BigDecimal increment = parametreFiscalService.getValeur("PENALITE_TAUX_INCREMENT_MENSUEL")
                .multiply(BigDecimal.valueOf(Math.max(0, moisRetard - 1)));
        return tauxInitial.add(increment);
    }

    /**
     * Determination du taux de depart selon la taille d'entreprise.
     * CDI 20.01.53 :
     *   - Grande   : CA >= 400 000 000 Ar -> 3%
     *   - Moyenne  : CA >= 100 000 000 Ar -> 2%
     *   - Petite   : CA <  100 000 000 Ar -> 1%
     */
    private BigDecimal determinerTauxInitial(BigDecimal chiffreAffaires) {
        if (chiffreAffaires == null) {
            return parametreFiscalService.getValeur("PENALITE_TAUX_INITIAL_PETITE");
        }
        BigDecimal seuilGrande = parametreFiscalService.getValeur("PENALITE_SEUIL_GRANDE_ENTREPRISE");
        BigDecimal seuilMoyenne = parametreFiscalService.getValeur("PENALITE_SEUIL_MOYENNE_ENTREPRISE");

        if (chiffreAffaires.compareTo(seuilGrande) >= 0) {
            return parametreFiscalService.getValeur("PENALITE_TAUX_INITIAL_GRANDE");
        }
        if (chiffreAffaires.compareTo(seuilMoyenne) >= 0) {
            return parametreFiscalService.getValeur("PENALITE_TAUX_INITIAL_MOYENNE");
        }
        return parametreFiscalService.getValeur("PENALITE_TAUX_INITIAL_PETITE");
    }
}

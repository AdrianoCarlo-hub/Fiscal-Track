package mg.dgi.fiscaltrack.application.usecase.penalite;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class CalculerPenaliteUseCase {

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private final DeclarationRepositoryPort declarationRepositoryPort;

    public CalculerPenaliteUseCase(CompteCourantFiscalRepositoryPort compteRepositoryPort,
                                    DeclarationRepositoryPort declarationRepositoryPort) {
        this.compteRepositoryPort = compteRepositoryPort;
        this.declarationRepositoryPort = declarationRepositoryPort;
    }

    /**
     * RC4-RC7 : calcule la penalite de retard pour un compte courant fiscal.
     * Le taux mensuel depend de la taille de l'entreprise :
     *   - Grande entreprise   : 3% par mois
     *   - Moyenne entreprise  : 2% par mois
     *   - Petite entreprise   : 1% par mois
     * Toute fraction de mois entamee compte pour un mois complet.
     */
    @Transactional
    public BigDecimal execute(Long idCompte, LocalDate dateLimiteReelle) {
        CompteCourantFiscal compte = compteRepositoryPort.findById(idCompte)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Compte courant fiscal introuvable : " + idCompte));

        if (dateLimiteReelle == null) {
            throw new IllegalArgumentException("La date limite reelle est obligatoire");
        }

        long moisRetard = calculerMoisRetard(dateLimiteReelle, LocalDate.now());
        if (moisRetard <= 0) {
            return compte.getMontantPenalites();
        }

        Declaration declaration = declarationRepositoryPort
                .findById(compte.getIdDeclaration())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Declaration introuvable pour le compte : " + idCompte));

        BigDecimal tauxMensuel = determinerTaux(declaration.getChiffreAffairesDeclare());
        BigDecimal penalites = compte.getMontantPrincipal()
                .multiply(tauxMensuel)
                .multiply(BigDecimal.valueOf(moisRetard))
                .setScale(2, RoundingMode.HALF_UP);

        compte.setMontantPenalites(penalites);
        compte.setUpdatedAt(OffsetDateTime.now());
        compteRepositoryPort.save(compte);

        return penalites;
    }

    /**
     * Calcul du nombre de mois de retard. Toute fraction de mois
     * entamee compte pour un mois complet.
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
     * Determination de la taille d'entreprise a partir du chiffre d'affaires.
     * Seuils usuels :
     *   - Grande   : CA >= 400 000 000 Ar
     *   - Moyenne  : CA >= 100 000 000 Ar
     *   - Petite   : CA <  100 000 000 Ar
     */
    private BigDecimal determinerTaux(BigDecimal chiffreAffaires) {
        if (chiffreAffaires == null) {
            return new BigDecimal("0.01");
        }
        BigDecimal seuilGrande = new BigDecimal("400000000");
        BigDecimal seuilMoyenne = new BigDecimal("100000000");

        if (chiffreAffaires.compareTo(seuilGrande) >= 0) {
            return new BigDecimal("0.03");
        }
        if (chiffreAffaires.compareTo(seuilMoyenne) >= 0) {
            return new BigDecimal("0.02");
        }
        return new BigDecimal("0.01");
    }
}

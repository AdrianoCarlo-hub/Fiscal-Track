package mg.dgi.fiscaltrack.application.usecase.paiement;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.PaiementRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.ModePaiement;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.exception.PaiementInvalideException;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Paiement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class EnregistrerPaiementUseCase {

    private final PaiementRepositoryPort paiementRepositoryPort;
    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public EnregistrerPaiementUseCase(PaiementRepositoryPort paiementRepositoryPort,
                                       CompteCourantFiscalRepositoryPort compteRepositoryPort,
                                       EnregistrerActionUseCase enregistrerActionUseCase) {
        this.paiementRepositoryPort = paiementRepositoryPort;
        this.compteRepositoryPort = compteRepositoryPort;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    @Transactional
    public Paiement execute(Long idCompte, BigDecimal montantVerse,
                            ModePaiement modePaiement, String referenceTransaction) {

        if (montantVerse == null || montantVerse.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaiementInvalideException("Le montant verse doit etre strictement positif");
        }
        if (modePaiement == null) {
            throw new PaiementInvalideException("Le mode de paiement est obligatoire");
        }
        if (referenceTransaction != null
                && paiementRepositoryPort.existsByReferenceTransaction(referenceTransaction)) {
            throw new PaiementInvalideException("Une transaction avec cette reference existe deja");
        }

        CompteCourantFiscal compte = compteRepositoryPort.findById(idCompte)
                .orElseThrow(() -> new PaiementInvalideException("Compte courant fiscal introuvable : " + idCompte));

        if (compte.getStatutRecouvrement() == StatutRecouvrement.SOLDE) {
            throw new PaiementInvalideException("Ce compte est deja totalement solde");
        }

        BigDecimal reste = compte.getResteARecouvrer();
        if (reste == null) {
            reste = compte.getMontantPrincipal()
                    .add(compte.getMontantPenalites())
                    .subtract(compte.getMontantPaye());
        }
        if (montantVerse.compareTo(reste) > 0) {
            throw new PaiementInvalideException("Le montant verse depasse le reste a recouvrer : " + reste);
        }

        Paiement paiement = Paiement.builder()
                .idCompte(idCompte)
                .montantVerse(montantVerse)
                .datePaiement(OffsetDateTime.now())
                .modePaiement(modePaiement)
                .referenceTransaction(referenceTransaction)
                .build();
        Paiement paiementSauve = paiementRepositoryPort.save(paiement);

        BigDecimal nouveauMontantPaye = compte.getMontantPaye().add(montantVerse);
        compte.setMontantPaye(nouveauMontantPaye);
        compte.setStatutRecouvrement(calculerStatut(compte, nouveauMontantPaye));
        compteRepositoryPort.save(compte);

        enregistrerActionUseCase.execute(
                compte.getNif(),
                "ENREGISTREMENT_PAIEMENT",
                "Paiement #" + paiementSauve.getIdPaiement() + " de " + montantVerse + " Ar sur compte #" + idCompte);

        return paiementSauve;
    }

    private StatutRecouvrement calculerStatut(CompteCourantFiscal compte, BigDecimal montantPaye) {
        BigDecimal totalDu = compte.getMontantPrincipal().add(compte.getMontantPenalites());
        if (montantPaye.compareTo(BigDecimal.ZERO) == 0) {
            return StatutRecouvrement.NON_SOLDE;
        }
        if (montantPaye.compareTo(totalDu) >= 0) {
            return StatutRecouvrement.SOLDE;
        }
        return StatutRecouvrement.PARTIEL;
    }
}

package mg.dgi.fiscaltrack.application.usecase.paiement;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.PaiementRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.application.usecase.notification.ContexteNotification;
import mg.dgi.fiscaltrack.application.usecase.notification.ContexteObligationService;
import mg.dgi.fiscaltrack.application.usecase.notification.NotifierEvenementUseCase;
import mg.dgi.fiscaltrack.domain.enums.ModePaiement;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import mg.dgi.fiscaltrack.domain.exception.PaiementInvalideException;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.domain.model.Paiement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class EnregistrerPaiementUseCase {

    private final PaiementRepositoryPort paiementRepositoryPort;
    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    private DeclarationRepositoryPort declarationRepositoryPort;
    private ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private ContexteObligationService contexteObligationService;
    private NotifierEvenementUseCase notifierEvenementUseCase;

    public EnregistrerPaiementUseCase(PaiementRepositoryPort paiementRepositoryPort,
                                       CompteCourantFiscalRepositoryPort compteRepositoryPort,
                                       EnregistrerActionUseCase enregistrerActionUseCase) {
        this.paiementRepositoryPort = paiementRepositoryPort;
        this.compteRepositoryPort = compteRepositoryPort;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    @Autowired(required = false)
    public void setDeclarationRepositoryPort(DeclarationRepositoryPort declarationRepositoryPort) {
        this.declarationRepositoryPort = declarationRepositoryPort;
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

        notifierPaiement(compte, paiementSauve, montantVerse);

        return paiementSauve;
    }

    /** Notification de l'evenement PAIEMENT_RECU : jamais bloquante pour le paiement. */
    private void notifierPaiement(CompteCourantFiscal compte, Paiement paiement, BigDecimal montantVerse) {
        if (notifierEvenementUseCase == null) {
            return;
        }
        try {
            ContexteNotification contexte = contextePaiement(compte, montantVerse);
            notifierEvenementUseCase.notifier(TypeRelance.PAIEMENT_RECU, contexte,
                    null, compte.getIdCompte());
        } catch (Exception ignored) {
            // une panne de notification n'annule jamais le paiement
        }
    }

    private ContexteNotification contextePaiement(CompteCourantFiscal compte, BigDecimal montantVerse) {
        if (declarationRepositoryPort != null && obligationRepositoryPort != null
                && contexteObligationService != null && compte.getIdDeclaration() != null) {
            Optional<Declaration> declaration = declarationRepositoryPort.findById(compte.getIdDeclaration());
            if (declaration.isPresent() && declaration.get().getIdObligationFiscale() != null) {
                Optional<ObligationFiscale> obligation = obligationRepositoryPort
                        .findById(declaration.get().getIdObligationFiscale());
                if (obligation.isPresent()) {
                    ContexteNotification base = contexteObligationService.depuis(obligation.get());
                    return ContexteNotification.builder()
                            .nif(base.getNif())
                            .codeImpot(base.getCodeImpot())
                            .nomImpot(base.getNomImpot())
                            .periode(base.getPeriode())
                            .echeance(base.getEcheance())
                            .montantRetenu(montantVerse)
                            .build();
                }
            }
        }
        return ContexteNotification.builder()
                .nif(compte.getNif())
                .montantRetenu(montantVerse)
                .build();
    }

    private StatutRecouvrement calculerStatut(CompteCourantFiscal compte, BigDecimal montantPaye) {
        // Le total du inclut l'amende de defaut de depot (CDI 20.01.52),
        // au meme titre que la colonne generee montant_total_du.
        BigDecimal totalDu = compte.getMontantPrincipal().add(compte.getMontantPenalites());
        if (compte.getMontantAmende() != null) {
            totalDu = totalDu.add(compte.getMontantAmende());
        }
        if (montantPaye.compareTo(BigDecimal.ZERO) == 0) {
            return StatutRecouvrement.NON_SOLDE;
        }
        if (montantPaye.compareTo(totalDu) >= 0) {
            return StatutRecouvrement.SOLDE;
        }
        return StatutRecouvrement.PARTIEL;
    }
}

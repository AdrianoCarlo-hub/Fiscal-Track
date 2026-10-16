package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.TypeImpotRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.enums.StatutDeclaration;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Generation des obligations fiscales avec echeances selon CPF et CDI.
 * 
 * Regles :
 *   - IR (CPF I-11) :
 *     - Exercice civil : 15 mai N+1
 *     - Cloture au 30 juin : 15 novembre de la meme annee
 *     - Autres clotures : 15e jour du 4e mois suivant la cloture
 *     - Sans date de cloture : exercice civil par defaut, avec avertissement
 *   - IS (CPF I-17) : avant le 31 mars N+1
 *   - TVA (CDI 06.01.16) : 15 du mois suivant
 *   - IRSA (CPF I-21) : dans les 15 premiers jours du mois suivant
 *   - Toutes les dates passent par CalculerEcheanceReelleUseCase pour report au premier jour ouvrable (CPF VII-32)
 *   - Acomptes IR (Document IR 2026) : 31 mars et 30 juin, 25% de l'IR N-1 chacun
 *
 * Source de verite des echeances : les REGLES CODEES de ce use case
 * (methode calculerDateTheoriqueSpecifique). Les champs echeanceTheoriqueJour/
 * echeanceTheoriqueMois de TypeImpot ne sont volontairement PAS utilises comme
 * source de calcul (double source supprimee). Voir CONSTANTES_FISCALES.md.
 */
@Service
public class GenererObligationsUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final TypeImpotRepositoryPort typeImpotRepositoryPort;
    private final ContribuableRepositoryPort contribuableRepositoryPort;
    private final CalculerEcheanceReelleUseCase calculerEcheanceReelleUseCase;
    private final ParametreFiscalService parametreFiscalService;

    public GenererObligationsUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                      TypeImpotRepositoryPort typeImpotRepositoryPort,
                                      ContribuableRepositoryPort contribuableRepositoryPort,
                                      CalculerEcheanceReelleUseCase calculerEcheanceReelleUseCase,
                                      ParametreFiscalService parametreFiscalService) {
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.typeImpotRepositoryPort = typeImpotRepositoryPort;
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.calculerEcheanceReelleUseCase = calculerEcheanceReelleUseCase;
        this.parametreFiscalService = parametreFiscalService;
    }

    public ObligationFiscale execute(String nif, String codeImpot, String periodeFiscale) {
        // Verifie l'existence du type d'impot (les echeances sont codees, pas lues ici)
        typeImpotRepositoryPort.findByCodeImpot(codeImpot)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Type d'impot introuvable : " + codeImpot));

        // Evite les doublons : une obligation est unique par (nif, impot, periode)
        boolean dejaExistante = !obligationRepositoryPort
                .findByNifEtPeriode(nif, periodeFiscale).stream()
                .filter(o -> o.getCodeImpot().equals(codeImpot))
                .toList().isEmpty();
        if (dejaExistante) {
            throw new IllegalArgumentException(
                    "Une obligation existe deja pour cette periode et cet impot");
        }

        LocalDate dateCloture = contribuableRepositoryPort.findByNif(nif)
                .map(Contribuable::getDateCloture)
                .orElse(null);

        LocalDate dateTheorique = calculerDateTheorique(codeImpot, periodeFiscale, dateCloture);
        CalculerEcheanceReelleUseCase.EcheanceReelle echeance =
                calculerEcheanceReelleUseCase.calculer(dateTheorique);

        ObligationFiscale obligation = ObligationFiscale.builder()
                .nif(nif)
                .codeImpot(codeImpot)
                .periodeFiscale(periodeFiscale)
                .dateLimiteTheorique(dateTheorique)
                .dateLimiteReelle(echeance.date())
                .idJourFerie(echeance.idJourFerie())
                .statutDeclaration(StatutDeclaration.ATTENTE)
                .build();

        return obligationRepositoryPort.save(obligation);
    }

    /**
     * Calcule la date theorique selon les regles codees (source de verite unique).
     * Le parametre {@code typeImpot} n'est plus utilise pour le calcul.
     */
    private LocalDate calculerDateTheorique(String codeImpot, String periodeFiscale, LocalDate dateCloture) {
        return calculerDateTheoriqueSpecifique(codeImpot, periodeFiscale, dateCloture);
    }

    /**
     * Calcule la date theorique selon les regles specifiques par type d'impot.
     * Applique les regles CPF et CDI.
     */
    private LocalDate calculerDateTheoriqueSpecifique(String codeImpot, String periodeFiscale,
                                                      LocalDate dateCloture) {
        String codeUpper = codeImpot.toUpperCase();
        
        // TVA (CDI 06.01.16) : 15 du mois suivant
        if (codeUpper.equals("TVA")) {
            return calculerEcheanceTva(periodeFiscale);
        }
        
        // IS (CPF I-17) : avant le 31 mars N+1
        if (codeUpper.equals("IS") || codeUpper.equals("IMPOT_SYNTHETIQUE")) {
            return calculerEcheanceIs(periodeFiscale);
        }
        
        // IRSA (CPF I-21) : dans les 15 premiers jours du mois suivant
        if (codeUpper.equals("IRSA")) {
            return calculerEcheanceIrsa(periodeFiscale);
        }
        
        // IR (CPF I-11) : selon date de cloture
        if (codeUpper.equals("IR")) {
            return calculerEcheanceIr(periodeFiscale, dateCloture);
        }
        
        // Acomptes IR (Document IR 2026) : 31 mars et 30 juin
        if (codeUpper.equals("ACOMPTE_IR")) {
            return calculerEcheanceAcompteIr(periodeFiscale);
        }
        
        // Par defaut, utiliser mars de l'annee suivante
        int annee = Integer.parseInt(periodeFiscale);
        Integer jourEcheance = parametreFiscalService.getValeur("ECHEANCE_IS_JOUR").intValue();
        Integer moisEcheance = parametreFiscalService.getValeur("ECHEANCE_IS_MOIS").intValue();
        return LocalDate.of(annee + 1, moisEcheance, jourEcheance);
    }

    /**
     * TVA : echeance le 15 du mois suivant (CDI 06.01.16).
     */
    private LocalDate calculerEcheanceTva(String periodeFiscale) {
        // periodeFiscale au format "YYYY-MM"
        String[] parts = periodeFiscale.split("-");
        int annee = Integer.parseInt(parts[0]);
        int moisPeriode = Integer.parseInt(parts[1]);
        
        int moisEcheance = moisPeriode == 12 ? 1 : moisPeriode + 1;
        int anneeEcheance = moisPeriode == 12 ? annee + 1 : annee;
        
        Integer jour = parametreFiscalService.getValeur("TVA_ECHEANCE_JOUR").intValue();
        return LocalDate.of(anneeEcheance, moisEcheance, jour);
    }

    /**
     * IS : avant le 31 mars N+1 (CPF I-17).
     */
    private LocalDate calculerEcheanceIs(String periodeFiscale) {
        // periodeFiscale au format "YYYY"
        int annee = Integer.parseInt(periodeFiscale);
        
        Integer jour = parametreFiscalService.getValeur("ECHEANCE_IS_JOUR").intValue();
        Integer mois = parametreFiscalService.getValeur("ECHEANCE_IS_MOIS").intValue();
        return LocalDate.of(annee + 1, mois, jour);
    }

    /**
     * IRSA : dans les 15 premiers jours du mois suivant (CPF I-21).
     */
    private LocalDate calculerEcheanceIrsa(String periodeFiscale) {
        // periodeFiscale au format "YYYY-MM"
        String[] parts = periodeFiscale.split("-");
        int annee = Integer.parseInt(parts[0]);
        int moisPeriode = Integer.parseInt(parts[1]);
        
        int moisEcheance = moisPeriode == 12 ? 1 : moisPeriode + 1;
        int anneeEcheance = moisPeriode == 12 ? annee + 1 : annee;
        
        Integer jour = parametreFiscalService.getValeur("ECHEANCE_IRSA_JOUR").intValue();
        return LocalDate.of(anneeEcheance, moisEcheance, jour);
    }

    /**
     * IR : selon date de cloture (CPF I-11, Document IR 2026).
     * - Exercice civil (31 decembre ou date non renseignee) : 15 mai N+1
     * - Cloture au 30 juin : 15 novembre de la meme annee
     * - Autres clotures (sur autorisation) : 15e jour du 4e mois suivant la cloture
     *
     * periodeFiscale au format "YYYY".
     */
    private LocalDate calculerEcheanceIr(String periodeFiscale, LocalDate dateCloture) {
        int annee = Integer.parseInt(periodeFiscale);

        boolean exerciceCivil = dateCloture == null
                || (dateCloture.getMonthValue() == 12 && dateCloture.getDayOfMonth() == 31);

        if (exerciceCivil) {
            Integer jour = parametreFiscalService.getValeur("ECHEANCE_IR_EXERCICE_CIVIL_JOUR").intValue();
            Integer mois = parametreFiscalService.getValeur("ECHEANCE_IR_EXERCICE_CIVIL_MOIS").intValue();
            return LocalDate.of(annee + 1, mois, jour);
        }

        if (dateCloture.getMonthValue() == 6 && dateCloture.getDayOfMonth() == 30) {
            Integer jour = parametreFiscalService.getValeur("ECHEANCE_IR_CLOTURE_30_JUIN_JOUR").intValue();
            Integer mois = parametreFiscalService.getValeur("ECHEANCE_IR_CLOTURE_30_JUIN_MOIS").intValue();
            return LocalDate.of(annee, mois, jour);
        }

        // Autres clotures : 15e jour du 4e mois suivant la cloture
        LocalDate clotureExercice = LocalDate.of(annee,
                dateCloture.getMonthValue(), dateCloture.getDayOfMonth());
        int delaiMois = parametreFiscalService.getValeur("ECHEANCE_IR_AUTRE_CLOTURE_DELAI_MOIS").intValue();
        Integer jour = parametreFiscalService.getValeur("ECHEANCE_IR_AUTRE_CLOTURE_JOUR").intValue();
        LocalDate cible = clotureExercice.plusMonths(delaiMois);
        return LocalDate.of(cible.getYear(), cible.getMonthValue(), jour);
    }

    /**
     * Acomptes IR : 31 mars et 30 juin (Document IR 2026).
     * Chaque acompte = 25% de l'IR N-1.
     * Pour periodeFiscale au format "YYYY-AC1" ou "YYYY-AC2"
     */
    private LocalDate calculerEcheanceAcompteIr(String periodeFiscale) {
        // periodeFiscale au format "YYYY-AC1" ou "YYYY-AC2"
        String[] parts = periodeFiscale.split("-");
        int annee = Integer.parseInt(parts[0]);
        String acompte = parts[1];
        
        if (acompte.equals("AC1")) {
            Integer jour = parametreFiscalService.getValeur("ACOMPTE_IR_ECHEANCE_1").intValue();
            Integer mois = parametreFiscalService.getValeur("ACOMPTE_IR_ECHEANCE_1_MOIS").intValue();
            return LocalDate.of(annee, mois, jour);
        } else {
            Integer jour = parametreFiscalService.getValeur("ACOMPTE_IR_ECHEANCE_2").intValue();
            Integer mois = parametreFiscalService.getValeur("ACOMPTE_IR_ECHEANCE_2_MOIS").intValue();
            return LocalDate.of(annee, mois, jour);
        }
    }
}

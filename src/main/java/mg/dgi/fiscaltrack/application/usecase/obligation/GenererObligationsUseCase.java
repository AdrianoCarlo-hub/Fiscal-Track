package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.TypeImpotRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.enums.Periodicite;
import mg.dgi.fiscaltrack.domain.enums.StatutDeclaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.domain.model.TypeImpot;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

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
 */
@Service
public class GenererObligationsUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final TypeImpotRepositoryPort typeImpotRepositoryPort;
    private final CalculerEcheanceReelleUseCase calculerEcheanceReelleUseCase;
    private final ParametreFiscalService parametreFiscalService;

    public GenererObligationsUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                      TypeImpotRepositoryPort typeImpotRepositoryPort,
                                      CalculerEcheanceReelleUseCase calculerEcheanceReelleUseCase,
                                      ParametreFiscalService parametreFiscalService) {
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.typeImpotRepositoryPort = typeImpotRepositoryPort;
        this.calculerEcheanceReelleUseCase = calculerEcheanceReelleUseCase;
        this.parametreFiscalService = parametreFiscalService;
    }

    public ObligationFiscale execute(String nif, String codeImpot, String periodeFiscale) {
        TypeImpot typeImpot = typeImpotRepositoryPort.findByCodeImpot(codeImpot)
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

        LocalDate dateTheorique = calculerDateTheorique(typeImpot, codeImpot, periodeFiscale);
        LocalDate dateReelle = calculerEcheanceReelleUseCase.execute(dateTheorique);

        ObligationFiscale obligation = ObligationFiscale.builder()
                .nif(nif)
                .codeImpot(codeImpot)
                .periodeFiscale(periodeFiscale)
                .dateLimiteTheorique(dateTheorique)
                .dateLimiteReelle(dateReelle)
                .statutDeclaration(StatutDeclaration.ATTENTE)
                .build();

        return obligationRepositoryPort.save(obligation);
    }

    /**
     * Calcule la date theorique selon le type d'impot et la periode.
     * Implemente les regles specifiques pour IR, IS, TVA, IRSA.
     */
    private LocalDate calculerDateTheorique(TypeImpot typeImpot, String codeImpot, String periodeFiscale) {
        // Si TypeImpot a des echeances configurees, les utiliser
        if (typeImpot.getEcheanceTheoriqueJour() != null) {
            return calculerDateTheoriqueDepuisTypeImpot(typeImpot, periodeFiscale);
        }

        // Sinon, utiliser les regles specifiques par type d'impot
        return calculerDateTheoriqueSpecifique(codeImpot, periodeFiscale);
    }

    /**
     * Calcule la date theorique depuis les champs echeanceTheoriqueJour/Mois de TypeImpot.
     */
    private LocalDate calculerDateTheoriqueDepuisTypeImpot(TypeImpot typeImpot, String periodeFiscale) {
        Integer jour = typeImpot.getEcheanceTheoriqueJour();
        Integer mois = typeImpot.getEcheanceTheoriqueMois();

        if (typeImpot.getPeriodicite() == Periodicite.MENSUELLE) {
            // periodeFiscale au format "YYYY-MM" : l'echeance tombe le mois suivant
            String[] parts = periodeFiscale.split("-");
            int annee = Integer.parseInt(parts[0]);
            int moisPeriode = Integer.parseInt(parts[1]);
            int moisEcheance = moisPeriode == 12 ? 1 : moisPeriode + 1;
            int anneeEcheance = moisPeriode == 12 ? annee + 1 : annee;
            return LocalDate.of(anneeEcheance, moisEcheance, jour);
        }

        // Impots annuels : periodeFiscale au format "YYYY"
        int annee = Integer.parseInt(periodeFiscale);
        int moisEcheance = mois != null ? mois : 3;
        return LocalDate.of(annee + 1, moisEcheance, jour);
    }

    /**
     * Calcule la date theorique selon les regles specifiques par type d'impot.
     * Applique les regles CPF et CDI.
     */
    private LocalDate calculerDateTheoriqueSpecifique(String codeImpot, String periodeFiscale) {
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
            return calculerEcheanceIr(periodeFiscale);
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
     * IR : selon date de cloture (CPF I-11).
     * - Exercice civil : 15 mai N+1
     * - Cloture au 30 juin : 15 novembre de la meme annee
     * - Autres clotures : 15e jour du 4e mois suivant la cloture
     * 
     * Pour l'instant, on utilise exercice civil par defaut.
     * La date de cloture doit etre stockee dans le Contribuable.
     */
    private LocalDate calculerEcheanceIr(String periodeFiscale) {
        // periodeFiscale au format "YYYY"
        int annee = Integer.parseInt(periodeFiscale);
        
        // Par defaut : exercice civil -> 15 mai N+1
        Integer jour = parametreFiscalService.getValeur("ECHEANCE_IR_EXERCICE_CIVIL_JOUR").intValue();
        Integer mois = parametreFiscalService.getValeur("ECHEANCE_IR_EXERCICE_CIVIL_MOIS").intValue();
        return LocalDate.of(annee + 1, mois, jour);
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

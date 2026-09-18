package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.TypeImpotRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.Periodicite;
import mg.dgi.fiscaltrack.domain.enums.StatutDeclaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.domain.model.TypeImpot;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class GenererObligationsUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final TypeImpotRepositoryPort typeImpotRepositoryPort;
    private final CalculerEcheanceReelleUseCase calculerEcheanceReelleUseCase;

    public GenererObligationsUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                      TypeImpotRepositoryPort typeImpotRepositoryPort,
                                      CalculerEcheanceReelleUseCase calculerEcheanceReelleUseCase) {
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.typeImpotRepositoryPort = typeImpotRepositoryPort;
        this.calculerEcheanceReelleUseCase = calculerEcheanceReelleUseCase;
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

        LocalDate dateTheorique = calculerDateTheorique(typeImpot, periodeFiscale);
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

    private LocalDate calculerDateTheorique(TypeImpot typeImpot, String periodeFiscale) {
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
}

package mg.dgi.fiscaltrack.interfaces.dto.request;

import mg.dgi.fiscaltrack.domain.model.DeficitReportable;
import mg.dgi.fiscaltrack.domain.model.SaisieDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Transformation du DTO HTTP {@link DeclarationRequest} en objet de domaine
 * {@link SaisieDeclaration} (aucune regle de calcul a ce niveau).
 */
public final class DeclarationRequestMapper {

    private DeclarationRequestMapper() {
    }

    public static SaisieDeclaration toDomain(DeclarationRequest r) {
        if (r == null) {
            return null;
        }

        List<DeficitReportable> deficits = new ArrayList<>();
        if (r.getDeficits() != null) {
            for (DeclarationRequest.DeficitRequest d : r.getDeficits()) {
                if (d == null) continue;
                deficits.add(new DeficitReportable(
                        d.getAnneeOrigine() == null ? 0 : d.getAnneeOrigine(),
                        d.getMontant()));
            }
        }

        List<SaisieDeclaration.LigneSalarie> salaries = new ArrayList<>();
        if (r.getSalaries() != null) {
            for (DeclarationRequest.LigneSalarieRequest l : r.getSalaries()) {
                if (l == null) continue;
                salaries.add(new SaisieDeclaration.LigneSalarie(
                        l.getNom(),
                        l.getSalaireBrut(),
                        l.getCotisationsSociales(),
                        l.getAvantageLogement(),
                        l.getAvantageVehicule(),
                        l.getAvantageTelephone(),
                        l.getPersonnesCharge()));
            }
        }

        return SaisieDeclaration.builder()
                .idObligationFiscale(r.getIdObligationFiscale())
                .impotPrincipalCalcule(r.getImpotPrincipalCalcule())
                .chiffreAffairesDeclare(r.getChiffreAffairesDeclare())
                .acomptes(r.getAcomptes())
                .acomptesDouane(r.getAcomptesDouane())
                .acomptesMarchesPublics(r.getAcomptesMarchesPublics())
                .caBrut(r.getCaBrut())
                .adherentCga(r.getAdherentCga())
                .achatsConformes(r.getAchatsConformes())
                .chargesSalariales(r.getChargesSalariales())
                .categorieActiviteIs(r.getCategorieActiviteIs())
                .caLocal(r.getCaLocal())
                .caExportation(r.getCaExportation())
                .gazButane(r.getGazButane())
                .tvaDeductibleBiens(r.getTvaDeductibleBiens())
                .tvaDeductibleImmobilisations(r.getTvaDeductibleImmobilisations())
                .creditTvaAnterieur(r.getCreditTvaAnterieur())
                .caHT(r.getCaHT())
                .chargesDeductibles(r.getChargesDeductibles())
                .reintegrations(r.getReintegrations())
                .deductions(r.getDeductions())
                .deficits(deficits)
                .categorieActivite(r.getCategorieActivite())
                .moisActifs(r.getMoisActifs())
                .salaries(salaries)
                .build();
    }
}

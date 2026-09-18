package mg.dgi.fiscaltrack.interfaces.controller;

import mg.dgi.fiscaltrack.application.usecase.calendrier.ConsulterCalendrierUseCase;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.interfaces.dto.response.ObligationResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendrier")
public class CalendrierController {

    private final ConsulterCalendrierUseCase consulterUseCase;

    public CalendrierController(ConsulterCalendrierUseCase consulterUseCase) {
        this.consulterUseCase = consulterUseCase;
    }

    /**
     * Retourne toutes les obligations d'un contribuable pour une annee donnee.
     * Utilise par le front-office pour la vue agenda.
     */
    @GetMapping("/{nif}")
    public List<ObligationResponse> calendrierParAnnee(@PathVariable String nif,
                                                        @RequestParam(required = false) Integer annee) {
        int anneeCible = annee != null ? annee : LocalDate.now().getYear();
        return consulterUseCase.parContribuableEtAnnee(nif, anneeCible)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Retourne les obligations dont l'echeance approche dans les N prochains jours.
     */
    @GetMapping("/{nif}/prochaines")
    public List<ObligationResponse> prochainesEcheances(@PathVariable String nif,
                                                         @RequestParam(defaultValue = "7") int jours) {
        return consulterUseCase.echeancesProchaines(nif, jours)
                .stream().map(this::toResponse).toList();
    }

    private ObligationResponse toResponse(ObligationFiscale o) {
        return ObligationResponse.builder()
                .idObligationFiscale(o.getIdObligationFiscale())
                .nif(o.getNif())
                .codeImpot(o.getCodeImpot())
                .idAgent(o.getIdAgent())
                .idJourFerie(o.getIdJourFerie())
                .periodeFiscale(o.getPeriodeFiscale())
                .dateLimiteTheorique(o.getDateLimiteTheorique())
                .dateLimiteReelle(o.getDateLimiteReelle())
                .statutDeclaration(o.getStatutDeclaration() == null ? null : o.getStatutDeclaration().name())
                .dateDepotEffectif(o.getDateDepotEffectif())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}

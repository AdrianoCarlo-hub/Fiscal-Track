package mg.dgi.fiscaltrack.interfaces.controller;

import mg.dgi.fiscaltrack.application.port.out.HistoriqueActionRepositoryPort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/historique")
@PreAuthorize("hasRole('ADMIN')")
public class HistoriqueController {

    private final HistoriqueActionRepositoryPort historiqueRepositoryPort;

    public HistoriqueController(HistoriqueActionRepositoryPort historiqueRepositoryPort) {
        this.historiqueRepositoryPort = historiqueRepositoryPort;
    }

    @GetMapping
    public List<Map<String, Object>> lister() {
        return historiqueRepositoryPort.findAll().stream().map(this::toMap).toList();
    }

    @GetMapping("/utilisateur/{utilisateur}")
    public List<Map<String, Object>> parUtilisateur(@PathVariable String utilisateur) {
        return historiqueRepositoryPort.findByUtilisateur(utilisateur).stream().map(this::toMap).toList();
    }

    private Map<String, Object> toMap(HistoriqueActionRepositoryPort.HistoriqueAction h) {
        return Map.of(
                "idHistorique", h.idHistorique(),
                "utilisateur", h.utilisateur(),
                "action", h.action(),
                "details", h.details() == null ? "" : h.details(),
                "createdAt", h.createdAt().toString()
        );
    }
}

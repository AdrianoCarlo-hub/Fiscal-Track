package mg.dgi.fiscaltrack.interfaces.controller;

import jakarta.validation.Valid;
import mg.dgi.fiscaltrack.application.usecase.auth.AuthentifierAgentUseCase;
import mg.dgi.fiscaltrack.application.usecase.auth.AuthentifierContribuableUseCase;
import mg.dgi.fiscaltrack.domain.model.AgentFiscal;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.infrastructure.security.JwtService;
import mg.dgi.fiscaltrack.interfaces.dto.request.LoginRequest;
import mg.dgi.fiscaltrack.interfaces.dto.response.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthentifierContribuableUseCase authentifierContribuableUseCase;
    private final AuthentifierAgentUseCase authentifierAgentUseCase;
    private final JwtService jwtService;
    private final long expirationMs;

    public AuthController(AuthentifierContribuableUseCase authentifierContribuableUseCase,
                           AuthentifierAgentUseCase authentifierAgentUseCase,
                           JwtService jwtService,
                           @Value("${jwt.expiration}") long expirationMs) {
        this.authentifierContribuableUseCase = authentifierContribuableUseCase;
        this.authentifierAgentUseCase = authentifierAgentUseCase;
        this.jwtService = jwtService;
        this.expirationMs = expirationMs;
    }

    /**
     * Endpoint de connexion. L'identifiant peut etre un NIF ou un email agent.
     * Le type de retour depend de l'utilisateur trouve.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Un identifiant agent est un email (contient '@') ; sinon c'est un NIF
        // contribuable. On ne bascule PAS d'un type a l'autre en cas d'echec :
        // un mauvais mot de passe doit renvoyer 401 (et non 404 contribuable).
        if (request.getIdentifiant() != null && request.getIdentifiant().contains("@")) {
            AgentFiscal agent = authentifierAgentUseCase.execute(
                    request.getIdentifiant(), request.getMotDePasse());
            String token = jwtService.genererToken(
                    agent.getIdAgent(),
                    agent.getRoleSecurite().name(),
                    "AGENT");
            return ResponseEntity.ok(LoginResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .identifiant(agent.getIdAgent())
                    .role(agent.getRoleSecurite().name())
                    .expirationMs(expirationMs)
                    .build());
        }

        Contribuable contribuable = authentifierContribuableUseCase.execute(
                request.getIdentifiant(), request.getMotDePasse());
        String token = jwtService.genererToken(
                contribuable.getNif(),
                "CONTRIBUABLE",
                "CONTRIBUABLE");
        return ResponseEntity.ok(LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .identifiant(contribuable.getNif())
                .role("CONTRIBUABLE")
                .expirationMs(expirationMs)
                .build());
    }
}

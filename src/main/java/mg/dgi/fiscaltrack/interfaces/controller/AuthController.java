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
        // Tentative cote agent (email)
        var agentOpt = tryAuthentifierAgent(request.getIdentifiant(), request.getMotDePasse());
        if (agentOpt != null) {
            String token = jwtService.genererToken(
                    agentOpt.getIdAgent(),
                    agentOpt.getRoleSecurite().name(),
                    "AGENT");
            return ResponseEntity.ok(LoginResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .identifiant(agentOpt.getIdAgent())
                    .role(agentOpt.getRoleSecurite().name())
                    .expirationMs(expirationMs)
                    .build());
        }

        // Sinon cote contribuable (NIF)
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

    private AgentFiscal tryAuthentifierAgent(String identifiant, String motDePasse) {
        if (!identifiant.contains("@")) {
            return null;
        }
        try {
            return authentifierAgentUseCase.execute(identifiant, motDePasse);
        } catch (Exception e) {
            return null;
        }
    }
}

package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerAcompteUseCase;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerFiscalDispatcherUseCase;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Recalcul serveur de l'impot du a la declaration (point 2.8).
 *
 * Le montant saisi par le contribuable ne fait plus foi : le serveur
 * recalcule l'impot via le dispatcher fiscal a partir du chiffre d'affaires
 * declare. Le resultat est presente sous forme de {@link ResultatCalculFiscal}
 * pour permettre la comparaison et la journalisation d'un ecart.
 *
 * Seuls les impots dont le moteur se base sur le chiffre d'affaires sont
 * recalculables ici (IR, IS, IRCM, TVA). Pour les autres (IRSA, IPVI, IMP,
 * acomptes AC1/AC2), le recalcul est ignore (Optional.empty()) et le
 * montant soumis est conserve : voir QUESTIONS_ENCADREUR.md.
 */
@Service
public class CalculerImpotDeclarationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CalculerImpotDeclarationUseCase.class);

    private final CalculerFiscalDispatcherUseCase calculerFiscalDispatcherUseCase;
    private final CalculerAcompteUseCase calculerAcompteUseCase;
    private final DeclarationRepositoryPort declarationRepositoryPort;

    public CalculerImpotDeclarationUseCase(CalculerFiscalDispatcherUseCase calculerFiscalDispatcherUseCase,
                                           CalculerAcompteUseCase calculerAcompteUseCase,
                                           DeclarationRepositoryPort declarationRepositoryPort) {
        this.calculerFiscalDispatcherUseCase = calculerFiscalDispatcherUseCase;
        this.calculerAcompteUseCase = calculerAcompteUseCase;
        this.declarationRepositoryPort = declarationRepositoryPort;
    }

    /**
     * Recalcule l'impot selon le code impot, le chiffre d'affaires declare
     * et, si necessaire, le NIF (pour les acomptes bases sur l'IR N-1).
     *
     * @return le resultat du moteur, ou Optional.empty() si le type d'impot
     *         n'est pas recalculable.
     */
    public Optional<ResultatCalculFiscal> execute(String codeImpot, BigDecimal chiffreAffaires, String nif) {
        if (codeImpot != null && codeImpot.equalsIgnoreCase("ACOMPTE_IR")) {
            return calculerAcompte(nif);
        }
        return execute(codeImpot, chiffreAffaires);
    }

    /**
     * Recalcule l'impot selon le code impot et le chiffre d'affaires declare.
     *
     * @return le resultat du moteur, ou Optional.empty() si le type d'impot
     *         n'est pas recalculable a partir du seul chiffre d'affaires.
     */
    public Optional<ResultatCalculFiscal> execute(String codeImpot, BigDecimal chiffreAffaires) {
        String cleCa = cleChiffreAffaires(codeImpot);
        if (cleCa == null) {
            return Optional.empty();
        }

        Map<String, Object> params = new HashMap<>();
        params.put(cleCa, chiffreAffaires);

        try {
            return Optional.of(calculerFiscalDispatcherUseCase.execute(codeImpot, params));
        } catch (RuntimeException e) {
            logger.warn("[DECLARATION] Recalcul serveur impossible pour le code impot {} : {}",
                    codeImpot, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Acompte provisionnel IR : 25 % de l'IR N-1 (CDI / Document IR 2026).
     * La base est l'impot de la derniere declaration IR du NIF.
     */
    private Optional<ResultatCalculFiscal> calculerAcompte(String nif) {
        if (nif == null) {
            return Optional.empty();
        }
        return declarationRepositoryPort.findDerniereDeclarationByNifEtImpot(nif, "IR")
                .map(d -> d.getImpotPrincipalCalcule())
                .filter(impotN1 -> impotN1 != null && impotN1.signum() >= 0)
                .map(impotN1 -> {
                    BigDecimal montant = calculerAcompteUseCase.calculerAcompteProvisionnel(impotN1);
                    return ResultatCalculFiscal.builder()
                            .typeImpot("ACOMPTE_IR")
                            .baseImposable(impotN1)
                            .montantBrut(montant)
                            .solde(montant)
                            .explication("Acompte provisionnel IR : 25 % de l'IR N-1 (" + impotN1 + " Ar)")
                            .build();
                });
    }

    private String cleChiffreAffaires(String codeImpot) {
        if (codeImpot == null) {
            return null;
        }
        return switch (codeImpot.toUpperCase()) {
            case "IR" -> "caHT";
            case "IS", "IRCM" -> "caBrut";
            case "TVA" -> "chiffreAffaires";
            default -> null;
        };
    }
}

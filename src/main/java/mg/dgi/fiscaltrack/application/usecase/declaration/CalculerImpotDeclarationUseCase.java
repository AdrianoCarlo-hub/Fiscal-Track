package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerAcompteUseCase;
import mg.dgi.fiscaltrack.application.usecase.calculfiscal.CalculerFiscalDispatcherUseCase;
import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import mg.dgi.fiscaltrack.domain.exception.TypeImpotNonSupporteException;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * recalculables ici (IR, IS, IRCM, TVA). Pour les autres (IPVI, IMP,
 * acomptes AC1/AC2 hors IR), le recalcul est ignore (Optional.empty()) et le
 * montant soumis est conserve : voir QUESTIONS_ENCADREUR.md.
 *
 * Depuis « formulaires-et-ux », {@link #executeAvecParametres} prend en charge
 * les formulaires adosses a chaque impot (IS, TVA, IR, IRSA, ACOMPTE_IR) :
 * les parametres viennent du formulaire du contribuable, le calcul reste
 * exclusivement assure par le dispatcher et les calculateurs existants.
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
     * Recalcule l'impot a partir des parametres fournis par
     * {@link PreparerDonneesDeclarationUseCase} (champs du formulaire adapte
     * au type d'impot). Le dispatcher et les calculateurs sont inchanges.
     *
     * IRSA : chaque ligne salarie est calculee separement puis les soldes
     * sont additionnes ; le detail de chaque ligne est expose dans
     * {@code ResultatCalculFiscal.etapesCalcul}.
     *
     * @return le resultat du moteur, ou Optional.empty() si le type n'est pas
     *         recalculable (ex. acompte IR sans historique).
     */
    public Optional<ResultatCalculFiscal> executeAvecParametres(String codeImpot,
                                                                Map<String, Object> params,
                                                                String nif) {
        if (codeImpot != null && codeImpot.equalsIgnoreCase("ACOMPTE_IR")) {
            return calculerAcompte(nif);
        }
        if (codeImpot != null && codeImpot.equalsIgnoreCase("IRSA")) {
            return calculerIrsaMultiLignes(params);
        }
        if (codeImpot == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(calculerFiscalDispatcherUseCase.execute(codeImpot, params));
        } catch (TypeImpotNonSupporteException e) {
            throw e;
        } catch (RuntimeException e) {
            logger.warn("[DECLARATION] Recalcul serveur impossible pour le code impot {} : {}",
                    codeImpot, e.getMessage());
            throw new CalculFiscalInvalideException(
                    e.getMessage() != null ? e.getMessage()
                            : "Calcul impossible pour le type d'impot " + codeImpot);
        }
    }

    /**
     * Calcule l'IRSA de chaque ligne salarie avec le calculateur existant et
     * renvoie le total comme impot principal de la declaration.
     */
    @SuppressWarnings("unchecked")
    private Optional<ResultatCalculFiscal> calculerIrsaMultiLignes(Map<String, Object> params) {
        Object brut = params.get("salaries");
        if (!(brut instanceof List<?> lignes) || lignes.isEmpty()) {
            throw new CalculFiscalInvalideException(
                    "Au moins un salarie est obligatoire pour declarer l'IRSA");
        }

        List<String> etapesDetail = new ArrayList<>();
        List<String> avertissements = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal baseTotale = BigDecimal.ZERO;
        int numero = 1;

        for (Object element : lignes) {
            if (!(element instanceof Map<?, ?> ligne)) {
                continue;
            }
            Map<String, Object> parametresLigne = (Map<String, Object>) ligne.get("parametres");
            Object nomBrut = ligne.get("nom");
            String nom = nomBrut == null || String.valueOf(nomBrut).isBlank()
                    ? "Salarie " + numero : String.valueOf(nomBrut);

            ResultatCalculFiscal resultatLigne =
                    calculerFiscalDispatcherUseCase.execute("IRSA", parametresLigne);

            BigDecimal soldeLigne = resultatLigne.getSolde() == null
                    ? BigDecimal.ZERO : resultatLigne.getSolde();
            total = total.add(soldeLigne);
            if (resultatLigne.getBaseImposable() != null) {
                baseTotale = baseTotale.add(resultatLigne.getBaseImposable());
            }

            etapesDetail.add(nom + " : " + soldeLigne + " Ar"
                    + " (base " + resultatLigne.getBaseImposable() + " Ar)");
            etapesDetail.addAll(resultatLigne.getEtapesCalcul());
            avertissements.addAll(resultatLigne.getAvertissements());
            numero++;
        }

        List<String> etapes = new ArrayList<>();
        etapes.add("Nombre de salaries : " + lignes.size());
        etapes.add("Base imposable totale : " + baseTotale + " Ar");
        etapes.addAll(etapesDetail);
        etapes.add("Total IRSA a payer : " + total + " Ar");

        return Optional.of(ResultatCalculFiscal.builder()
                .typeImpot("IRSA")
                .baseImposable(baseTotale)
                .montantBrut(total)
                .solde(total)
                .explication("IRSA = somme des IRSA de chaque salarie")
                .etapesCalcul(etapes)
                .avertissements(avertissements)
                .build());
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

package mg.dgi.fiscaltrack.application.usecase.admin;

import mg.dgi.fiscaltrack.application.port.out.ParametreFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.model.ParametreFiscal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Modification d'un parametre fiscal (page ADMIN), avec tracabilite d'audit.
 *
 * Patch strict : seuls les champs non-null fournis sont ecrits. Le code
 * (identifiant) n'est jamais modifiable.
 */
@Service
public class ModifierParametreFiscalUseCase {

    private final ParametreFiscalRepositoryPort repositoryPort;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public ModifierParametreFiscalUseCase(ParametreFiscalRepositoryPort repositoryPort,
                                          EnregistrerActionUseCase enregistrerActionUseCase) {
        this.repositoryPort = repositoryPort;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    @Transactional
    public ParametreFiscal execute(String code, BigDecimal valeur, Integer anneeDebut,
                                   Integer anneeFin, String baseLegale, String secteur,
                                   String description, String auteur) {
        if (valeur == null) {
            throw new IllegalArgumentException("La valeur du parametre est obligatoire");
        }
        if (anneeDebut != null && anneeFin != null && anneeFin < anneeDebut) {
            throw new IllegalArgumentException(
                    "L'annee de fin ne peut pas preceder l'annee de debut");
        }

        ParametreFiscal existant = repositoryPort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Parametre fiscal introuvable : " + code));

        BigDecimal ancienneValeur = existant.getValeur();
        existant.setValeur(valeur);
        if (anneeDebut != null) {
            existant.setAnneeDebut(java.time.Year.of(anneeDebut));
        }
        if (anneeFin != null) {
            existant.setAnneeFin(java.time.Year.of(anneeFin));
        }
        if (baseLegale != null) {
            existant.setBaseLegale(baseLegale);
        }
        if (secteur != null) {
            existant.setSecteur(secteur);
        }
        if (description != null) {
            existant.setDescription(description);
        }

        ParametreFiscal saved = repositoryPort.save(existant);

        enregistrerActionUseCase.execute(
                auteur == null ? "SYSTEME" : auteur,
                "MODIFICATION_PARAMETRE_FISCAL",
                "Parametre " + code + " : " + ancienneValeur + " -> " + valeur);
        return saved;
    }
}

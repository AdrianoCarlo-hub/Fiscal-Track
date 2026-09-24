package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CompteCourantFiscalRepositoryPort {

    CompteCourantFiscal save(CompteCourantFiscal compteCourantFiscal);

    Optional<CompteCourantFiscal> findById(Long id);

    Optional<CompteCourantFiscal> findByIdDeclaration(Long idDeclaration);

    List<CompteCourantFiscal> findAll();

    List<CompteCourantFiscal> findByNif(String nif);

    List<CompteCourantFiscal> findByStatut(String statutRecouvrement);

    List<CompteCourantFiscal> findNonSoldes();

    void deleteById(Long id);

    /**
     * Comptes eligibles a l'emission d'un titre de perception :
     * statut NON_SOLDE ou PARTIEL, avec une obligation dont la date limite
     * reelle est anterieure au seuil fourni.
     */
    List<CompteCourantFiscal> findEligiblesTitrePerception(LocalDate seuilEcheance);

    /**
     * Comptes eligibles au lancement d'un ATD : statut TITRE_EMIS, dont le
     * dernier changement de statut (updated_at) est anterieur au seuil fourni.
     */
    List<CompteCourantFiscal> findEligiblesAtd(LocalDate seuilUpdatedAt);
}

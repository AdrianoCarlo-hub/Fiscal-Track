package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.ParametreFiscal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Port pour l'acces aux parametres fiscaux.
 * Toutes les constantes fiscales (taux, seuils, minima, plafonds) doivent passer par ce port.
 */
public interface ParametreFiscalRepositoryPort {

    /**
     * Trouve un parametre par son code.
     */
    Optional<ParametreFiscal> findByCode(String code);

    /**
     * Recupere la valeur d'un parametre par son code.
     * @throws IllegalArgumentException si le parametre n'existe pas
     */
    BigDecimal getValeurByCode(String code);

    /**
     * Trouve tous les parametres applicables pour une annee donnee.
     */
    List<ParametreFiscal> findApplicablesPourAnnee(int annee);

    /**
     * Trouve tous les parametres par secteur.
     */
    List<ParametreFiscal> findBySecteur(String secteur);

    /**
     * Trouve tous les parametres par base legale.
     */
    List<ParametreFiscal> findByBaseLegale(String baseLegale);

    /**
     * Trouve tous les parametres par type (prefixe de code).
     */
    List<ParametreFiscal> findByCodePrefix(String prefix);

    /**
     * Sauve un parametre fiscal.
     */
    ParametreFiscal save(ParametreFiscal parametre);

    /**
     * Supprime un parametre fiscal.
     */
    void delete(ParametreFiscal parametre);
}

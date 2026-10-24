package mg.dgi.fiscaltrack.application.usecase.admin;

import mg.dgi.fiscaltrack.application.port.out.ParametreFiscalRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.ParametreFiscal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service pour acceder aux parametres fiscaux.
 * Centralise l'acces aux parametres pour simplifier leur utilisation dans les use cases.
 */
@Service
public class ParametreFiscalService {

    private final ParametreFiscalRepositoryPort parametreFiscalRepositoryPort;

    public ParametreFiscalService(ParametreFiscalRepositoryPort parametreFiscalRepositoryPort) {
        this.parametreFiscalRepositoryPort = parametreFiscalRepositoryPort;
    }

    /**
     * Recupere la valeur d'un parametre fiscal.
     * @param code le code du parametre
     * @return la valeur du parametre
     * @throws IllegalArgumentException si le parametre n'existe pas
     */
    public BigDecimal getValeur(String code) {
        return parametreFiscalRepositoryPort.getValeurByCode(code);
    }

    public Optional<ParametreFiscal> getParametre(String code) {
        return parametreFiscalRepositoryPort.findByCode(code);
    }

    /**
     * Parametres dont la base legale est explicitement marquee « À CONFIRMER ».
     * Ces valeurs (ex. seuils de regime IR 20 M / IS synthetique 200 M en conflit
     * entre sources) ne doivent servir qu'a emettre un avertissement, jamais a
     * decider automatiquement d'un regime ni d'un calcul.
     */
    public List<ParametreFiscal> getParametresAConfirmer() {
        return parametreFiscalRepositoryPort.findByBaseLegale("À CONFIRMER");
    }

    /**
     * Recupere la valeur d'un parametre fiscal avec une valeur par defaut.
     * @param code le code du parametre
     * @param defaultValue la valeur par defaut si le parametre n'existe pas
     * @return la valeur du parametre ou la valeur par defaut
     */
    public BigDecimal getValeur(String code, BigDecimal defaultValue) {
        try {
            return getValeur(code);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}

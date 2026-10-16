package mg.dgi.fiscaltrack.application.usecase.penalite;

import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Calcul de l'amende pour defaut de depot (distinct de la penalite de retard).
 * CDI 20.01.52 :
 *   - CA >= 400M : 200 000 Ar
 *   - 200M <= CA < 400M : 100 000 Ar
 *   - 50M <= CA < 200M : 50 000 Ar (et associations/organismes sans but lucratif)
 *   - CA < 50M : 20 000 Ar
 * 
 * Cette amende est creee quand une obligation est echue sans declaration.
 * Le scheduler de retard doit appeller ce use case pour creer cette amende.
 */
@Service
public class CalculerAmendeDefautDepotUseCase {

    private final DeclarationRepositoryPort declarationRepositoryPort;
    private final ParametreFiscalService parametreFiscalService;

    public CalculerAmendeDefautDepotUseCase(DeclarationRepositoryPort declarationRepositoryPort,
                                            ParametreFiscalService parametreFiscalService) {
        this.declarationRepositoryPort = declarationRepositoryPort;
        this.parametreFiscalService = parametreFiscalService;
    }

    /**
     * Calcule le montant de l'amende pour defaut de depot.
     * 
     * @param idDeclaration l'ID de la declaration en defaut
     * @return le montant de l'amende
     */
    public BigDecimal execute(Long idDeclaration) {
        Declaration declaration = declarationRepositoryPort.findById(idDeclaration)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Declaration introuvable : " + idDeclaration));

        BigDecimal chiffreAffaires = declaration.getChiffreAffairesDeclare();
        if (chiffreAffaires == null) {
            chiffreAffaires = BigDecimal.ZERO;
        }

        return determinerAmende(chiffreAffaires);
    }

    /**
     * Calcule le montant de l'amende selon le CA.
     * CDI 20.01.52 :
     *   - CA >= 400M : 200 000 Ar
     *   - 200M <= CA < 400M : 100 000 Ar
     *   - 50M <= CA < 200M : 50 000 Ar
     *   - CA < 50M : 20 000 Ar
     *
     * Les seuils de l'amende sont distincts de ceux de la penalite de
     * retard (codes AMENDE_SEUIL_* vs PENALITE_SEUIL_*).
     */
    private BigDecimal determinerAmende(BigDecimal chiffreAffaires) {
        BigDecimal seuil400M = parametreFiscalService.getValeur("AMENDE_SEUIL_GRANDE_ENTREPRISE");
        BigDecimal seuil200M = parametreFiscalService.getValeur("AMENDE_SEUIL_MOYENNE_ENTREPRISE");
        BigDecimal seuil50M = parametreFiscalService.getValeur("AMENDE_SEUIL_PETITE_ENTREPRISE");

        if (chiffreAffaires.compareTo(seuil400M) >= 0) {
            return parametreFiscalService.getValeur("DEFAUT_DEPOT_GRANDE_ENTREPRISE");
        }
        if (chiffreAffaires.compareTo(seuil200M) >= 0) {
            return parametreFiscalService.getValeur("DEFAUT_DEPOT_MOYENNE_ENTREPRISE");
        }
        if (chiffreAffaires.compareTo(seuil50M) >= 0) {
            return parametreFiscalService.getValeur("DEFAUT_DEPOT_PETITE_ENTREPRISE");
        }
        return parametreFiscalService.getValeur("DEFAUT_DEPOT_MICRO_ENTREPRISE");
    }
}

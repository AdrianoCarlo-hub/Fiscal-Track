package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.CanalEnvoi;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Determination des canaux d'envoi pour un contribuable.
 *
 * Regles :
 * - Le canal prefere du contribuable est prioritaire.
 * - Un canal sans coordonnee (e-mail/telephone manquant) ou refuse
 *   (accepte_email / accepte_sms) n'est jamais utilise.
 * - Les avis legaux (retard, relance, penalite, amende) sont TOUJOURS
 *   publies sur le portail.
 * - Si aucun canal externe n'est disponible, le portail est retenu
 *   (PORTAIL seulement, sans erreur).
 */
@Service
public class CanalNotificationService {

    private final ContribuableRepositoryPort contribuableRepositoryPort;

    public CanalNotificationService(ContribuableRepositoryPort contribuableRepositoryPort) {
        this.contribuableRepositoryPort = contribuableRepositoryPort;
    }

    public Optional<Contribuable> contribuable(String nif) {
        return contribuableRepositoryPort.findByNif(nif);
    }

    /**
     * Canaux pour une generation automatique.
     *
     * @param contribuable contribuable cible (peut etre absent)
     * @param avisLegal    true pour les notifications a portee legale
     */
    public List<CanalEnvoi> canauxPour(Contribuable contribuable, boolean avisLegal) {
        List<CanalEnvoi> canaux = new ArrayList<>();

        if (contribuable == null) {
            canaux.add(CanalEnvoi.PORTAIL);
            return canaux;
        }

        CanalEnvoi prefere = contribuable.getCanalPrefere();
        boolean emailDisponible = estEmailUtilisable(contribuable);
        boolean smsDisponible = estSmsUtilisable(contribuable);

        if (prefere == CanalEnvoi.EMAIL) {
            if (emailDisponible) {
                canaux.add(CanalEnvoi.EMAIL);
            }
        } else if (prefere == CanalEnvoi.SMS) {
            if (smsDisponible) {
                canaux.add(CanalEnvoi.SMS);
            }
        } else if (prefere == CanalEnvoi.PORTAIL) {
            // volonte explicite : portail uniquement
        } else {
            // aucune preference : tous les canaux disponibles
            if (emailDisponible) {
                canaux.add(CanalEnvoi.EMAIL);
            }
            if (smsDisponible) {
                canaux.add(CanalEnvoi.SMS);
            }
        }

        if (avisLegal || canaux.isEmpty()) {
            canaux.add(CanalEnvoi.PORTAIL);
        }
        return canaux;
    }

    public boolean estEmailUtilisable(Contribuable contribuable) {
        return contribuable != null
                && !Boolean.FALSE.equals(contribuable.getAccepteEmail())
                && contribuable.getEmailContribuable() != null
                && !contribuable.getEmailContribuable().isBlank();
    }

    public boolean estSmsUtilisable(Contribuable contribuable) {
        return contribuable != null
                && !Boolean.FALSE.equals(contribuable.getAccepteSms())
                && contribuable.getTelephoneContribuable() != null
                && !contribuable.getTelephoneContribuable().isBlank();
    }

    /** Le contribuable refuse ce canal (preferences). */
    public boolean refuse(Contribuable contribuable, CanalEnvoi canal) {
        if (contribuable == null || canal == null) {
            return false;
        }
        if (canal == CanalEnvoi.EMAIL) {
            return Boolean.FALSE.equals(contribuable.getAccepteEmail());
        }
        if (canal == CanalEnvoi.SMS) {
            return Boolean.FALSE.equals(contribuable.getAccepteSms());
        }
        return false;
    }
}

package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.domain.enums.CanalEnvoi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Parametres d'envoi des notifications, lus dans l'environnement
 * (jamais de constante en dur pour les tentatives).
 */
@Service
public class NotificationParametresService {

    private final int nbTentativesMax;
    private final List<Integer> delaisTentativesMinutes;
    private final String fuseau;

    public NotificationParametresService(
            @Value("${NOTIFICATIONS_MAX_TENTATIVES:3}") int nbTentativesMax,
            @Value("${NOTIFICATIONS_DELAI_TENTATIVES_MINUTES:5,30}") String delais,
            @Value("${APP_TIMEZONE:Indian/Antananarivo}") String fuseau) {
        this.nbTentativesMax = Math.max(1, nbTentativesMax);
        this.delaisTentativesMinutes = Arrays.stream(delais.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();
        this.fuseau = fuseau;
    }

    /** Nombre maximum de tentatives (la retenue incluse). */
    public int getNbTentativesMax() {
        return nbTentativesMax;
    }

    /**
     * Delai d'attente (en minutes) avant la tentative n (1-indexe) :
     * delais.get(n - 1) si defini, sinon le dernier delai connu.
     */
    public int delaiAvantTentative(int numeroTentative) {
        if (delaisTentativesMinutes.isEmpty()) {
            return 5;
        }
        int index = Math.min(Math.max(numeroTentative - 1, 0), delaisTentativesMinutes.size() - 1);
        return delaisTentativesMinutes.get(index);
    }

    public String getFuseau() {
        return fuseau;
    }

    /** Les canaux acceptes pour un envoi manuel : EMAIL, SMS, PORTAIL. */
    public static boolean canalValide(String canal) {
        return Arrays.stream(CanalEnvoi.values()).anyMatch(c -> c.name().equals(canal));
    }
}

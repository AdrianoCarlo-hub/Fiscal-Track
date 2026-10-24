package mg.dgi.fiscaltrack.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Configuration de l'envoi asynchrone des notifications et de l'horloge
 * de reference (fuseau de Madagascar).
 */
@Configuration
@EnableAsync
public class AsyncNotificationConfig {

    private static final Logger logger = LoggerFactory.getLogger(AsyncNotificationConfig.class);

    /**
     * Exécuteur dedie a l'envoi (e-mail / SMS). File bornee : si la file est
     * pleine, la tache est rejettee et la notification reste EN_ATTENTE
     * (aucune perte, aucune exception remontee au demandeur).
     */
    @Bean("notificationTaskExecutor")
    public ThreadPoolTaskExecutor notificationTaskExecutor(
            @Value("${NOTIFICATIONS_ASYNC_CORE_POOL_SIZE:2}") int corePoolSize,
            @Value("${NOTIFICATIONS_ASYNC_MAX_POOL_SIZE:4}") int maxPoolSize,
            @Value("${NOTIFICATIONS_ASYNC_QUEUE_CAPACITY:100}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, corePoolSize));
        executor.setMaxPoolSize(Math.max(1, maxPoolSize));
        executor.setQueueCapacity(Math.max(1, queueCapacity));
        executor.setThreadNamePrefix("notif-envoi-");
        executor.setRejectedExecutionHandler((runnable, executorRef) ->
                logger.warn("[NOTIFICATION][ASYNC] File d'attente pleine ; "
                        + "notification conservee en attente d'une prochaine execution"));
        executor.initialize();
        return executor;
    }

    /**
     * Horloge injectable (testable). Le fuseau par defaut est celui de
     * Madagascar : les calculs J-7 / J-1 / J+1 / J+8 se font sur la date
     * locale de ce fuseau.
     */
    @Bean
    public Clock clock(@Value("${APP_TIMEZONE:Indian/Antananarivo}") String fuseau) {
        return Clock.system(ZoneId.of(fuseau));
    }
}

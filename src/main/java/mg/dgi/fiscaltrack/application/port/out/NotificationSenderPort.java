package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.Notification;

public interface NotificationSenderPort {

    void envoyer(Notification notification);

    boolean supporterCanal(String canalEnvoi);
}

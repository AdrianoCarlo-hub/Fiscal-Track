package mg.dgi.fiscaltrack.infrastructure.persistence.mapper;

import mg.dgi.fiscaltrack.domain.enums.CanalEnvoi;
import mg.dgi.fiscaltrack.domain.enums.StatutEnvoi;
import mg.dgi.fiscaltrack.domain.enums.TypeRelance;
import mg.dgi.fiscaltrack.domain.model.Notification;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public Notification toDomain(NotificationEntity e) {
        if (e == null) {
            return null;
        }
        return Notification.builder()
                .idNotif(e.getIdNotif())
                .idObligationFiscale(e.getIdObligationFiscale())
                .idCompte(e.getIdCompte())
                .nif(e.getNif())
                .idAgent(e.getIdAgent())
                .typeRelance(parseTypeRelance(e.getTypeRelance()))
                .messageContenu(e.getMessageContenu())
                .dateEnvoiPrevue(e.getDateEnvoiPrevue())
                .dateEnvoiEffective(e.getDateEnvoiEffective())
                .canalEnvoi(parseCanalEnvoi(e.getCanalEnvoi()))
                .statutEnvoi(parseStatutEnvoi(e.getStatutEnvoi()))
                .createdAt(e.getCreatedAt())
                .build();
    }

    public NotificationEntity toEntity(Notification d) {
        if (d == null) {
            return null;
        }
        return NotificationEntity.builder()
                .idNotif(d.getIdNotif())
                .idObligationFiscale(d.getIdObligationFiscale())
                .idCompte(d.getIdCompte())
                .nif(d.getNif())
                .idAgent(d.getIdAgent())
                .typeRelance(d.getTypeRelance() == null ? null : d.getTypeRelance().getCode())
                .messageContenu(d.getMessageContenu())
                .dateEnvoiPrevue(d.getDateEnvoiPrevue())
                .dateEnvoiEffective(d.getDateEnvoiEffective())
                .canalEnvoi(d.getCanalEnvoi() == null ? null : d.getCanalEnvoi().name())
                .statutEnvoi(d.getStatutEnvoi() == null ? null : d.getStatutEnvoi().name())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private TypeRelance parseTypeRelance(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TypeRelance.fromCode(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private CanalEnvoi parseCanalEnvoi(String value) {
        if (value == null) {
            return null;
        }
        try {
            return CanalEnvoi.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private StatutEnvoi parseStatutEnvoi(String value) {
        if (value == null) {
            return null;
        }
        try {
            return StatutEnvoi.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

package mg.dgi.fiscaltrack.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notif")
    private Long idNotif;

    @Column(name = "id_obligation_fiscale")
    private Long idObligationFiscale;

    @Column(name = "id_compte")
    private Long idCompte;

    @Column(name = "nif", nullable = false, length = 10)
    private String nif;

    @Column(name = "id_agent", length = 6)
    private String idAgent;

    @Column(name = "type_relance", nullable = false)
    private String typeRelance;

    @Column(name = "message_contenu", nullable = false, columnDefinition = "TEXT")
    private String messageContenu;

    @Column(name = "date_envoi_prevue", nullable = false)
    private OffsetDateTime dateEnvoiPrevue;

    @Column(name = "date_envoi_effective")
    private OffsetDateTime dateEnvoiEffective;

    @Column(name = "canal_envoi", nullable = false)
    private String canalEnvoi;

    @Column(name = "statut_envoi", nullable = false)
    private String statutEnvoi;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

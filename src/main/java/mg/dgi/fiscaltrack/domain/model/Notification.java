package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.dgi.fiscaltrack.domain.enums.CanalEnvoi;
import mg.dgi.fiscaltrack.domain.enums.StatutEnvoi;
import mg.dgi.fiscaltrack.domain.enums.TypeRelance;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    private Long idNotif;
    private Long idObligationFiscale;
    private Long idCompte;
    private String nif;
    private String idAgent;
    private TypeRelance typeRelance;
    private String messageContenu;
    private OffsetDateTime dateEnvoiPrevue;
    private OffsetDateTime dateEnvoiEffective;
    private CanalEnvoi canalEnvoi;
    private StatutEnvoi statutEnvoi;
    private OffsetDateTime createdAt;
}

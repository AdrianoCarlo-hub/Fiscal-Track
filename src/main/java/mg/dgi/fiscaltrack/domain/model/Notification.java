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
    /** Texte SMS genere (160 caracteres max, sans accent), conserve pour tracabilite. */
    private String messageSms;
    private OffsetDateTime dateEnvoiPrevue;
    private OffsetDateTime dateEnvoiEffective;
    private CanalEnvoi canalEnvoi;
    private StatutEnvoi statutEnvoi;
    /** Nombre de tentatives d'envoi effectuees (1 apres la premiere). */
    private Integer nbTentatives;
    /** Date de la derniere tentative d'envoi. */
    private OffsetDateTime derniereTentative;
    /** Message court d'erreur (jamais de stack trace ni de secret). */
    private String erreurEnvoi;
    /** Consultation portail : lu par le contribuable. */
    private Boolean lu;
    private OffsetDateTime createdAt;
}

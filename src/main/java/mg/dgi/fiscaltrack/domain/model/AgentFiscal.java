package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.dgi.fiscaltrack.domain.enums.Division;
import mg.dgi.fiscaltrack.domain.enums.RoleSecurite;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentFiscal {

    private String idAgent;
    private String nomAgent;
    private String prenomAgent;
    private String emailAgent;
    private String telephoneAgent;
    private String motDePasseHashAgent;
    private Division division;
    private RoleSecurite roleSecurite;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

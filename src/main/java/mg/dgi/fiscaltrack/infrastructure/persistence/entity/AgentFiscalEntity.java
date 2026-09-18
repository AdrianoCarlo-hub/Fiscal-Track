package mg.dgi.fiscaltrack.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "agents_fiscaux")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentFiscalEntity {

    @Id
    @Column(name = "id_agent", length = 6)
    private String idAgent;

    @Column(name = "nom_agent", nullable = false)
    private String nomAgent;

    @Column(name = "prenom_agent", nullable = false)
    private String prenomAgent;

    @Column(name = "email_agent", nullable = false, unique = true)
    private String emailAgent;

    @Column(name = "telephone_agent")
    private String telephoneAgent;

    @Column(name = "mot_de_passe_hash_agent", nullable = false, columnDefinition = "TEXT")
    private String motDePasseHashAgent;

    @Column(name = "division", nullable = false)
    private String division;

    @Column(name = "role_securite", nullable = false)
    private String roleSecurite;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

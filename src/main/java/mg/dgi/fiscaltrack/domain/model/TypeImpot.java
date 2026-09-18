package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.dgi.fiscaltrack.domain.enums.Periodicite;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeImpot {

    private String codeImpot;
    private String nomImpot;
    private Periodicite periodicite;
    private Integer echeanceTheoriqueJour;
    private Integer echeanceTheoriqueMois;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

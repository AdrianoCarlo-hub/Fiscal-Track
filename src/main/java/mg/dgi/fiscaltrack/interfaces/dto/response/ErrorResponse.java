package mg.dgi.fiscaltrack.interfaces.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    /** Erreurs de validation par champ (uniquement pour les 400 de validation). */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> champs;
}

package mg.dgi.fiscaltrack.application.usecase.typeimpot;

import mg.dgi.fiscaltrack.application.port.out.TypeImpotRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.TypeImpot;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsulterTypesImpotsUseCase {

    private final TypeImpotRepositoryPort repositoryPort;

    public ConsulterTypesImpotsUseCase(TypeImpotRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public TypeImpot parCode(String codeImpot) {
        return repositoryPort.findByCodeImpot(codeImpot)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Type d'impot introuvable avec le code : " + codeImpot));
    }

    public List<TypeImpot> tous() {
        return repositoryPort.findAll();
    }
}

package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.TypeImpot;

import java.util.List;
import java.util.Optional;

public interface TypeImpotRepositoryPort {

    TypeImpot save(TypeImpot typeImpot);

    Optional<TypeImpot> findByCodeImpot(String codeImpot);

    List<TypeImpot> findAll();

    void deleteByCodeImpot(String codeImpot);

    boolean existsByCodeImpot(String codeImpot);
}

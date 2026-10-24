package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.AmendeDefautDepot;

import java.util.List;
import java.util.Optional;

public interface AmendeDefautDepotRepositoryPort {

    AmendeDefautDepot save(AmendeDefautDepot amende);

    Optional<AmendeDefautDepot> findByIdObligationFiscale(Long idObligationFiscale);

    boolean existsByIdObligationFiscale(Long idObligationFiscale);

    List<AmendeDefautDepot> findByNif(String nif);
}

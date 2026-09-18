package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ObligationFiscaleRepositoryPort {

    ObligationFiscale save(ObligationFiscale obligationFiscale);

    Optional<ObligationFiscale> findById(Long id);

    List<ObligationFiscale> findAll();

    List<ObligationFiscale> findByNif(String nif);

    List<ObligationFiscale> findByStatut(String statutDeclaration);

    List<ObligationFiscale> findByAgent(String idAgent);

    List<ObligationFiscale> findEnRetard(LocalDate date);

    List<ObligationFiscale> findEcheancesProches(LocalDate date, int nombreJours);

    List<ObligationFiscale> findByNifEtPeriode(String nif, String periodeFiscale);

    void deleteById(Long id);
}

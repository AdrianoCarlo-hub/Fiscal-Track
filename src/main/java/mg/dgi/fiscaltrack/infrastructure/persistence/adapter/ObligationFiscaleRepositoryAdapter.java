package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.ObligationFiscaleMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.ObligationFiscaleJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ObligationFiscaleRepositoryAdapter implements ObligationFiscaleRepositoryPort {

    private final ObligationFiscaleJpaRepository jpaRepository;
    private final ObligationFiscaleMapper mapper;

    public ObligationFiscaleRepositoryAdapter(ObligationFiscaleJpaRepository jpaRepository,
                                               ObligationFiscaleMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ObligationFiscale save(ObligationFiscale obligationFiscale) {
        if (obligationFiscale.getCreatedAt() == null) {
            obligationFiscale.setCreatedAt(OffsetDateTime.now());
        }
        obligationFiscale.setUpdatedAt(OffsetDateTime.now());
        var entity = mapper.toEntity(obligationFiscale);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<ObligationFiscale> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ObligationFiscale> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ObligationFiscale> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public List<ObligationFiscale> findByNif(String nif) {
        return jpaRepository.findByNif(nif).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ObligationFiscale> findByStatut(String statutDeclaration) {
        return jpaRepository.findByStatutDeclaration(statutDeclaration).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ObligationFiscale> findByAgent(String idAgent) {
        return jpaRepository.findByIdAgent(idAgent).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ObligationFiscale> findEnRetard(LocalDate date) {
        return jpaRepository.findEnRetard(date).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ObligationFiscale> findEcheancesProches(LocalDate date, int nombreJours) {
        LocalDate datePlus = date.plusDays(nombreJours);
        return jpaRepository.findEcheancesProches(date, datePlus).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ObligationFiscale> findByNifEtPeriode(String nif, String periodeFiscale) {
        return jpaRepository.findByNifAndPeriodeFiscale(nif, periodeFiscale).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}

package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.PaiementRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.Paiement;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.PaiementMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.PaiementJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PaiementRepositoryAdapter implements PaiementRepositoryPort {

    private final PaiementJpaRepository jpaRepository;
    private final PaiementMapper mapper;

    public PaiementRepositoryAdapter(PaiementJpaRepository jpaRepository,
                                      PaiementMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Paiement save(Paiement paiement) {
        if (paiement.getCreatedAt() == null) {
            paiement.setCreatedAt(OffsetDateTime.now());
        }
        if (paiement.getDatePaiement() == null) {
            paiement.setDatePaiement(OffsetDateTime.now());
        }
        var entity = mapper.toEntity(paiement);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Paiement> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Paiement> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Paiement> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public List<Paiement> findByIdCompte(Long idCompte) {
        return jpaRepository.findByIdCompte(idCompte).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Paiement> findByReferenceTransaction(String referenceTransaction) {
        return jpaRepository.findByReferenceTransaction(referenceTransaction).map(mapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByReferenceTransaction(String referenceTransaction) {
        return jpaRepository.existsByReferenceTransaction(referenceTransaction);
    }
}

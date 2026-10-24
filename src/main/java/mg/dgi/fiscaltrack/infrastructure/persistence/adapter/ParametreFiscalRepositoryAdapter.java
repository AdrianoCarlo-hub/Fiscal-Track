package mg.dgi.fiscaltrack.infrastructure.persistence.adapter;

import mg.dgi.fiscaltrack.application.port.out.ParametreFiscalRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.ParametreFiscal;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.ParametreFiscalEntity;
import mg.dgi.fiscaltrack.infrastructure.persistence.mapper.ParametreFiscalMapper;
import mg.dgi.fiscaltrack.infrastructure.persistence.repository.ParametreFiscalJpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur pour l'acces aux parametres fiscaux.
 * Implemente le port ParametreFiscalRepositoryPort.
 */
@Component
public class ParametreFiscalRepositoryAdapter implements ParametreFiscalRepositoryPort {

    private final ParametreFiscalJpaRepository jpaRepository;
    private final ParametreFiscalMapper mapper;

    public ParametreFiscalRepositoryAdapter(ParametreFiscalJpaRepository jpaRepository,
                                          ParametreFiscalMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ParametreFiscal> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public java.util.List<ParametreFiscal> findAll() {
        return jpaRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal getValeurByCode(String code) {
        return jpaRepository.findByCode(code)
                .map(ParametreFiscalEntity::getValeur)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Parametre fiscal introuvable : " + code));
    }

    @Override
    public List<ParametreFiscal> findApplicablesPourAnnee(int annee) {
        return jpaRepository
                .findApplicablesPourAnnee(Year.of(annee))
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParametreFiscal> findBySecteur(String secteur) {
        return jpaRepository.findBySecteur(secteur)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParametreFiscal> findByBaseLegale(String baseLegale) {
        return jpaRepository.findByBaseLegale(baseLegale)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParametreFiscal> findByCodePrefix(String prefix) {
        return jpaRepository.findByCodeStartingWith(prefix)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public ParametreFiscal save(ParametreFiscal parametre) {
        ParametreFiscalEntity entity = mapper.toEntity(parametre);
        ParametreFiscalEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(ParametreFiscal parametre) {
        jpaRepository.delete(mapper.toEntity(parametre));
    }
}

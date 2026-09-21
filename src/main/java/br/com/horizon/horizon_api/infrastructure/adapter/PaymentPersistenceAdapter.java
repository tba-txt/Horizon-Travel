package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.domain.model.Payment;
import br.com.horizon.horizon_api.domain.port.PaymentPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PaymentEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.PaymentRepository;
import br.com.horizon.horizon_api.application.mapper.PaymentMapper;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements PaymentPersistencePort {

    private final PaymentRepository repository;
    private final PaymentMapper mapper;

    @Override
    public Payment save(Payment domain) {
        PaymentEntity entity = mapper.toEntity(domain);
        PaymentEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}

package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.domain.model.Passenger;
import br.com.horizon.horizon_api.domain.port.PassengerPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PassengerEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.PassengerRepository;
import br.com.horizon.horizon_api.application.mapper.PassengerMapper;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PassengerPersistenceAdapter implements PassengerPersistencePort {

    private final PassengerRepository repository;
    private final PassengerMapper mapper;

    @Override
    public Passenger save(Passenger domain) {
        PassengerEntity entity = mapper.toEntity(domain);
        PassengerEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Passenger> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Passenger> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}

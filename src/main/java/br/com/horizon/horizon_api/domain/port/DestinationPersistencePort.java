package br.com.horizon.horizon_api.domain.port;

import br.com.horizon.horizon_api.domain.model.Destination;

import java.util.List;
import java.util.Optional;

public interface DestinationPersistencePort {
    List<Destination> findActive(Integer page, Integer size);
    Optional<Destination> findById(Long id);
}

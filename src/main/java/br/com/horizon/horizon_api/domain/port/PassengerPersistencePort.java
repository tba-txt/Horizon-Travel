package br.com.horizon.horizon_api.domain.port;

import br.com.horizon.horizon_api.domain.model.Passenger;
import java.util.List;
import java.util.Optional;

public interface PassengerPersistencePort {
    Passenger save(Passenger passenger);
    Optional<Passenger> findById(Long id);
    List<Passenger> findAll();
    void delete(Long id);
}

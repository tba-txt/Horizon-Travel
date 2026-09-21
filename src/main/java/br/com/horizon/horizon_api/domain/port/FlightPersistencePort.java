package br.com.horizon.horizon_api.domain.port;

import br.com.horizon.horizon_api.domain.model.Flight;
import br.com.horizon.horizon_api.domain.model.FlightAvailability;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FlightPersistencePort {
    Optional<Flight> getFlightById(Long id);
    List<Flight> getAvailableFlights(Long destinationId, LocalDate start, LocalDate end);
    Optional<FlightAvailability> getAvailabilityByFlightId(Long flightId);
    boolean reserveSeats(Long flightId, int seats);
    boolean releaseSeats(Long flightId, int seats);
}

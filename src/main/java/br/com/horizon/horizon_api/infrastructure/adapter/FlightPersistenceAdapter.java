package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.domain.model.Flight;
import br.com.horizon.horizon_api.domain.model.FlightAvailability;
import br.com.horizon.horizon_api.domain.port.FlightPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.FlightEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.FlightAvailabilityEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.FlightRepository;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.FlightAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FlightPersistenceAdapter implements FlightPersistencePort {

    private final FlightRepository flightRepository;
    private final FlightAvailabilityRepository flightAvailabilityRepository;

    @Override
    public Optional<Flight> getFlightById(Long id) {
        return flightRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Flight> getAvailableFlights(Long destinationId, LocalDate start, LocalDate end) {
        return flightRepository.findAvailableFlights(destinationId, start, end)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<FlightAvailability> getAvailabilityByFlightId(Long flightId) {
        return flightAvailabilityRepository.findByFlightId(flightId).map(this::toAvailabilityDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean reserveSeats(Long flightId, int seats) {
        int updated = flightAvailabilityRepository.reserveSeats(flightId, seats);
        return updated > 0;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean releaseSeats(Long flightId, int seats) {
        int updated = flightAvailabilityRepository.releaseSeats(flightId, seats);
        return updated > 0;
    }

    private Flight toDomain(FlightEntity entity) {
        Flight f = new Flight();
        f.setId(entity.getId());
        f.setDestinationId(entity.getDestination().getId());
        f.setOriginAirportCode(entity.getOriginAirportCode());
        f.setDestinationAirportCode(entity.getDestinationAirportCode());
        f.setFlightDate(entity.getFlightDate());
        f.setDepartureTime(entity.getDepartureTime());
        f.setArrivalTime(entity.getArrivalTime());
        f.setFlightNumber(entity.getFlightNumber());
        f.setPricePerPerson(entity.getPricePerPerson());
        f.setPriceExecutive(entity.getPriceExecutive());
        f.setPricePremium(entity.getPricePremium());
        f.setActive(entity.getActive());
        return f;
    }

    private FlightAvailability toAvailabilityDomain(FlightAvailabilityEntity entity) {
        FlightAvailability fa = new FlightAvailability();
        fa.setId(entity.getId());
        fa.setFlightId(entity.getFlight().getId());
        fa.setTotalSeats(entity.getTotalSeats());
        fa.setAvailableSeats(entity.getAvailableSeats());
        fa.setUpdatedAt(entity.getUpdatedAt());
        return fa;
    }
}

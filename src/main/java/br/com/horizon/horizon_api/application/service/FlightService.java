package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.dto.response.FlightResponse;
import br.com.horizon.horizon_api.application.usecase.SearchFlightsUseCase;
import br.com.horizon.horizon_api.domain.model.Flight;
import br.com.horizon.horizon_api.domain.model.FlightAvailability;
import br.com.horizon.horizon_api.domain.port.FlightPersistencePort;
import br.com.horizon.horizon_api.domain.port.DestinationPersistencePort;
import br.com.horizon.horizon_api.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FlightService implements SearchFlightsUseCase {
    
    private final FlightPersistencePort flightPort;
    private final DestinationPersistencePort destinationPort;

    @Override
    public List<FlightResponse> execute(Long destinationId, LocalDate start, LocalDate end) {
        if (destinationPort.findById(destinationId).isEmpty()) {
            throw new ResourceNotFoundException("Destino não encontrado.");
        }

        if (start == null || start.isBefore(LocalDate.now().plusDays(3))) {
            start = LocalDate.now().plusDays(3);
        }
        if (end == null) {
            end = start.plusMonths(24);
        }
        
        List<Flight> flights = flightPort.getAvailableFlights(destinationId, start, end);
        log.info("Service: searchFlights destinationId={}, start={}, end={} - Encontrados {} voos", destinationId, start, end, flights.size());
        
        return flights.stream().map(f -> {
            FlightResponse r = new FlightResponse();
            r.setId(f.getId());
            r.setDestinationId(f.getDestinationId());
            r.setOriginAirportCode(f.getOriginAirportCode());
            r.setDestinationAirportCode(f.getDestinationAirportCode());
            r.setFlightDate(f.getFlightDate());
            r.setDepartureTime(f.getDepartureTime());
            r.setArrivalTime(f.getArrivalTime());
            r.setFlightNumber(f.getFlightNumber());
            r.setPricePerPerson(f.getPricePerPerson());
            r.setPriceExecutive(f.getPriceExecutive());
            r.setPricePremium(f.getPricePremium());
            
            // To be totally precise, since we need available_seats, we should fetch it.
            // Even though this is N+1, we can optimize later or since the list is short, it's ok for now.
            // Wait, I will just call getAvailabilityByFlightId. It's fully cached in Hibernate session usually if we joined, but we didn't FETCH join.
            flightPort.getAvailabilityByFlightId(f.getId()).ifPresent(av -> {
                r.setTotalSeats(av.getTotalSeats());
                r.setAvailableSeats(av.getAvailableSeats());
            });
            
            return r;
        }).collect(Collectors.toList());
    }
}

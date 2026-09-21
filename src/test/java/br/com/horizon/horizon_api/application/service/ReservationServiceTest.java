package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.dto.request.PassengerRequest;
import br.com.horizon.horizon_api.application.dto.request.ReservationRequest;
import br.com.horizon.horizon_api.application.dto.response.ReservationResponse;
import br.com.horizon.horizon_api.domain.model.Flight;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.port.FlightPersistencePort;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationPersistencePort reservationPort;

    @Mock
    private FlightPersistencePort flightPort;

    @Mock
    private br.com.horizon.horizon_api.domain.port.DestinationPersistencePort destinationPort;

    @Mock
    private br.com.horizon.horizon_api.domain.port.TicketPersistencePort ticketPort;

    @Mock
    private br.com.horizon.horizon_api.application.mapper.TicketMapper ticketMapper;

    @InjectMocks
    private ReservationService service;

    private ReservationRequest req;
    private PassengerRequest p1;
    private Flight outFlight;
    private Flight retFlight;

    @BeforeEach
    void setUp() {
        req = new ReservationRequest();
        req.setDestinationId(1L);
        req.setTripType(TripType.ONE_WAY);
        req.setOutboundFlightId(10L);

        p1 = new PassengerRequest();
        p1.setName("John");
        p1.setAge(30);
        req.setPassengers(List.of(p1));

        outFlight = new Flight();
        outFlight.setId(10L);
        outFlight.setDestinationId(1L);
        outFlight.setFlightDate(LocalDate.now().plusDays(10));
        outFlight.setPricePerPerson(new BigDecimal("500.00"));

        retFlight = new Flight();
        retFlight.setId(20L);
        retFlight.setDestinationId(1L);
        retFlight.setFlightDate(LocalDate.now().plusDays(15));
        retFlight.setPricePerPerson(new BigDecimal("400.00"));
    }

    @Test
    void testOneWayCalculatesCorrectlyAndPendingStatus() {
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));
        when(flightPort.reserveSeats(10L, 1)).thenReturn(true);
        when(reservationPort.save(any())).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        ReservationResponse res = service.execute(1L, req);
        
        assertNotNull(res);
        assertEquals(new BigDecimal("500.00"), res.getTotalAmount());
        assertEquals(ReservationStatus.PENDENTE, res.getStatus());
        verify(flightPort).reserveSeats(10L, 1);
        verify(reservationPort).save(any());
    }

    @Test
    void testRoundTripCalculatesCorrectly() {
        req.setTripType(TripType.ROUND_TRIP);
        req.setReturnFlightId(20L);

        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));
        when(flightPort.getFlightById(20L)).thenReturn(Optional.of(retFlight));
        when(flightPort.reserveSeats(10L, 1)).thenReturn(true);
        when(flightPort.reserveSeats(20L, 1)).thenReturn(true);
        when(reservationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.execute(1L, req);
        
        assertEquals(new BigDecimal("900.00"), res.getTotalAmount());
        verify(flightPort).reserveSeats(10L, 1);
        verify(flightPort).reserveSeats(20L, 1);
    }

    @Test
    void testRejectsLessThan3Days() {
        outFlight.setFlightDate(LocalDate.now().plusDays(2));
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));

        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("at least 3 days"));
    }

    @Test
    void testRejectsMoreThan24Months() {
        outFlight.setFlightDate(LocalDate.now().plusMonths(25));
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));

        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("exceed 24 months"));
    }

    @Test
    void testRejectsMoreThan5Passengers() {
        req.setPassengers(List.of(p1, p1, p1, p1, p1, p1)); // 6 passengers
        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("between 1 and 5"));
    }

    @Test
    void testRejectsAgeOutside1To99() {
        p1.setAge(100);
        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("between 1 and 99"));

        p1.setAge(0);
        Exception e2 = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e2.getMessage().contains("between 1 and 99"));
    }

    @Test
    void testRejectsFlightWithoutAvailability() {
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));
        when(flightPort.reserveSeats(10L, 1)).thenReturn(false);

        Exception e = assertThrows(IllegalStateException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("Not enough seats available"));
    }
}

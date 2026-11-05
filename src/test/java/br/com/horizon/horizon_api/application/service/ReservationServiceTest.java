package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.dto.request.PassengerRequest;
import br.com.horizon.horizon_api.application.dto.request.ReservationRequest;
import br.com.horizon.horizon_api.application.dto.response.ReservationResponse;
import br.com.horizon.horizon_api.domain.model.Destination;
import br.com.horizon.horizon_api.domain.model.Flight;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.model.User;
import br.com.horizon.horizon_api.domain.port.FlightPersistencePort;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
    private br.com.horizon.horizon_api.domain.port.UserPersistencePort userPort;

    @Mock
    private br.com.horizon.horizon_api.application.mapper.TicketMapper ticketMapper;

    @InjectMocks
    private ReservationService service;

    private ReservationRequest req;
    private PassengerRequest p1;
    private Flight outFlight;
    private Flight retFlight;
    private User adultBuyer;

    @BeforeEach
    void setUp() {
        adultBuyer = new User();
        adultBuyer.setId(1L);
        adultBuyer.setBirthDate(LocalDate.now().minusYears(25));
        lenient().when(userPort.findById(1L)).thenReturn(Optional.of(adultBuyer));

        req = new ReservationRequest();
        req.setDestinationId(1L);
        req.setTripType(TripType.ONE_WAY);
        req.setOutboundFlightId(10L);

        p1 = new PassengerRequest();
        p1.setName("John Doe");
        p1.setAge(30);
        req.setPassengers(new ArrayList<>(List.of(p1)));

        outFlight = new Flight();
        outFlight.setId(10L);
        outFlight.setDestinationId(1L);
        outFlight.setActive(true);
        outFlight.setFlightDate(LocalDate.now().plusDays(10));
        outFlight.setPricePerPerson(new BigDecimal("500.00"));

        retFlight = new Flight();
        retFlight.setId(20L);
        retFlight.setDestinationId(1L);
        retFlight.setActive(true);
        retFlight.setFlightDate(LocalDate.now().plusDays(15));
        retFlight.setPricePerPerson(new BigDecimal("400.00"));

        Destination defaultDest = new Destination();
        defaultDest.setId(1L);
        defaultDest.setName("Rio de Janeiro");
        defaultDest.setCountry("Brasil");
        lenient().when(destinationPort.findById(1L)).thenReturn(Optional.of(defaultDest));
    }

    @Test
    @DisplayName("Deve calcular corretamente viagem só de ida e iniciar em status PENDENTE com assentos reservados")
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
    @DisplayName("Deve calcular corretamente viagem de ida e volta")
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
    @DisplayName("Deve rejeitar comprador menor de 18 anos")
    void testRejectsBuyerUnder18() {
        User minorBuyer = new User();
        minorBuyer.setId(2L);
        minorBuyer.setBirthDate(LocalDate.now().minusYears(17).minusDays(364));
        when(userPort.findById(2L)).thenReturn(Optional.of(minorBuyer));

        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(2L, req));
        assertTrue(e.getMessage().contains("18 anos"), "Deveria exigir maioridade do comprador");
    }

    @Test
    @DisplayName("Deve aceitar comprador com exatamente 18 anos")
    void testAcceptsBuyerExactly18() {
        User exact18Buyer = new User();
        exact18Buyer.setId(3L);
        exact18Buyer.setBirthDate(LocalDate.now().minusYears(18));
        when(userPort.findById(3L)).thenReturn(Optional.of(exact18Buyer));
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));
        when(flightPort.reserveSeats(10L, 1)).thenReturn(true);
        when(reservationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.execute(3L, req);
        assertNotNull(res);
    }

    @Test
    @DisplayName("Deve aceitar passageiro com idade mínima de 1 ano")
    void testAcceptsPassengerAge1() {
        p1.setAge(1);
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));
        when(flightPort.reserveSeats(10L, 1)).thenReturn(true);
        when(reservationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.execute(1L, req);
        assertNotNull(res);
    }

    @Test
    @DisplayName("Deve aceitar passageiro com idade máxima permitida de 120 anos")
    void testAcceptsPassengerAge120() {
        p1.setAge(120);
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));
        when(flightPort.reserveSeats(10L, 1)).thenReturn(true);
        when(reservationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.execute(1L, req);
        assertNotNull(res);
    }

    @Test
    @DisplayName("Deve rejeitar passageiro com idade acima de 120 anos")
    void testRejectsPassengerAgeAbove120() {
        p1.setAge(121);
        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("1 e 120 anos"), "Mensagem deve indicar limite de 1 a 120 anos");
    }

    @Test
    @DisplayName("Deve rejeitar passageiro com idade zero ou negativa")
    void testRejectsPassengerAgeZeroOrNegative() {
        p1.setAge(0);
        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("1 e 120 anos"));
    }

    @Test
    @DisplayName("Deve aceitar quantidade mínima de 1 passageiro")
    void testAcceptsMinimum1Passenger() {
        assertEquals(1, req.getPassengers().size());
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));
        when(flightPort.reserveSeats(10L, 1)).thenReturn(true);
        when(reservationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.execute(1L, req);
        assertEquals(1, res.getPassengers().size());
    }

    @Test
    @DisplayName("Deve aceitar quantidade máxima de 5 passageiros e multiplicar o total")
    void testAcceptsMaximum5Passengers() {
        List<PassengerRequest> passengers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            PassengerRequest p = new PassengerRequest();
            p.setName("Passageiro " + i);
            p.setAge(20 + i);
            passengers.add(p);
        }
        req.setPassengers(passengers);

        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));
        when(flightPort.reserveSeats(10L, 5)).thenReturn(true);
        when(reservationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.execute(1L, req);
        assertEquals(5, res.getPassengers().size());
        assertEquals(new BigDecimal("2500.00"), res.getTotalAmount()); // 500 * 5
    }

    @Test
    @DisplayName("Deve rejeitar reserva com zero passageiros")
    void testRejectsZeroPassengers() {
        req.setPassengers(Collections.emptyList());
        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("1 e 5 passageiros"));
    }

    @Test
    @DisplayName("Deve rejeitar reserva com mais de 5 passageiros")
    void testRejectsMoreThan5Passengers() {
        List<PassengerRequest> sixPassengers = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            PassengerRequest p = new PassengerRequest();
            p.setName("Passageiro " + i);
            p.setAge(25);
            sixPassengers.add(p);
        }
        req.setPassengers(sixPassengers);

        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("1 e 5"));
    }

    @Test
    @DisplayName("Deve rejeitar data de partida com menos de 3 dias de antecedência")
    void testRejectsLessThan3Days() {
        outFlight.setFlightDate(LocalDate.now().plusDays(2));
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));

        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("3 dias"));
    }

    @Test
    @DisplayName("Deve aceitar data de partida exatamente no limite de 3 dias de antecedência")
    void testAcceptsExactly3Days() {
        outFlight.setFlightDate(LocalDate.now().plusDays(3));
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));
        when(flightPort.reserveSeats(10L, 1)).thenReturn(true);
        when(reservationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.execute(1L, req);
        assertNotNull(res);
        assertEquals(LocalDate.now().plusDays(3), res.getDepartureDate());
    }

    @Test
    @DisplayName("Deve rejeitar data de partida com mais de 24 meses")
    void testRejectsMoreThan24Months() {
        outFlight.setFlightDate(LocalDate.now().plusMonths(25));
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));

        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("24 meses"));
    }

    @Test
    @DisplayName("Deve rejeitar ida e volta quando a data de volta for anterior à data de ida")
    void testRejectsReturnDateBeforeDeparture() {
        req.setTripType(TripType.ROUND_TRIP);
        req.setReturnFlightId(20L);
        outFlight.setFlightDate(LocalDate.now().plusDays(10));
        retFlight.setFlightDate(LocalDate.now().plusDays(8)); // anterior!

        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));
        when(flightPort.getFlightById(20L)).thenReturn(Optional.of(retFlight));

        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("não pode ser anterior"));
    }

    @Test
    @DisplayName("América do Sul: aceita retorno com pelo menos 1 dia de estadia e rejeita no mesmo dia")
    void testSouthAmericaStayDurationRules() {
        req.setTripType(TripType.ROUND_TRIP);
        req.setReturnFlightId(20L);

        Destination saDest = new Destination();
        saDest.setId(1L);
        saDest.setCountry("Argentina");
        when(destinationPort.findById(1L)).thenReturn(Optional.of(saDest));

        // Caso 1: Mesmo dia (diferença 0 dias) -> rejeita
        outFlight.setFlightDate(LocalDate.now().plusDays(5));
        retFlight.setFlightDate(LocalDate.now().plusDays(5));
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));
        when(flightPort.getFlightById(20L)).thenReturn(Optional.of(retFlight));

        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("América do Sul") && e.getMessage().contains("no mínimo 1 dia"));

        // Caso 2: 1 dia de estadia -> aceita
        retFlight.setFlightDate(LocalDate.now().plusDays(6));
        when(flightPort.reserveSeats(10L, 1)).thenReturn(true);
        when(flightPort.reserveSeats(20L, 1)).thenReturn(true);
        when(reservationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.execute(1L, req);
        assertNotNull(res);
    }

    @Test
    @DisplayName("Fora da América do Sul: exige retorno com pelo menos 3 dias de estadia devido ao tempo de voo")
    void testOutsideSouthAmericaStayDurationRules() {
        req.setTripType(TripType.ROUND_TRIP);
        req.setReturnFlightId(20L);

        Destination europeDest = new Destination();
        europeDest.setId(1L);
        europeDest.setCountry("França");
        when(destinationPort.findById(1L)).thenReturn(Optional.of(europeDest));

        // Caso 1: 2 dias de intervalo -> rejeita
        outFlight.setFlightDate(LocalDate.now().plusDays(10));
        retFlight.setFlightDate(LocalDate.now().plusDays(12));
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));
        when(flightPort.getFlightById(20L)).thenReturn(Optional.of(retFlight));

        Exception e = assertThrows(IllegalArgumentException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("internacionais fora da América do Sul") && e.getMessage().contains("no mínimo 3 dias"));

        // Caso 2: 3 dias de intervalo -> aceita
        retFlight.setFlightDate(LocalDate.now().plusDays(13));
        when(flightPort.reserveSeats(10L, 1)).thenReturn(true);
        when(flightPort.reserveSeats(20L, 1)).thenReturn(true);
        when(reservationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.execute(1L, req);
        assertNotNull(res);
    }

    @Test
    @DisplayName("Deve rejeitar quando não houver assentos disponíveis na reserva atômica")
    void testRejectsFlightWithoutAvailability() {
        when(flightPort.getFlightById(10L)).thenReturn(Optional.of(outFlight));
        when(flightPort.reserveSeats(10L, 1)).thenReturn(false);

        Exception e = assertThrows(IllegalStateException.class, () -> service.execute(1L, req));
        assertTrue(e.getMessage().contains("assentos suficientes"));
    }
}

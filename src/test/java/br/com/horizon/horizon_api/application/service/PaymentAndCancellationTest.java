package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.usecase.GenerateTicketsUseCase;
import br.com.horizon.horizon_api.domain.model.Destination;
import br.com.horizon.horizon_api.domain.model.Payment;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.model.User;
import br.com.horizon.horizon_api.domain.port.DestinationPersistencePort;
import br.com.horizon.horizon_api.domain.port.EmailPort;
import br.com.horizon.horizon_api.domain.port.FlightPersistencePort;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.domain.port.UserPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentMethod;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentStatus;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentAndCancellationTest {

    @Mock
    private ReservationPersistencePort reservationPort;

    @Mock
    private FlightPersistencePort flightPort;

    @Mock
    private UserPersistencePort userPort;

    @Mock
    private DestinationPersistencePort destinationPort;

    @Mock
    private EmailPort emailPort;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GenerateTicketsUseCase generateTicketsUseCase;

    @InjectMocks
    private PaymentService paymentService;

    @InjectMocks
    private CancellationService cancellationService;

    private Reservation res;
    private Payment pay;
    private UserEntity userEntity;
    private User userDomain;
    private Destination destination;

    @BeforeEach
    void setUp() {
        res = new Reservation();
        res.setId(100L);
        res.setUserId(1L);
        res.setDestinationId(10L);
        res.setOutboundFlightId(10L);
        res.setReturnFlightId(20L);
        res.setNumberOfPassengers(2);
        res.setStatus(ReservationStatus.PENDENTE);
        res.setPaymentDeadline(OffsetDateTime.now().plusHours(24));

        pay = new Payment();
        pay.setId(200L);
        pay.setStatus(PaymentStatus.PENDENTE);
        res.setPayment(pay);

        userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setName("Carlos Viajante");
        userEntity.setEmail("carlos@example.com");

        userDomain = new User();
        userDomain.setId(1L);
        userDomain.setName("Carlos Viajante");
        userDomain.setEmail("carlos@example.com");

        destination = new Destination();
        destination.setId(10L);
        destination.setName("Fernando de Noronha");

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        lenient().when(userPort.findById(1L)).thenReturn(Optional.of(userDomain));
        lenient().when(destinationPort.findById(10L)).thenReturn(Optional.of(destination));
    }

    @Test
    @DisplayName("Pagamento confirmado: transita PENDENTE -> CONFIRMADA, status PAGO, emite bilhetes e envia e-mail")
    void testPendingToPaidEmitsTicketsAndSendsEmail() {
        when(reservationPort.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(res));
        when(reservationPort.save(any())).thenReturn(res);

        paymentService.execute(100L, 1L, PaymentMethod.PIX);

        assertEquals(ReservationStatus.CONFIRMADA, res.getStatus());
        assertEquals(PaymentStatus.PAGO, pay.getStatus());
        assertEquals(PaymentMethod.PIX, pay.getPaymentMethod());
        assertNotNull(pay.getPaidAt());

        verify(reservationPort).save(res);
        verify(generateTicketsUseCase, times(1)).execute(res);
        verify(emailPort, times(1)).sendEmail(eq("carlos@example.com"), contains("Pagamento Confirmado"), anyString());
    }

    @Test
    @DisplayName("Pagamento rejeitado quando prazo limite de 24 horas tiver expirado")
    void testExpiredPaymentRejected() {
        res.setPaymentDeadline(OffsetDateTime.now().minusHours(1));
        when(reservationPort.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(res));

        Exception e = assertThrows(IllegalStateException.class, () -> paymentService.execute(100L, 1L, PaymentMethod.PIX));
        assertTrue(e.getMessage().contains("Payment deadline has expired"));
        verify(generateTicketsUseCase, never()).execute(any());
        verify(emailPort, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Pagamento duplicado rejeitado quando a reserva já estiver CONFIRMADA")
    void testDuplicatePaymentRejected() {
        res.setStatus(ReservationStatus.CONFIRMADA);
        pay.setStatus(PaymentStatus.PAGO);
        when(reservationPort.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(res));

        Exception e = assertThrows(IllegalStateException.class, () -> paymentService.execute(100L, 1L, PaymentMethod.PIX));
        assertTrue(e.getMessage().contains("Reservation is not in PENDENTE status"));
    }

    @Test
    @DisplayName("Cancelamento de reserva PENDENTE: status CANCELADA, libera assentos de ida e volta e cancela pagamento pendente")
    void testPendingToCancelledReleasesOutboundAndReturnSeats() {
        when(reservationPort.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(res));

        cancellationService.execute(100L, 1L);

        assertEquals(ReservationStatus.CANCELADA, res.getStatus());
        assertEquals(PaymentStatus.CANCELADO, pay.getStatus());
        assertNotNull(res.getCancelledAt());

        // Libera assentos de ida (10L) e volta (20L) para os 2 passageiros
        verify(flightPort).releaseSeats(10L, 2);
        verify(flightPort).releaseSeats(20L, 2);
        verify(reservationPort).save(res);
        verify(emailPort, times(1)).sendEmail(eq("carlos@example.com"), contains("Cancelamento"), anyString());
    }

    @Test
    @DisplayName("Cancelamento repetido rejeitado: não permite cancelar reserva já CANCELADA nem libera assentos duas vezes")
    void testRepeatedCancellationRejected() {
        res.setStatus(ReservationStatus.CANCELADA);
        when(reservationPort.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(res));

        Exception e = assertThrows(IllegalStateException.class, () -> cancellationService.execute(100L, 1L));
        assertTrue(e.getMessage().contains("cannot be cancelled"));

        // Garante que assentos não são liberados novamente
        verify(flightPort, never()).releaseSeats(any(), anyInt());
    }

    @Test
    @DisplayName("Cancelamento disparado pelo Batch (userId nulo): cancela reserva e libera assentos automaticamente")
    void testBatchCancellationLogic() {
        when(reservationPort.findById(100L)).thenReturn(Optional.of(res));

        cancellationService.execute(100L, null); // Batch usa userId nulo

        assertEquals(ReservationStatus.CANCELADA, res.getStatus());
        verify(flightPort).releaseSeats(10L, 2);
        verify(flightPort).releaseSeats(20L, 2);
    }

    @Test
    @DisplayName("Batch não deve cancelar reservas já CONFIRMADAS com pagamento efetuado")
    void testBatchDoesNotProcessConfirmed() {
        res.setStatus(ReservationStatus.CONFIRMADA);
        when(reservationPort.findById(100L)).thenReturn(Optional.of(res));

        Exception e = assertThrows(IllegalStateException.class, () -> cancellationService.execute(100L, null));
        assertTrue(e.getMessage().contains("cannot be cancelled"));
        verify(flightPort, never()).releaseSeats(any(), anyInt());
    }
}

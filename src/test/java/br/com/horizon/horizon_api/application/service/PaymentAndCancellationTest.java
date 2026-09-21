package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.domain.model.Payment;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.port.FlightPersistencePort;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentMethod;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentStatus;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import br.com.horizon.horizon_api.application.usecase.GenerateTicketsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentAndCancellationTest {

    @Mock
    private ReservationPersistencePort reservationPort;

    @Mock
    private FlightPersistencePort flightPort;

    @Mock
    private GenerateTicketsUseCase generateTicketsUseCase;

    @InjectMocks
    private PaymentService paymentService;

    @InjectMocks
    private CancellationService cancellationService;

    private Reservation res;
    private Payment pay;

    @BeforeEach
    void setUp() {
        res = new Reservation();
        res.setId(100L);
        res.setUserId(1L);
        res.setOutboundFlightId(10L);
        res.setNumberOfPassengers(2);
        res.setStatus(ReservationStatus.PENDENTE);
        res.setPaymentDeadline(OffsetDateTime.now().plusHours(24));

        pay = new Payment();
        pay.setId(200L);
        pay.setStatus(PaymentStatus.PENDENTE);
        res.setPayment(pay);
    }

    @Test
    void testPendingToPaid() {
        when(reservationPort.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(res));
        
        paymentService.execute(100L, 1L, PaymentMethod.PIX);
        
        assertEquals(ReservationStatus.CONFIRMADA, res.getStatus());
        assertEquals(PaymentStatus.PAGO, pay.getStatus());
        assertNotNull(pay.getPaidAt());
        verify(reservationPort).save(res);
    }

    @Test
    void testExpiredPaymentRejected() {
        res.setPaymentDeadline(OffsetDateTime.now().minusHours(1));
        when(reservationPort.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(res));

        Exception e = assertThrows(IllegalStateException.class, () -> paymentService.execute(100L, 1L, PaymentMethod.PIX));
        assertTrue(e.getMessage().contains("Payment deadline has expired"));
    }

    @Test
    void testDuplicatePaymentRejected() {
        res.setStatus(ReservationStatus.CONFIRMADA);
        pay.setStatus(PaymentStatus.PAGO);
        when(reservationPort.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(res));

        Exception e = assertThrows(IllegalStateException.class, () -> paymentService.execute(100L, 1L, PaymentMethod.PIX));
        assertTrue(e.getMessage().contains("Reservation is not in PENDENTE status"));
    }

    @Test
    void testPendingToCancelledReleasesSeats() {
        when(reservationPort.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(res));
        
        cancellationService.execute(100L, 1L);
        
        assertEquals(ReservationStatus.CANCELADA, res.getStatus());
        assertEquals(PaymentStatus.CANCELADO, pay.getStatus());
        assertNotNull(res.getCancelledAt());
        
        verify(flightPort).releaseSeats(10L, 2);
        verify(reservationPort).save(res);
    }

    @Test
    void testRepeatedCancellationRejected() {
        res.setStatus(ReservationStatus.CANCELADA);
        when(reservationPort.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(res));

        Exception e = assertThrows(IllegalStateException.class, () -> cancellationService.execute(100L, 1L));
        assertTrue(e.getMessage().contains("cannot be cancelled"));
        
        // Ensure seats are not released twice
        verify(flightPort, never()).releaseSeats(any(), anyInt());
    }

    @Test
    void testBatchCancellationLogic() {
        when(reservationPort.findById(100L)).thenReturn(Optional.of(res));
        
        cancellationService.execute(100L, null); // Batch uses null userId
        
        assertEquals(ReservationStatus.CANCELADA, res.getStatus());
        verify(flightPort).releaseSeats(10L, 2);
    }

    @Test
    void testBatchDoesNotProcessConfirmed() {
        res.setStatus(ReservationStatus.CONFIRMADA);
        when(reservationPort.findById(100L)).thenReturn(Optional.of(res));

        Exception e = assertThrows(IllegalStateException.class, () -> cancellationService.execute(100L, null));
        assertTrue(e.getMessage().contains("cannot be cancelled"));
        verify(flightPort, never()).releaseSeats(any(), anyInt());
    }
}

package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.usecase.CancelReservationUseCase;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.port.FlightPersistencePort;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentStatus;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CancellationService implements CancelReservationUseCase {

    private final ReservationPersistencePort reservationPort;
    private final FlightPersistencePort flightPort;

    @Override
    @Transactional
    public void execute(Long id, Long userId) {
        Reservation reservation;
        if (userId != null) {
            reservation = reservationPort.findByIdAndUserId(id, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));
        } else {
            // Used by batch
            reservation = reservationPort.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));
        }

        if (reservation.getStatus() == ReservationStatus.CANCELADA || 
            reservation.getStatus() == ReservationStatus.CONCLUIDA) {
            throw new IllegalStateException("Reservation cannot be cancelled in its current state.");
        }

        boolean wasPendente = reservation.getStatus() == ReservationStatus.PENDENTE;

        // Change statuses
        reservation.setStatus(ReservationStatus.CANCELADA);
        reservation.setCancelledAt(OffsetDateTime.now());

        if (wasPendente && reservation.getPayment() != null && 
            reservation.getPayment().getStatus() == PaymentStatus.PENDENTE) {
            reservation.getPayment().setStatus(PaymentStatus.CANCELADO);
        }

        // Release seats
        int seats = reservation.getNumberOfPassengers();
        
        flightPort.releaseSeats(reservation.getOutboundFlightId(), seats);
        
        if (reservation.getReturnFlightId() != null) {
            flightPort.releaseSeats(reservation.getReturnFlightId(), seats);
        }

        reservationPort.save(reservation);
    }
}

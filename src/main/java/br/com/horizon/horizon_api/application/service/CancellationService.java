package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.usecase.CancelReservationUseCase;
import br.com.horizon.horizon_api.domain.model.Destination;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.port.DestinationPersistencePort;
import br.com.horizon.horizon_api.domain.port.EmailPort;
import br.com.horizon.horizon_api.domain.port.FlightPersistencePort;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.domain.port.UserPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentStatus;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CancellationService implements CancelReservationUseCase {

    private final ReservationPersistencePort reservationPort;
    private final FlightPersistencePort flightPort;
    private final UserPersistencePort userPort;
    private final DestinationPersistencePort destinationPort;
    private final EmailPort emailPort;

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
            reservation.getStatus() == ReservationStatus.CONCLUIDA ||
            (userId == null && reservation.getStatus() != ReservationStatus.PENDENTE)) {
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
        log.info("Cancelled reservation ID: {} successfully", reservation.getId());

        // Send cancellation email to user
        try {
            if (reservation.getUserId() != null) {
                userPort.findById(reservation.getUserId()).ifPresent(user -> {
                    String destName = "seu destino";
                    if (reservation.getDestinationId() != null && destinationPort != null) {
                        destName = destinationPort.findById(reservation.getDestinationId())
                                .map(Destination::getName)
                                .orElse("seu destino");
                    }
                    String subject = "Horizon - Cancelamento da Reserva #" + reservation.getId();
                    String motivo = (wasPendente && userId == null)
                            ? "O prazo de 24 horas para o pagamento da sua reserva expirou e ela foi cancelada automaticamente."
                            : "Sua reserva para " + destName + " foi cancelada com sucesso.";

                    String body = "<div style=\"font-family: Arial, sans-serif; color: #333; line-height: 1.6;\">"
                            + "<h2 style=\"color: #4895CC;\">Horizon Travel</h2>"
                            + "<p>Olá, <strong>" + user.getName() + "</strong>!</p>"
                            + "<p>Informamos que a sua reserva <strong>#" + reservation.getId() + "</strong> para <strong>" + destName + "</strong> foi cancelada.</p>"
                            + "<p>" + motivo + "</p>"
                            + "<p>Se você deseja realizar uma nova reserva ou tiver dúvidas, acesse nossa plataforma.</p>"
                            + "<br/>"
                            + "<p>Atenciosamente,<br/><strong>Equipe Horizon</strong></p>"
                            + "</div>";

                    emailPort.sendEmail(user.getEmail(), subject, body);
                    log.info("Cancellation email sent successfully to {} for Reservation ID: {}", user.getEmail(), reservation.getId());
                });
            }
        } catch (Exception e) {
            log.error("Failed to send cancellation email for Reservation ID: {}", reservation.getId(), e);
        }
    }
}

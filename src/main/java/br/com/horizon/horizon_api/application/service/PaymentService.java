package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.usecase.ConfirmPaymentUseCase;
import br.com.horizon.horizon_api.application.usecase.GenerateTicketsUseCase;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentMethod;
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
public class PaymentService implements ConfirmPaymentUseCase {

    private final ReservationPersistencePort reservationPort;
    private final GenerateTicketsUseCase generateTicketsUseCase;

    @Override
    @Transactional
    public void execute(Long reservationId, Long userId, PaymentMethod method) {
        Reservation reservation = reservationPort.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));

        // Idempotency: if already confirmed and paid, ensure tickets exist and return safely
        if (reservation.getStatus() == ReservationStatus.CONFIRMADA &&
            reservation.getPayment() != null && reservation.getPayment().getStatus() == PaymentStatus.PAGO) {
            log.info("Reservation {} already CONFIRMADA and PAGO. Ensuring tickets exist...", reservationId);
            generateTicketsUseCase.execute(reservation);
            return;
        }

        if (reservation.getStatus() != ReservationStatus.PENDENTE) {
            throw new IllegalStateException("Reservation is not in PENDENTE status.");
        }

        if (reservation.getPaymentDeadline() != null && reservation.getPaymentDeadline().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Payment deadline has expired.");
        }

        if (reservation.getPayment() == null || reservation.getPayment().getStatus() != PaymentStatus.PENDENTE) {
            throw new IllegalStateException("Payment is not in PENDENTE status.");
        }

        reservation.setStatus(ReservationStatus.CONFIRMADA);
        reservation.getPayment().setStatus(PaymentStatus.PAGO);
        reservation.getPayment().setPaymentMethod(method);
        reservation.getPayment().setPaidAt(OffsetDateTime.now());

        Reservation saved = reservationPort.save(reservation);

        // Generate tickets for all passengers upon confirmation
        generateTicketsUseCase.execute(saved);
        log.info("Payment confirmed and tickets generated for Reservation ID: {}", saved.getId());
    }
}

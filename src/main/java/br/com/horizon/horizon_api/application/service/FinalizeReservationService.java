package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.usecase.FinalizeReservationUseCase;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinalizeReservationService implements FinalizeReservationUseCase {

    private final ReservationPersistencePort reservationPort;

    @Override
    @Transactional
    public void execute(Long reservationId) {
        Reservation reservation = reservationPort.findById(reservationId).orElseThrow();
        if (reservation.getStatus() == ReservationStatus.CONFIRMADA) {
            reservation.setStatus(ReservationStatus.CONCLUIDA);
            reservationPort.save(reservation);
            log.info("Reservation {} finalized successfully", reservationId);
        }
    }
}

package br.com.horizon.horizon_api.infrastructure.config;

import br.com.horizon.horizon_api.domain.port.EmailPort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.ReservationEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripFeedbackBatchScheduler {

    private final ReservationRepository reservationRepository;
    private final EmailPort emailPort;

    /**
     * Batch job que roda periodicamente para:
     * 1. Concluir reservas CONFIRMADAS cujo voo de ida (ONE_WAY) ou volta (ROUND_TRIP) já aconteceu.
     * 2. Enviar um e-mail básico avisando que a avaliação da viagem foi liberada.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processCompletedTripsAndSendFeedbackEmails() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 1. Atualiza reservas CONFIRMADAS que já passaram da chegada da ida (ONE_WAY) ou volta (ROUND_TRIP)
        List<ReservationEntity> newlyCompleted = reservationRepository.findConfirmedReservationsPastEndDate(today, now);
        for (ReservationEntity res : newlyCompleted) {
            res.setStatus(ReservationStatus.CONCLUIDA);
            reservationRepository.save(res);
            log.info("Reserva #{} marcada como CONCLUIDA pela batch.", res.getId());
        }

        // 2. Envia o e-mail básico de avaliação para reservas CONCLUIDAS que ainda não receberam o e-mail
        List<ReservationEntity> readyForFeedback = reservationRepository.findCompletedReservationsReadyForFeedback(today);
        for (ReservationEntity res : readyForFeedback) {
            try {
                if (res.getUser() != null && res.getUser().getEmail() != null) {
                    String destinationName = res.getDestination() != null ? res.getDestination().getName() : "seu destino";
                    String subject = "Horizon - Avalie sua viagem para " + destinationName;
                    String feedbackLink = "http://localhost:4200/profile?openFeedback=trip&reservationId=" + res.getId();
                    String htmlBody = br.com.horizon.horizon_api.application.service.EmailTemplateService.buildTripFeedbackInvitationEmail(
                            res.getUser().getName(),
                            res.getId(),
                            destinationName,
                            feedbackLink
                    );

                    emailPort.sendEmail(res.getUser().getEmail(), subject, htmlBody);
                }
                res.setFeedbackEmailSent(true);
                reservationRepository.save(res);
                log.info("E-mail de avaliação enviado para a reserva #{}.", res.getId());
            } catch (Exception e) {
                log.error("Erro ao enviar e-mail de avaliação para a reserva #{}: {}", res.getId(), e.getMessage());
            }
        }
    }
}

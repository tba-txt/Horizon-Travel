package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.usecase.TriggerFeedbackUseCase;
import br.com.horizon.horizon_api.domain.model.Reservation;
import br.com.horizon.horizon_api.domain.port.EmailPort;
import br.com.horizon.horizon_api.domain.port.ReservationPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TriggerFeedbackService implements TriggerFeedbackUseCase {

    private final ReservationPersistencePort reservationPort;
    private final EmailPort emailPort;
    private final UserRepository userRepository;
    private final br.com.horizon.horizon_api.domain.port.DestinationPersistencePort destinationPort;

    @Value("${FRONTEND_BASE_URL:http://localhost:4200}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public void execute(Reservation reservation) {
        if (reservation.getFeedbackEmailSent() != null && reservation.getFeedbackEmailSent()) {
            return;
        }

        UserEntity user = userRepository.findById(reservation.getUserId()).orElseThrow();

        String destName = "seu destino";
        if (reservation.getDestinationId() != null && destinationPort != null) {
            destName = destinationPort.findById(reservation.getDestinationId())
                    .map(br.com.horizon.horizon_api.domain.model.Destination::getName)
                    .orElse("seu destino");
        }

        String feedbackLink = frontendBaseUrl + "/profile?openFeedback=trip&reservationId=" + reservation.getId();
        String subject = "Horizon - Avalie sua Viagem para " + destName;
        String htmlBody = EmailTemplateService.buildTripFeedbackInvitationEmail(
                user.getName(),
                reservation.getId(),
                destName,
                feedbackLink
        );

        try {
            emailPort.sendEmail(user.getEmail(), subject, htmlBody);
            
            reservation.setFeedbackEmailSent(true);
            reservationPort.save(reservation);
            log.info("Feedback email sent for Reservation ID: {}", reservation.getId());
        } catch (Exception e) {
            log.error("Failed to send feedback email for Reservation ID: {}", reservation.getId(), e);
        }
    }
}

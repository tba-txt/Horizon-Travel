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

    @Value("${FRONTEND_BASE_URL:http://localhost:3000}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public void execute(Reservation reservation) {
        if (reservation.getFeedbackEmailSent() != null && reservation.getFeedbackEmailSent()) {
            return;
        }

        UserEntity user = userRepository.findById(reservation.getUserId()).orElseThrow();

        String feedbackLink = frontendBaseUrl + "/feedback/trip/" + reservation.getId();
        String subject = "Horizon - Avalie sua Viagem";
        String body = "Ola " + user.getName() + ",\n\n" +
                "Esperamos que sua viagem tenha sido incrivel!\n" +
                "Gostariamos muito de saber sua opiniao. Por favor, acesse o link abaixo para nos enviar seu feedback:\n" +
                feedbackLink + "\n\n" +
                "Sua opiniao e muito importante para continuarmos melhorando nossos servicos.\n\n" +
                "Obrigado,\n" +
                "Equipe Horizon";

        try {
            emailPort.sendEmail(user.getEmail(), subject, body);
            
            reservation.setFeedbackEmailSent(true);
            reservationPort.save(reservation);
            log.info("Feedback email sent for Reservation ID: {}", reservation.getId());
        } catch (Exception e) {
            log.error("Failed to send feedback email for Reservation ID: {}", reservation.getId(), e);
        }
    }
}

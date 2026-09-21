package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.domain.model.Feedback;
import br.com.horizon.horizon_api.domain.port.FeedbackPersistencePort;
import br.com.horizon.horizon_api.application.usecase.RegisterFeedbackUseCase;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackService implements RegisterFeedbackUseCase {
    
    private final FeedbackPersistencePort port;

    private final br.com.horizon.horizon_api.domain.port.ReservationPersistencePort reservationPort;
    private final br.com.horizon.horizon_api.domain.port.FlightPersistencePort flightPort;
    private final br.com.horizon.horizon_api.application.mapper.FeedbackMapper mapper;

    @Override
    public void execute(br.com.horizon.horizon_api.application.dto.request.FeedbackRequest request) {
        if (request.getFeedbackType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType.NPS) {
            if (request.getScore() < 0 || request.getScore() > 10) throw new br.com.horizon.horizon_api.domain.exception.BusinessException("NPS must be between 0 and 10");
        } else if (request.getFeedbackType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType.CSAT) {
            if (request.getScore() < 1 || request.getScore() > 5) throw new br.com.horizon.horizon_api.domain.exception.BusinessException("CSAT must be between 1 and 5");
        }

        if (request.getTargetType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType.TRIP) {
            if (request.getReservationId() == null) {
                throw new br.com.horizon.horizon_api.domain.exception.BusinessException("Reservation ID is required for TRIP feedback");
            }
            br.com.horizon.horizon_api.domain.model.Reservation res = reservationPort.findByIdAndUserId(request.getReservationId(), request.getUserId())
                    .orElseThrow(() -> new br.com.horizon.horizon_api.domain.exception.ResourceNotFoundException("Reservation not found or access denied"));

            if (port.existsByUserIdAndReservationIdAndFeedbackTypeAndTargetType(request.getUserId(), request.getReservationId(), request.getFeedbackType(), request.getTargetType())) {
                throw new br.com.horizon.horizon_api.domain.exception.BusinessException("Feedback already submitted for this reservation");
            }

            br.com.horizon.horizon_api.domain.model.Flight flightToCheck = null;
            if (res.getTripType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType.ONE_WAY) {
                flightToCheck = flightPort.getFlightById(res.getOutboundFlightId()).orElseThrow();
            } else {
                flightToCheck = flightPort.getFlightById(res.getReturnFlightId()).orElseThrow();
            }

            java.time.LocalDate arrivalDate = flightToCheck.getFlightDate();
            java.time.LocalDate eligibilityDate = arrivalDate.plusDays(3);
            if (java.time.LocalDate.now().isBefore(eligibilityDate)) {
                throw new br.com.horizon.horizon_api.domain.exception.BusinessException("Feedback is not yet available for this trip");
            }
        } else if (request.getTargetType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType.PLATFORM) {
            if (port.existsByUserIdAndFeedbackTypeAndTargetType(request.getUserId(), request.getFeedbackType(), request.getTargetType())) {
                throw new br.com.horizon.horizon_api.domain.exception.BusinessException("Platform feedback already submitted by this user");
            }
        }

        Feedback domain = mapper.toDomain(request);
        domain.setCreatedAt(java.time.OffsetDateTime.now());
        port.save(domain);
    }
}

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
    @org.springframework.transaction.annotation.Transactional
    public void execute(br.com.horizon.horizon_api.application.dto.request.FeedbackRequest request) {
        if (request.getFeedbackType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType.NPS) {
            if (request.getScore() < 0 || request.getScore() > 10) {
                throw new br.com.horizon.horizon_api.domain.exception.BusinessException("A nota de recomendação deve ser entre 0 e 10.");
            }
        } else if (request.getFeedbackType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType.CSAT) {
            if (request.getScore() < 1 || request.getScore() > 5) {
                throw new br.com.horizon.horizon_api.domain.exception.BusinessException("A nota de satisfação deve ser entre 1 e 5.");
            }
        }

        if (request.getTargetType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType.TRIP) {
            if (request.getReservationId() == null) {
                throw new br.com.horizon.horizon_api.domain.exception.BusinessException("O identificador da reserva é obrigatório para avaliar a viagem.");
            }
            br.com.horizon.horizon_api.domain.model.Reservation res = reservationPort.findByIdAndUserId(request.getReservationId(), request.getUserId())
                    .orElseThrow(() -> new br.com.horizon.horizon_api.domain.exception.ResourceNotFoundException("Reserva não encontrada ou acesso negado."));

            if (port.existsByUserIdAndReservationIdAndFeedbackTypeAndTargetType(request.getUserId(), request.getReservationId(), request.getFeedbackType(), request.getTargetType())) {
                throw new br.com.horizon.horizon_api.domain.exception.BusinessException("Você já avaliou esta viagem.");
            }

            br.com.horizon.horizon_api.domain.model.Flight flightToCheck = null;
            if (res.getTripType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType.ONE_WAY || res.getReturnFlightId() == null) {
                flightToCheck = flightPort.getFlightById(res.getOutboundFlightId()).orElseThrow();
            } else {
                flightToCheck = flightPort.getFlightById(res.getReturnFlightId()).orElseThrow();
            }

            // Permitir avaliação para reservas CONFIRMADAS ou CONCLUIDAS
            if (res.getStatus() != br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus.CONFIRMADA &&
                res.getStatus() != br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus.CONCLUIDA) {
                throw new br.com.horizon.horizon_api.domain.exception.BusinessException("Apenas reservas confirmadas ou concluídas podem ser avaliadas.");
            }
        }

        Feedback domain = mapper.toDomain(request);
        domain.setCreatedAt(java.time.OffsetDateTime.now());
        port.save(domain);
    }

    public br.com.horizon.horizon_api.application.dto.response.FeedbackMetricsResponse getMetrics() {
        java.util.List<br.com.horizon.horizon_api.domain.model.Feedback> all = port.findAll();

        long platformTotal = 0;
        long platformNpsCount = 0;
        long platformPromoters = 0;
        long platformDetractors = 0;
        long platformCsatCount = 0;
        double platformCsatSum = 0;

        long tripTotal = 0;
        long tripNpsCount = 0;
        long tripPromoters = 0;
        long tripDetractors = 0;
        long tripCsatCount = 0;
        double tripCsatSum = 0;

        for (br.com.horizon.horizon_api.domain.model.Feedback f : all) {
            if (f.getScore() == null) continue;
            int score = f.getScore();

            if (f.getTargetType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType.PLATFORM) {
                platformTotal++;
                if (f.getFeedbackType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType.NPS) {
                    platformNpsCount++;
                    if (score >= 9) platformPromoters++;
                    else if (score <= 6) platformDetractors++;
                } else if (f.getFeedbackType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType.CSAT) {
                    platformCsatCount++;
                    platformCsatSum += score;
                }
            } else if (f.getTargetType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType.TRIP) {
                tripTotal++;
                if (f.getFeedbackType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType.NPS) {
                    tripNpsCount++;
                    if (score >= 9) tripPromoters++;
                    else if (score <= 6) tripDetractors++;
                } else if (f.getFeedbackType() == br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType.CSAT) {
                    tripCsatCount++;
                    tripCsatSum += score;
                }
            }
        }

        double platformNps = platformNpsCount > 0 ? ((double) (platformPromoters - platformDetractors) / platformNpsCount) * 100 : 0.0;
        double platformCsat = platformCsatCount > 0 ? platformCsatSum / platformCsatCount : 0.0;

        double tripNps = tripNpsCount > 0 ? ((double) (tripPromoters - tripDetractors) / tripNpsCount) * 100 : 0.0;
        double tripCsat = tripCsatCount > 0 ? tripCsatSum / tripCsatCount : 0.0;

        return br.com.horizon.horizon_api.application.dto.response.FeedbackMetricsResponse.builder()
                .platformTotalFeedbacks(platformTotal)
                .platformNps(platformNps)
                .platformCsat(platformCsat)
                .tripTotalFeedbacks(tripTotal)
                .tripNps(tripNps)
                .tripCsat(tripCsat)
                .build();
    }

    public java.util.List<br.com.horizon.horizon_api.application.dto.response.FeedbackResponse> getMyFeedbacks(Long userId) {
        return port.findAll().stream()
                .filter(f -> userId.equals(f.getUserId()))
                .map(mapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }
}

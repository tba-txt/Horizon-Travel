package br.com.horizon.horizon_api.application.mapper;

import br.com.horizon.horizon_api.domain.model.Feedback;
import br.com.horizon.horizon_api.application.dto.request.FeedbackRequest;
import br.com.horizon.horizon_api.application.dto.response.FeedbackResponse;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.FeedbackEntity;
import org.springframework.stereotype.Component;

@Component
public class FeedbackMapper {
    
    public Feedback toDomain(FeedbackEntity entity) {
        if (entity == null) return null;
        Feedback domain = new Feedback();
        domain.setId(entity.getId());
        domain.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        domain.setReservationId(entity.getReservation() != null ? entity.getReservation().getId() : null);
        domain.setFeedbackType(entity.getFeedbackType());
        domain.setTargetType(entity.getTargetType());
        domain.setScore(entity.getScore());
        domain.setComment(entity.getComment());
        domain.setCreatedAt(entity.getAnsweredAt());
        return domain;
    }
    
    public FeedbackEntity toEntity(Feedback domain) {
        if (domain == null) return null;
        FeedbackEntity entity = new FeedbackEntity();
        entity.setId(domain.getId());
        entity.setFeedbackType(domain.getFeedbackType());
        entity.setTargetType(domain.getTargetType());
        entity.setScore(domain.getScore());
        entity.setComment(domain.getComment());
        entity.setAnsweredAt(domain.getCreatedAt());
        // User and Reservation are mapped in the adapter
        return entity;
    }
    
    public Feedback toDomain(FeedbackRequest request) {
        if (request == null) return null;
        Feedback domain = new Feedback();
        domain.setUserId(request.getUserId());
        domain.setReservationId(request.getReservationId());
        domain.setFeedbackType(request.getFeedbackType());
        domain.setTargetType(request.getTargetType());
        domain.setScore(request.getScore());
        domain.setComment(request.getComment());
        return domain;
    }
    
    public FeedbackResponse toResponse(Feedback domain) {
        if (domain == null) return null;
        FeedbackResponse response = new FeedbackResponse();
        response.setId(domain.getId());
        response.setUserId(domain.getUserId());
        response.setReservationId(domain.getReservationId());
        response.setFeedbackType(domain.getFeedbackType());
        response.setTargetType(domain.getTargetType());
        response.setScore(domain.getScore());
        response.setComment(domain.getComment());
        response.setCreatedAt(domain.getCreatedAt());
        return response;
    }
}

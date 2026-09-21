package br.com.horizon.horizon_api.domain.port;

import br.com.horizon.horizon_api.domain.model.Feedback;
import java.util.List;
import java.util.Optional;

public interface FeedbackPersistencePort {
    Feedback save(Feedback feedback);
    Optional<Feedback> findById(Long id);
    List<Feedback> findAll();
    void delete(Long id);
    boolean existsByUserIdAndReservationIdAndFeedbackTypeAndTargetType(Long userId, Long reservationId, br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType feedbackType, br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType targetType);
    boolean existsByUserIdAndFeedbackTypeAndTargetType(Long userId, br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType feedbackType, br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType targetType);
}

package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.FeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {
    boolean existsByUserIdAndReservation_IdAndFeedbackTypeAndTargetType(
            Long userId, Long reservationId, 
            br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType feedbackType, 
            br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType targetType);

    boolean existsByUserIdAndFeedbackTypeAndTargetType(
            Long userId, 
            br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType feedbackType, 
            br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType targetType);

    List<FeedbackEntity> findByAnsweredAtAfter(OffsetDateTime date);
}


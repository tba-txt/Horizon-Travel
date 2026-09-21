package br.com.horizon.horizon_api.application.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackResponse {
    private Long id;
    private Long userId;
    private Long reservationId;
    private br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType feedbackType;
    private br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType targetType;
    private Integer score;
    private String comment;
    private java.time.OffsetDateTime createdAt;
}

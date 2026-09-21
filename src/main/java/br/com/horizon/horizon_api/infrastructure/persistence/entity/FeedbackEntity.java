package br.com.horizon.horizon_api.infrastructure.persistence.entity;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.FeedbackType;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "feedbacks")
@Getter
@Setter
public class FeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private ReservationEntity reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", length = 20)
    private FeedbackType feedbackType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    private TargetType targetType;

    @Column(name = "score")
    private Integer score;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "available_at")
    private OffsetDateTime availableAt;

    @Column(name = "answered_at")
    private OffsetDateTime answeredAt;
}

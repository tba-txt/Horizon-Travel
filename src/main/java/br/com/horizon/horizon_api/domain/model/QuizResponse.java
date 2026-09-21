package br.com.horizon.horizon_api.domain.model;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class QuizResponse {
    private Long id; private Long userId; private Long quizId; private Long questionId; private Long answerId; private java.util.UUID attemptId; private java.time.OffsetDateTime answeredAt;
}

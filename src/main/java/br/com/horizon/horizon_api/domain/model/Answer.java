package br.com.horizon.horizon_api.domain.model;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class Answer {
    private Long id; private Long questionId; private String answerText; private java.util.Map<Long, java.math.BigDecimal> attributes = new java.util.HashMap<>();
}

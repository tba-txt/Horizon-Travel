package br.com.horizon.horizon_api.domain.model;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class Question {
    private Long id; private Long quizId; private String questionText; private Integer questionOrder; private Boolean active;
}

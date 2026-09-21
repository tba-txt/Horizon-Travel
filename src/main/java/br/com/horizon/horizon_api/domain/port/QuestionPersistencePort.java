package br.com.horizon.horizon_api.domain.port;
import br.com.horizon.horizon_api.domain.model.*;
public interface QuestionPersistencePort {
    java.util.List<Question> getQuestionsByQuizId(Long quizId);
}

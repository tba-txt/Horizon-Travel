package br.com.horizon.horizon_api.domain.port;
import br.com.horizon.horizon_api.domain.model.*;
public interface AnswerPersistencePort {
    java.util.List<Answer> getAnswersByQuestionId(Long questionId); java.util.List<Answer> getAnswersByIds(java.util.List<Long> answerIds);
}

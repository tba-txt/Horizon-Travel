package br.com.horizon.horizon_api.domain.port;
import br.com.horizon.horizon_api.domain.model.*;
public interface QuizPersistencePort {
    java.util.Optional<Quiz> getActiveQuiz();
}

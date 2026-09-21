package br.com.horizon.horizon_api.domain.port;
import br.com.horizon.horizon_api.domain.model.*;
public interface QuizResponsePersistencePort {
    void saveAll(java.util.List<QuizResponse> responses); java.util.List<QuizResponse> getHistoryByUserId(Long userId);
}

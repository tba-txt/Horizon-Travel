package br.com.horizon.horizon_api.infrastructure.adapter;
import br.com.horizon.horizon_api.domain.model.Quiz;
import br.com.horizon.horizon_api.domain.port.QuizPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.QuizRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QuizPersistenceAdapter implements QuizPersistencePort {
    private final QuizRepository repo;
    
    @Override
    public Optional<Quiz> getActiveQuiz() {
        return repo.findByActiveTrue().map(e -> {
            Quiz q = new Quiz();
            q.setId(e.getId());
            q.setTitle(e.getTitle());
            q.setVersion(e.getVersion());
            q.setActive(e.getActive());
            q.setCreatedAt(e.getCreatedAt());
            return q;
        });
    }
}

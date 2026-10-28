package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.domain.model.Question;
import br.com.horizon.horizon_api.domain.port.QuestionPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.QuestionEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.QuestionRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class QuestionPersistenceAdapter implements QuestionPersistencePort {
    private final QuestionRepository repo;
    
    @Override
    public List<Question> getQuestionsByQuizId(Long quizId) {
        return repo.findByQuiz_IdAndActiveTrue(quizId).stream()
            .sorted(Comparator.comparing(QuestionEntity::getQuestionOrder, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(e -> {
                Question q = new Question();
                q.setId(e.getId());
                q.setQuizId(e.getQuiz().getId());
                q.setQuestionText(e.getQuestionText());
                q.setQuestionOrder(e.getQuestionOrder());
                q.setActive(e.getActive());
                return q;
            }).collect(Collectors.toList());
    }
}

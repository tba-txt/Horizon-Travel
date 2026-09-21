package br.com.horizon.horizon_api.infrastructure.adapter;
import br.com.horizon.horizon_api.domain.model.Answer;
import br.com.horizon.horizon_api.domain.port.AnswerPersistencePort;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.AnswerRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AnswerPersistenceAdapter implements AnswerPersistencePort {
    private final AnswerRepository repo;
    
    private Answer map(br.com.horizon.horizon_api.infrastructure.persistence.entity.AnswerEntity e) {
        Answer a = new Answer();
        a.setId(e.getId());
        a.setQuestionId(e.getQuestion().getId());
        a.setAnswerText(e.getAnswerText());
        if (e.getAttributes() != null) {
            for (var attr : e.getAttributes()) {
                a.getAttributes().put(attr.getAttribute().getId(), attr.getWeight());
            }
        }
        return a;
    }
    
    @Override
    public List<Answer> getAnswersByQuestionId(Long questionId) {
        return repo.findByQuestion_Id(questionId).stream().map(this::map).collect(Collectors.toList());
    }
    @Override
    public List<Answer> getAnswersByIds(List<Long> answerIds) {
        return repo.findAllById(answerIds).stream().map(this::map).collect(Collectors.toList());
    }
}

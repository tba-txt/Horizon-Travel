package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.QuestionResponseDTO;
import java.util.List;

public interface GetQuizQuestionsUseCase {
    List<QuestionResponseDTO> execute(Long quizId);
}

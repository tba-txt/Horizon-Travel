package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.QuizResponseDTO;

public interface GetActiveQuizUseCase {
    QuizResponseDTO execute();
}

package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.SubmitQuizRequest;

public interface SubmitQuizUseCase {
    void execute(SubmitQuizRequest req);
}

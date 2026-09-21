package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.FeedbackRequest;

public interface RegisterFeedbackUseCase {
    void execute(FeedbackRequest request);
}

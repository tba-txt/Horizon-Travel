package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.InteractionRequest;

public interface RegisterInteractionUseCase {
    void execute(InteractionRequest req);
}

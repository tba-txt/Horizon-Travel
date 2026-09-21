package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.request.PostRequest;
import br.com.horizon.horizon_api.application.dto.response.PostResponseDTO;

public interface CreatePostUseCase {
    PostResponseDTO execute(PostRequest req);
}

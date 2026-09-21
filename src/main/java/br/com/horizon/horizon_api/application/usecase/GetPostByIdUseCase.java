package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.PostResponseDTO;

public interface GetPostByIdUseCase {
    PostResponseDTO execute(Long id);
}

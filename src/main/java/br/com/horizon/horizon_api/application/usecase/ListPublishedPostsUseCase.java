package br.com.horizon.horizon_api.application.usecase;

import br.com.horizon.horizon_api.application.dto.response.PostResponseDTO;
import java.util.List;

public interface ListPublishedPostsUseCase {
    List<PostResponseDTO> execute();
}

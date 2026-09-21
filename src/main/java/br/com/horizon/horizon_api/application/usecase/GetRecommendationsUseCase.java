package br.com.horizon.horizon_api.application.usecase;
import br.com.horizon.horizon_api.application.dto.response.RecommendationResponseDTO;
import java.util.List;
public interface GetRecommendationsUseCase {
    List<RecommendationResponseDTO> execute(Long userId, Integer limit);
}

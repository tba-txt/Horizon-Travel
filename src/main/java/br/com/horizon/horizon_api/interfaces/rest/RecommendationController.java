package br.com.horizon.horizon_api.interfaces.rest;
import br.com.horizon.horizon_api.application.dto.response.RecommendationResponseDTO;
import br.com.horizon.horizon_api.application.usecase.GetRecommendationsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recomendações", description = "Endpoints para o motor de recomendação de destinos")
public class RecommendationController {
    private final GetRecommendationsUseCase recommendationUseCase;
    
    @Operation(summary = "Obter recomendações", description = "Retorna uma lista de destinos recomendados com base no perfil explícito (quiz) e comportamental (interações).")
    @GetMapping
    public ResponseEntity<List<RecommendationResponseDTO>> getRecommendations(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        log.info("Recebida requisição GET /recommendations para userId={}, limit={}", userId, limit);
        List<RecommendationResponseDTO> result = recommendationUseCase.execute(userId, limit);
        log.info("Retornando {} recomendações para userId={}", result.size(), userId);
        return ResponseEntity.ok(result);
    }
}

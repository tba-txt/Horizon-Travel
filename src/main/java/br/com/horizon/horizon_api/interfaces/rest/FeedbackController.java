package br.com.horizon.horizon_api.interfaces.rest;

import br.com.horizon.horizon_api.application.dto.request.FeedbackRequest;
import br.com.horizon.horizon_api.application.dto.response.FeedbackResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final br.com.horizon.horizon_api.application.usecase.RegisterFeedbackUseCase registerFeedbackUseCase;

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody FeedbackRequest request) {
        request.setUserId(SecurityUtils.getAuthenticatedUserId());
        registerFeedbackUseCase.execute(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/metrics")
    public ResponseEntity<br.com.horizon.horizon_api.application.dto.response.FeedbackMetricsResponse> getMetrics() {
        return ResponseEntity.ok(((br.com.horizon.horizon_api.application.service.FeedbackService) registerFeedbackUseCase).getMetrics());
    }

    @GetMapping("/my")
    public ResponseEntity<List<FeedbackResponse>> getMyFeedbacks() {
        return ResponseEntity.ok(((br.com.horizon.horizon_api.application.service.FeedbackService) registerFeedbackUseCase).getMyFeedbacks(SecurityUtils.getAuthenticatedUserId()));
    }
}

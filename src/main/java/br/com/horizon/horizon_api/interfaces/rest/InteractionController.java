package br.com.horizon.horizon_api.interfaces.rest;
import br.com.horizon.horizon_api.application.dto.request.InteractionRequest;
import br.com.horizon.horizon_api.application.service.InteractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/interactions")
@RequiredArgsConstructor
@Tag(name = "Interações", description = "Endpoints para interação (Like/Dislike) com posts")
public class InteractionController {
    private final InteractionService service;
    
    @Operation(summary = "Registrar interação", description = "Registra um like ou dislike em um post, influenciando o perfil comportamental do usuário.")
    @PostMapping
    public ResponseEntity<Void> interact(@Valid @RequestBody InteractionRequest request) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        log.info("Recebida requisição POST /interactions para userId={}, postId={}, type={}", userId, request.getPostId(), request.getInteractionType());
        request.setUserId(userId);
        service.execute(request);
        log.info("Interação registrada com sucesso");
        return ResponseEntity.ok().build();
    }
}

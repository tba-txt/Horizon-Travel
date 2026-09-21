package br.com.horizon.horizon_api.interfaces.rest;
import br.com.horizon.horizon_api.application.dto.request.SubmitQuizRequest;
import br.com.horizon.horizon_api.application.dto.response.*;
import br.com.horizon.horizon_api.application.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "Endpoints para o Quiz de Perfil")
public class QuizController {
    private final QuizService service;
    
    @Operation(summary = "Obter quiz ativo", description = "Retorna o quiz de perfil de viajante ativo no sistema.")
    @GetMapping("/active")
    public ResponseEntity<QuizResponseDTO> getActive() {
        log.info("Recebida requisição GET /quizzes/active");
        QuizResponseDTO result = service.execute();
        log.info("Retornando quiz ativo, id={}", result != null ? result.getId() : "null");
        return ResponseEntity.ok(result);
    }
    
    @Operation(summary = "Obter perguntas do quiz", description = "Retorna as perguntas e opções de resposta para um dado quiz.")
    @GetMapping("/{id}/questions")
    public ResponseEntity<List<QuestionResponseDTO>> getQuestions(@PathVariable Long id) {
        log.info("Recebida requisição GET /quizzes/{}/questions", id);
        return ResponseEntity.ok(service.execute(id));
    }
    
    @Operation(summary = "Submeter respostas", description = "Submete as respostas do quiz e o orçamento para formar o perfil explícito do usuário.")
    @PostMapping({"/submit", "/{id}/submit"})
    public ResponseEntity<Void> submit(@PathVariable(required = false) Long id, @Valid @RequestBody SubmitQuizRequest request) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        log.info("Recebida requisição POST /quizzes/submit (quizId={}) para userId={}", id, userId);
        request.setUserId(userId);
        service.execute(request);
        log.info("Quiz submetido com sucesso para userId={}", userId);
        return ResponseEntity.ok().build();
    }
}

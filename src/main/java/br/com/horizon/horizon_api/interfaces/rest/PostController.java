package br.com.horizon.horizon_api.interfaces.rest;
import br.com.horizon.horizon_api.application.dto.response.PostResponseDTO;
import br.com.horizon.horizon_api.application.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService service;
    
    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDTO> getById(@PathVariable Long id) {
        log.info("Recebida requisição GET /posts/{}", id);
        return ResponseEntity.ok(service.execute(id));
    }
    
    @GetMapping
    public ResponseEntity<List<PostResponseDTO>> listPublished() {
        log.info("Recebida requisição GET /posts");
        List<PostResponseDTO> result = service.execute();
        log.info("Retornando {} posts", result.size());
        return ResponseEntity.ok(result);
    }
}

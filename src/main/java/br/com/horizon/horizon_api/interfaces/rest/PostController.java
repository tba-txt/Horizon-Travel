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
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .body(service.execute(id));
    }
    
    @GetMapping
    public ResponseEntity<List<PostResponseDTO>> listPublished(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "100") Integer size) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Object principal = (auth != null) ? auth.getPrincipal() : "anonymous";
        log.info("Recebida requisição GET /posts para principal={}", principal);
        List<PostResponseDTO> result = service.execute(page, size);
        log.info("Retornando {} posts para principal={}", result.size(), principal);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .body(result);
    }
}

package br.com.horizon.horizon_api.interfaces.rest;
import br.com.horizon.horizon_api.application.dto.response.ProfileAttributeDTO;
import br.com.horizon.horizon_api.application.dto.response.ProfileResponseDTO;
import br.com.horizon.horizon_api.application.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
@Tag(name = "Perfis", description = "Endpoints para consulta de perfis explícito e comportamental")
public class ProfileController {
    private final ProfileService service;
    
    @Operation(summary = "Obter perfil explícito", description = "Retorna o perfil formado a partir das respostas do quiz.")
    @GetMapping("/explicit")
    public ResponseEntity<ProfileResponseDTO> getExplicit() {
        return ResponseEntity.ok(service.executeExplicit(SecurityUtils.getAuthenticatedUserId()));
    }
    
    @Operation(summary = "Obter perfil comportamental", description = "Retorna o perfil formado a partir das interações (like/dislike) com os posts.")
    @GetMapping("/behavioral")
    public ResponseEntity<ProfileResponseDTO> getBehavioral() {
        return ResponseEntity.ok(service.executeBehavior(SecurityUtils.getAuthenticatedUserId()));
    }
}

package br.com.horizon.horizon_api.interfaces.rest;

import br.com.horizon.horizon_api.application.dto.request.UserRequest;
import br.com.horizon.horizon_api.application.dto.response.UserResponse;
import br.com.horizon.horizon_api.application.usecase.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Endpoints para cadastro e gerenciamento de conta")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;
    private final UploadProfilePictureUseCase uploadProfilePictureUseCase;

    @Operation(summary = "Cadastrar nova conta / Registrar usuário", description = "Cria uma nova conta de usuário no sistema com perfil ativo.")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createUserUseCase.execute(request));
    }

    @Operation(summary = "Obter dados da minha conta", description = "Retorna os dados do perfil do usuário autenticado.")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe() {
        Long id = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(getUserByIdUseCase.execute(id));
    }

    @Operation(summary = "Atualizar meu perfil", description = "Atualiza os dados pessoais e preferências do usuário autenticado.")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody UserRequest request) {
        Long id = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(updateUserUseCase.execute(id, request));
    }

    @Operation(summary = "Desativar minha conta", description = "Desativa logicamente a conta do usuário autenticado.")
    @DeleteMapping("/me/deactivate")
    public ResponseEntity<Void> deactivateMe() {
        Long id = SecurityUtils.getAuthenticatedUserId();
        deactivateUserUseCase.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Atualizar foto de perfil", description = "Faz upload de uma nova foto para o Cloudinary e atualiza o perfil do usuário.")
    @PostMapping(value = "/me/profile-picture", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadProfilePicture(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        Long id = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(uploadProfilePictureUseCase.execute(id, file));
    }
}

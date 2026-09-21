package br.com.horizon.horizon_api.interfaces.rest;

import br.com.horizon.horizon_api.application.dto.request.ForgotPasswordRequest;
import br.com.horizon.horizon_api.application.dto.request.LoginRequest;
import br.com.horizon.horizon_api.application.dto.request.ResetPasswordRequest;
import br.com.horizon.horizon_api.application.dto.response.LoginResponse;
import br.com.horizon.horizon_api.application.usecase.ForgotPasswordUseCase;
import br.com.horizon.horizon_api.application.usecase.LoginUseCase;
import br.com.horizon.horizon_api.application.usecase.ResetPasswordUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.horizon.horizon_api.application.dto.request.UserRequest;
import br.com.horizon.horizon_api.application.dto.response.UserResponse;
import br.com.horizon.horizon_api.application.usecase.CreateUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para autenticação, login e cadastro de conta")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final CreateUserUseCase createUserUseCase;

    @Operation(summary = "Login / Obter token JWT", description = "Autentica o usuário e retorna o token JWT.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.execute(request));
    }

    @Operation(summary = "Cadastrar nova conta (atalho via /auth/register)", description = "Permite o cadastro de uma nova conta diretamente pela rota de autenticação.")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createUserUseCase.execute(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request);
        return ResponseEntity.ok().build();
    }
}

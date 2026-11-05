package br.com.horizon.horizon_api.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequest {
    @NotBlank(message = "O token do Google é obrigatório.")
    private String idToken;
}

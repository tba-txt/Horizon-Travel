package br.com.horizon.horizon_api.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "A senha atual é obrigatória.")
    private String currentPassword;

    @NotBlank(message = "A nova senha é obrigatória.")
    @Size(min = 6, max = 100, message = "A nova senha deve ter entre 6 e 100 caracteres.")
    private String newPassword;

    @NotBlank(message = "A confirmação de senha é obrigatória.")
    private String confirmPassword;
}

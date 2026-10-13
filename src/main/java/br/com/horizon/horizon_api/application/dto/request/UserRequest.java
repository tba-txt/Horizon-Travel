package br.com.horizon.horizon_api.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class UserRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String socialName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "CPF is required")
    @Size(min = 11, max = 11, message = "CPF must be 11 digits")
    private String cpf;

    @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres.")
    private String password;

    @NotNull(message = "A data de nascimento é obrigatória.")
    @jakarta.validation.constraints.Past(message = "A data de nascimento deve ser uma data no passado.")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    @io.swagger.v3.oas.annotations.media.Schema(example = "1990-01-01", type = "string", format = "date")
    private LocalDate birthDate;
    
    private String profileImageUrl;
}

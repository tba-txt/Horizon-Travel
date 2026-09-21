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

    private String password;

    @NotNull(message = "Birth date is required")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    @io.swagger.v3.oas.annotations.media.Schema(example = "1990-01-01", type = "string", format = "date")
    private LocalDate birthDate;
    
    private String profileImageUrl;
}

package br.com.horizon.horizon_api.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportTicketRequest {

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 150, message = "O nome deve ter entre 2 e 150 caracteres")
    private String userName;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    private String userEmail;

    @NotBlank(message = "O assunto é obrigatório")
    private String subject;

    private Integer slaHours;

    private String slaDescription;

    @NotBlank(message = "A descrição do chamado é obrigatória")
    @Size(min = 10, max = 3000, message = "A descrição deve ter entre 10 e 3000 caracteres")
    private String description;

    private String attachmentName;

    private Long attachmentSize;

    private String attachmentType;

    private String attachmentData;
}

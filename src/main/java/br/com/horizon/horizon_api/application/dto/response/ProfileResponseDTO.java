package br.com.horizon.horizon_api.application.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter @Setter
public class ProfileResponseDTO {
    
    @Schema(description = "Indica se o usuário já possui perfil construído", example = "true")
    private boolean hasProfile;
    
    @Schema(description = "Mensagem explicativa sobre o status do perfil", example = "Perfil construído com sucesso.")
    private String message;
    
    @Schema(description = "Lista de atributos do perfil")
    private List<ProfileAttributeDTO> attributes;
}

package br.com.horizon.horizon_api.application.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class PostRequest {
    @NotBlank private String title;
    @NotBlank private String caption;
    private String imageUrl;
    private Long destinationId;
    private Boolean published;
}

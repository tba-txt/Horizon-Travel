package br.com.horizon.horizon_api.application.dto.response;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.List;
@Getter @Setter
public class PostResponseDTO {
    private Long id;
    private String title;
    private String caption;
    private String imageUrl;
    private Long destinationId;
    private Boolean published;
    private OffsetDateTime createdAt;
    private List<PostAttributeDTO> attributes;
    private Long likesCount;
    private Long dislikesCount;
    private br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.InteractionType userInteraction;
}

package br.com.horizon.horizon_api.domain.model;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class Post {
    private Long id; private String title; private String caption; private String imageUrl; private Long destinationId; private Boolean published; private java.time.OffsetDateTime createdAt; private java.time.OffsetDateTime updatedAt;
}

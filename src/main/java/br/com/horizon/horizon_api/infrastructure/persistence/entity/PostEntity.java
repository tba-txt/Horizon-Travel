package br.com.horizon.horizon_api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_post_published", columnList = "published")
})
@Getter
@Setter
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 150)
    private String title;

    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;


    @Column(name = "published")
    private Boolean published;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private java.util.List<PostAttributeEntity> attributes = new java.util.ArrayList<>();

    @jakarta.persistence.Column(name="likes_count", nullable=false, columnDefinition="BIGINT DEFAULT 0")
    private Long likesCount = 0L;

    @jakarta.persistence.Column(name="dislikes_count", nullable=false, columnDefinition="BIGINT DEFAULT 0")
    private Long dislikesCount = 0L;

    @Column(name = "post_type", length = 50)
    private String postType = "DEFAULT";
}

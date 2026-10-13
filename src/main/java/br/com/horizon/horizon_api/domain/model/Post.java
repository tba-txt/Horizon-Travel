package br.com.horizon.horizon_api.domain.model;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class Post {
    private Long id; private String title; private String caption; private String imageUrl; private Boolean published; private java.time.OffsetDateTime createdAt; private java.time.OffsetDateTime updatedAt;
    private Long likesCount = 0L;
    private Long dislikesCount = 0L;
    private String postType = "DEFAULT";
    private java.util.List<PostAttribute> attributes = new java.util.ArrayList<>();
    
    @Getter @Setter
    public static class PostOption {
        private Long id;
        private String label;
        private String image;
        private Integer votes;
    }
    private java.util.List<PostOption> options = new java.util.ArrayList<>();
}

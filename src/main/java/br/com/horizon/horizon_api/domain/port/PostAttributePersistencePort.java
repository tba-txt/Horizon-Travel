package br.com.horizon.horizon_api.domain.port;
import br.com.horizon.horizon_api.domain.model.*;
public interface PostAttributePersistencePort {
    java.util.List<PostAttribute> findByPostId(Long postId);
}

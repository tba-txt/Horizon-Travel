package br.com.horizon.horizon_api.domain.port;
import br.com.horizon.horizon_api.domain.model.*;
public interface PostPersistencePort {
    Post save(Post post); java.util.Optional<Post> findById(Long id); java.util.List<Post> findPublished();
}

package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long> {
    
    java.util.List<br.com.horizon.horizon_api.infrastructure.persistence.entity.PostEntity> findByPublishedTrue(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"attributes", "attributes.attribute"})
    java.util.Optional<br.com.horizon.horizon_api.infrastructure.persistence.entity.PostEntity> findById(Long id);
}
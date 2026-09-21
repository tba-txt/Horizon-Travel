package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.PostAttributeEntity;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.PostAttributeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostAttributeRepository extends JpaRepository<PostAttributeEntity, PostAttributeId> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"attribute"})
    java.util.List<br.com.horizon.horizon_api.infrastructure.persistence.entity.PostAttributeEntity> findByPost_Id(Long postId);
}

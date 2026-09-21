package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.BehaviorProfileAttributeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BehaviorProfileAttributeRepository extends JpaRepository<BehaviorProfileAttributeEntity, Long> {
    java.util.Optional<br.com.horizon.horizon_api.infrastructure.persistence.entity.BehaviorProfileAttributeEntity> findByUser_IdAndAttribute_Id(Long userId, Long attributeId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"attribute"})
    java.util.List<br.com.horizon.horizon_api.infrastructure.persistence.entity.BehaviorProfileAttributeEntity> findByUser_Id(Long userId);
}

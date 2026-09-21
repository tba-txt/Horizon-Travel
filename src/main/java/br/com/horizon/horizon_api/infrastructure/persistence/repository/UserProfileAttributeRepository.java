package br.com.horizon.horizon_api.infrastructure.persistence.repository;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.UserProfileAttributeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileAttributeRepository extends JpaRepository<UserProfileAttributeEntity, Long> {
    void deleteByUser_Id(Long userId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"attribute"})
    java.util.List<br.com.horizon.horizon_api.infrastructure.persistence.entity.UserProfileAttributeEntity> findByUser_Id(Long userId);
}

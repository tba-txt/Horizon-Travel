package br.com.horizon.horizon_api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface BaseJpaRepository<T extends BaseEntityEntity> extends JpaRepository<T, UUID>, JpaSpecificationExecutor<T> {
	
	Optional<T> findById(UUID id);
}

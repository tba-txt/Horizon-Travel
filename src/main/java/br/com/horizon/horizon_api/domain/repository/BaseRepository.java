package br.com.horizon.horizon_api.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BaseRepository<T> {
	
	T save(T entity);
	
	Optional<T> findById(UUID id);
	
	List<T> findAll();
	
	void delete(UUID id);
	
	boolean existsById(UUID id);
}

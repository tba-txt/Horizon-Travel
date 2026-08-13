package br.com.horizon.horizon_api.application.mapper;

import java.util.List;

public interface BaseMapper<D, E> {
	
	E toEntity(D dto);
	
	D toDTO(E entity);
	
	List<E> toEntityList(List<D> dtoList);
	
	List<D> toDTOList(List<E> entityList);
}

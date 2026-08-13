package br.com.horizon.horizon_api.application.dto;

import java.util.UUID;

public abstract class BaseDTO {
	
	private UUID id;
	
	public UUID getId() {
		return id;
	}
	
	public void setId(UUID id) {
		this.id = id;
	}
}

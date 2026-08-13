package br.com.horizon.horizon_api.domain.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public abstract class BaseEntity {
	
	private UUID id;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
	protected BaseEntity() {
		this.id = UUID.randomUUID();
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}
	
	protected BaseEntity(UUID id) {
		this.id = Objects.requireNonNull(id, "ID cannot be null");
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}
	
	public UUID getId() {
		return id;
	}
	
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
	
	public void updateTimestamp() {
		this.updatedAt = LocalDateTime.now();
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BaseEntity that = (BaseEntity) o;
		return Objects.equals(id, that.id);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}

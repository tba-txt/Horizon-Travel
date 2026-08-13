package br.com.horizon.horizon_api.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

public abstract class BaseController {
	
	protected <T> ResponseEntity<T> ok(T body) {
		return ResponseEntity.ok(body);
	}
	
	protected <T> ResponseEntity<T> created(T body) {
		return ResponseEntity.status(201).body(body);
	}
	
	protected <T> ResponseEntity<T> noContent() {
		return ResponseEntity.noContent().build();
	}
	
	protected UUID parseUUID(String id) {
		try {
			return UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid UUID format: " + id);
		}
	}
}

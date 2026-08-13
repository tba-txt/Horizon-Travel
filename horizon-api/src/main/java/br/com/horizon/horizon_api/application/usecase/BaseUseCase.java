package br.com.horizon.horizon_api.application.usecase;

public interface BaseUseCase<I, O> {
	
	O execute(I input);
}

package br.com.horizon.horizon_api.domain.valueobject;

import java.util.Objects;

public abstract class BaseValueObject {
	
	@Override
	public abstract boolean equals(Object o);
	
	@Override
	public abstract int hashCode();
}

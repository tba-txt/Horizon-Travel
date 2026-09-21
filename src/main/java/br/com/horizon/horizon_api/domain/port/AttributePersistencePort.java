package br.com.horizon.horizon_api.domain.port;
import br.com.horizon.horizon_api.domain.model.*;
public interface AttributePersistencePort {
    java.util.Optional<Attribute> findById(Long id);
}

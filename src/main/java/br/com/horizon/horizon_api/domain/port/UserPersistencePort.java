package br.com.horizon.horizon_api.domain.port;

import br.com.horizon.horizon_api.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface UserPersistencePort {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    void delete(Long id);
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);
}

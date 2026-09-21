package br.com.horizon.horizon_api.domain.port;

import br.com.horizon.horizon_api.domain.model.User;

public interface TokenPort {
    String generateToken(User user);
    String validateToken(String token);
}

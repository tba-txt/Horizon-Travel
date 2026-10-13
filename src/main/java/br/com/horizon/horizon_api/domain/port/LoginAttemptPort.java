package br.com.horizon.horizon_api.domain.port;

public interface LoginAttemptPort {
    void loginSucceeded(String key);
    void loginFailed(String key);
    boolean isBlocked(String key);
}

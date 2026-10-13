package br.com.horizon.horizon_api.infrastructure.adapter;

import br.com.horizon.horizon_api.domain.port.LoginAttemptPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryLoginAttemptAdapter implements LoginAttemptPort {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 15 * 60; // 15 minutos

    private static class AttemptInfo {
        int attempts;
        Instant lastAttempt;

        AttemptInfo(int attempts, Instant lastAttempt) {
            this.attempts = attempts;
            this.lastAttempt = lastAttempt;
        }
    }

    private final Map<String, AttemptInfo> attemptsCache = new ConcurrentHashMap<>();

    @Override
    public void loginSucceeded(String key) {
        attemptsCache.remove(key.toLowerCase());
    }

    @Override
    public void loginFailed(String key) {
        String normalizedKey = key.toLowerCase();
        cleanupExpiredEntries();
        attemptsCache.compute(normalizedKey, (k, existing) -> {
            Instant now = Instant.now();
            if (existing == null) {
                return new AttemptInfo(1, now);
            }
            if (now.isAfter(existing.lastAttempt.plusSeconds(LOCK_DURATION_SECONDS))) {
                // Se a janela expirou, reinicia a contagem
                return new AttemptInfo(1, now);
            }
            return new AttemptInfo(existing.attempts + 1, now);
        });
    }

    @Override
    public boolean isBlocked(String key) {
        String normalizedKey = key.toLowerCase();
        AttemptInfo info = attemptsCache.get(normalizedKey);
        if (info == null) {
            return false;
        }
        Instant now = Instant.now();
        if (now.isAfter(info.lastAttempt.plusSeconds(LOCK_DURATION_SECONDS))) {
            attemptsCache.remove(normalizedKey);
            return false;
        }
        return info.attempts >= MAX_ATTEMPTS;
    }

    private void cleanupExpiredEntries() {
        Instant now = Instant.now();
        attemptsCache.entrySet().removeIf(entry ->
                now.isAfter(entry.getValue().lastAttempt.plusSeconds(LOCK_DURATION_SECONDS))
        );
    }
}

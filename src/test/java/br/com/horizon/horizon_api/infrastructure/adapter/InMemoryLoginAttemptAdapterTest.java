package br.com.horizon.horizon_api.infrastructure.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLoginAttemptAdapterTest {

    private InMemoryLoginAttemptAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryLoginAttemptAdapter();
    }

    @Test
    void shouldNotBeBlockedInitially() {
        assertFalse(adapter.isBlocked("test@example.com"));
    }

    @Test
    void shouldBlockAfterFiveFailedAttempts() {
        String email = "attacker@example.com";

        for (int i = 0; i < 4; i++) {
            adapter.loginFailed(email);
            assertFalse(adapter.isBlocked(email), "Should not be blocked at attempt " + (i + 1));
        }

        adapter.loginFailed(email);
        assertTrue(adapter.isBlocked(email), "Should be blocked on 5th failed attempt");
    }

    @Test
    void shouldResetAttemptsOnSuccessfulLogin() {
        String email = "user@example.com";

        for (int i = 0; i < 4; i++) {
            adapter.loginFailed(email);
        }
        assertFalse(adapter.isBlocked(email));

        adapter.loginSucceeded(email);
        assertFalse(adapter.isBlocked(email));

        // After success, it should allow 4 more failures without blocking
        for (int i = 0; i < 4; i++) {
            adapter.loginFailed(email);
            assertFalse(adapter.isBlocked(email));
        }
    }
}

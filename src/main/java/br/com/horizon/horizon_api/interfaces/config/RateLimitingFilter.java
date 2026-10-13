package br.com.horizon.horizon_api.interfaces.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight in-memory Rate Limiting Filter for API protection.
 * Protects against DoS and abusive scraping without requiring external dependencies (like Redis).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int DEFAULT_MAX_REQUESTS_PER_MINUTE = 100;
    private static final int AUTH_MAX_REQUESTS_PER_MINUTE = 20;

    private static class RequestCounter {
        final long windowStartEpochSecond;
        final AtomicInteger count = new AtomicInteger(0);

        RequestCounter(long windowStartEpochSecond) {
            this.windowStartEpochSecond = windowStartEpochSecond;
        }
    }

    private final Map<String, RequestCounter> clientCounters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip rate limiting for preflight CORS requests and static documentation
        String uri = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        boolean isAuthEndpoint = uri.startsWith("/auth/login") || uri.startsWith("/auth/forgot-password");
        int maxAllowed = isAuthEndpoint ? AUTH_MAX_REQUESTS_PER_MINUTE : DEFAULT_MAX_REQUESTS_PER_MINUTE;

        long currentSecond = Instant.now().getEpochSecond();
        long currentWindow = currentSecond / 60; // 1-minute window
        String key = clientIp + ":" + (isAuthEndpoint ? "auth" : "gen") + ":" + currentWindow;

        cleanupOldCounters(currentWindow);

        RequestCounter counter = clientCounters.computeIfAbsent(key, k -> new RequestCounter(currentWindow));
        int currentCount = counter.count.incrementAndGet();

        if (currentCount > maxAllowed) {
            log.warn("Rate limit exceeded for IP {} on URI {} ({} requests in window)", clientIp, uri, currentCount);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\": \"Limite de requisições excedido. Tente novamente em instantes.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void cleanupOldCounters(long currentWindow) {
        // Remove counters from windows older than 2 minutes
        if (clientCounters.size() > 500) {
            clientCounters.entrySet().removeIf(entry -> entry.getValue().windowStartEpochSecond < currentWindow - 2);
        }
    }
}

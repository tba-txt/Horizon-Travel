package br.com.horizon.horizon_api.interfaces.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void shouldAllowRequestsUnderLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/destinations");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldBlockWhenExceedingRateLimitOnAuthEndpoint() throws ServletException, IOException {
        String clientIp = "10.0.0.99";

        // Auth endpoint limit is 20 requests per minute
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, filterChain);
            assertEquals(200, res.getStatus());
        }

        // 21st request should be rejected with 429
        MockHttpServletRequest excessReq = new MockHttpServletRequest("POST", "/auth/login");
        excessReq.setRemoteAddr(clientIp);
        MockHttpServletResponse excessRes = new MockHttpServletResponse();

        filter.doFilter(excessReq, excessRes, filterChain);

        assertEquals(429, excessRes.getStatus());
        assertEquals("60", excessRes.getHeader("Retry-After"));
    }

    @Test
    void shouldBypassOptionsAndSwagger() throws ServletException, IOException {
        MockHttpServletRequest optionsReq = new MockHttpServletRequest("OPTIONS", "/auth/login");
        MockHttpServletResponse optionsRes = new MockHttpServletResponse();
        filter.doFilter(optionsReq, optionsRes, filterChain);
        assertEquals(200, optionsRes.getStatus());

        MockHttpServletRequest swaggerReq = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse swaggerRes = new MockHttpServletResponse();
        filter.doFilter(swaggerReq, swaggerRes, filterChain);
        assertEquals(200, swaggerRes.getStatus());
    }
}

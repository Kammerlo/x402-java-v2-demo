package demo.facilitator;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Customization 3, plain Spring around it: /verify and /settle need an {@code X-Api-Key} header,
 * /supported stays public (resource servers read it at startup). The facilitator's routes are
 * ordinary Spring endpoints, so a filter or Spring Security protects them like any other.
 *
 * <p>The resource server sends the key from its own YAML: {@code x402.facilitators[].headers}.
 */
@Configuration
class ApiKeyFilter {

  @Bean
  FilterRegistrationBean<OncePerRequestFilter> apiKeyFilterRegistration(@Value("${demo.api-key}") String key) {
    byte[] expected = key.getBytes(StandardCharsets.UTF_8);
    OncePerRequestFilter filter = new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
          FilterChain chain) throws ServletException, IOException {
        String given = request.getHeader("X-Api-Key");
        // Constant-time comparison, so the key can't be guessed byte by byte from timings.
        if (given == null
            || !MessageDigest.isEqual(expected, given.getBytes(StandardCharsets.UTF_8))) {
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "X-Api-Key required");
          return;
        }
        chain.doFilter(request, response);
      }
    };
    FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(filter);
    registration.addUrlPatterns("/x402/verify", "/x402/settle");
    return registration;
  }
}

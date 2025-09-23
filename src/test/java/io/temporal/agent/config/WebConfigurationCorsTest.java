package io.temporal.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WebConfigurationCorsTest {

    @Test
    void defaultConfigurationIncludesZeroHostOriginPattern() {
        CorsProperties properties = new CorsProperties();
        WebConfiguration configuration = new WebConfiguration(properties);
        RecordingCorsRegistry registry = new RecordingCorsRegistry();

        configuration.addCorsMappings(registry);

        CorsConfiguration corsConfiguration = registry.getConfigurationForPattern("/api/**");
        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOriginPatterns())
                .contains("http://0.0.0.0:*", "https://0.0.0.0:*");
    }

    @Test
    void defaultConfigurationEchoesRequestOriginWhenPatternMatches() {
        CorsProperties properties = new CorsProperties();
        WebConfiguration configuration = new WebConfiguration(properties);
        RecordingCorsRegistry registry = new RecordingCorsRegistry();

        configuration.addCorsMappings(registry);

        CorsConfiguration corsConfiguration = registry.getConfigurationForPattern("/api/**");
        assertThat(corsConfiguration).isNotNull();

        String resolvedOrigin = corsConfiguration.checkOrigin("http://localhost:5173");
        assertThat(resolvedOrigin).isEqualTo("http://localhost:5173");
    }

    @Test
    void corsFilterRespondsToPreflightRequest() throws ServletException, IOException {
        CorsProperties properties = new CorsProperties();
        WebConfiguration configuration = new WebConfiguration(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/agent/workflow/history");
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:5173");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

        MockHttpServletResponse response = new MockHttpServletResponse();
        configuration.corsFilter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://localhost:5173");
    }

    private static class RecordingCorsRegistry extends CorsRegistry {

        CorsConfiguration getConfigurationForPattern(String pattern) {
            Map<String, CorsConfiguration> configurations = super.getCorsConfigurations();
            return configurations.get(pattern);
        }
    }
}

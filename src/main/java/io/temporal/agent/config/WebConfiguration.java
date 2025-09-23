package io.temporal.agent.config;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfiguration implements WebMvcConfigurer {

    private static final String[] DEFAULT_ALLOWED_METHODS = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"};
    private static final String[] DEFAULT_ALLOWED_HEADERS = {"*"};
    private static final String[] DEFAULT_EXPOSED_HEADERS = {"*"};
    private static final String DEFAULT_PATH_PATTERN = "/api/**";
    private static final String DEFAULT_ORIGIN_FALLBACK = "*";

    private final CorsProperties corsProperties;

    public WebConfiguration(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsConfiguration configuration = buildCorsConfiguration();
        for (String pathPattern : resolvePathPatterns()) {
            CorsRegistration registration = registry.addMapping(pathPattern);
            applyRegistration(configuration, registration);
        }
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        for (String pathPattern : resolvePathPatterns()) {
            source.registerCorsConfiguration(pathPattern, buildCorsConfiguration());
        }
        return new CorsFilter(source);
    }

    CorsConfiguration buildCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        applyOriginConfiguration(configuration);
        configuration.setAllowedMethods(resolveValues(corsProperties.getAllowedMethods(), DEFAULT_ALLOWED_METHODS));
        configuration.setAllowedHeaders(resolveValues(corsProperties.getAllowedHeaders(), DEFAULT_ALLOWED_HEADERS));
        configuration.setExposedHeaders(resolveValues(corsProperties.getExposedHeaders(), DEFAULT_EXPOSED_HEADERS));
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());
        return configuration;
    }

    private void applyRegistration(CorsConfiguration configuration, CorsRegistration registration) {
        List<String> allowedOrigins = configuration.getAllowedOrigins();
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            registration.allowedOrigins(allowedOrigins.toArray(new String[0]));
        } else {
            List<String> allowedOriginPatterns = configuration.getAllowedOriginPatterns();
            if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
                registration.allowedOriginPatterns(DEFAULT_ORIGIN_FALLBACK);
            } else {
                registration.allowedOriginPatterns(allowedOriginPatterns.toArray(new String[0]));
            }
        }

        registration.allowedMethods(configuration.getAllowedMethods().toArray(new String[0]))
                .allowedHeaders(configuration.getAllowedHeaders().toArray(new String[0]))
                .exposedHeaders(configuration.getExposedHeaders().toArray(new String[0]))
                .allowCredentials(Boolean.TRUE.equals(configuration.getAllowCredentials()));

        Long maxAge = configuration.getMaxAge();
        if (maxAge != null) {
            registration.maxAge(maxAge);
        }
    }

    private void applyOriginConfiguration(CorsConfiguration configuration) {
        List<String> allowedOrigins = sanitize(corsProperties.getAllowedOrigins());
        if (!allowedOrigins.isEmpty()) {
            configuration.setAllowedOrigins(allowedOrigins);
            return;
        }

        List<String> allowedOriginPatterns = sanitize(corsProperties.getAllowedOriginPatterns());
        if (allowedOriginPatterns.isEmpty()) {
            configuration.setAllowedOriginPatterns(List.of(DEFAULT_ORIGIN_FALLBACK));
        } else {
            configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        }
    }

    private List<String> resolvePathPatterns() {
        List<String> pathPatterns = sanitize(corsProperties.getPathPatterns());
        if (pathPatterns.isEmpty()) {
            return List.of(DEFAULT_PATH_PATTERN);
        }
        return pathPatterns;
    }

    private static List<String> resolveValues(List<String> values, String[] defaults) {
        List<String> sanitized = sanitize(values);
        if (sanitized.isEmpty()) {
            return List.of(defaults);
        }
        return sanitized;
    }

    private static List<String> sanitize(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toList());
    }
}

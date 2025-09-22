package io.temporal.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "pandadoc")
public record PandadocProperties(
        String apiKey,
        @DefaultValue("https://api.pandadoc.com/public/v1") String baseUrl,
        @DefaultValue("30") int statusCheckIntervalSeconds) {

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}

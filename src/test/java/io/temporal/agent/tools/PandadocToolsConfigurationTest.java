package io.temporal.agent.tools;

import static org.assertj.core.api.Assertions.assertThat;

import io.temporal.agent.config.PandadocProperties;
import io.temporal.agent.tools.pandadoc.DefaultPandadocClient;
import io.temporal.agent.tools.pandadoc.PandadocClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class PandadocToolsConfigurationTest {

    private ToolRegistry registry;

    @BeforeEach
    void setup() {
        registry = new ToolRegistry();
        PandadocProperties properties = new PandadocProperties(null, "https://api.pandadoc.com/public/v1", 30);
        PandadocClient client = new DefaultPandadocClient(RestClient.builder(), properties);
        new PandadocToolsConfiguration(registry, client);
    }

    @Test
    void listTemplatesReturnsStubbedTemplates() {
        Map<String, Object> result = registry.execute("ListPandadocTemplates", Map.of());
        assertThat(result).containsKey("templates");
        List<?> templates = (List<?>) result.get("templates");
        assertThat(templates).isNotEmpty();
    }

    @Test
    void createSendAndFetchDocumentLifecycle() {
        Map<String, Object> createArgs = Map.of(
                "template_name", "Mutual NDA",
                "document_name", "NDA for Acme",
                "signers", List.of(Map.of("email", "john@example.com", "role", "signer", "name", "John Doe")),
                "tokens", Map.of("company_name", "Acme"));
        Map<String, Object> creation = registry.execute("CreatePandadocDocument", createArgs);
        assertThat(creation).containsKeys("document_id", "status");
        String documentId = (String) creation.get("document_id");

        Map<String, Object> sendResult = registry.execute("SendPandadocDocument", Map.of("document_id", documentId));
        assertThat(sendResult).containsEntry("document_id", documentId);

        Map<String, Object> status = registry.execute("GetPandadocDocumentStatus", Map.of("document_id", documentId));
        assertThat(status).containsEntry("document_id", documentId);
        assertThat(status.get("status")).isNotNull();
    }
}

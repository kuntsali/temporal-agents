package io.temporal.agent.tools.pandadoc;

import com.fasterxml.jackson.databind.JsonNode;
import io.temporal.agent.config.PandadocProperties;
import io.temporal.agent.tools.pandadoc.PandadocClient.CreateDocumentRequest;
import io.temporal.agent.tools.pandadoc.PandadocClient.Document;
import io.temporal.agent.tools.pandadoc.PandadocClient.Recipient;
import io.temporal.agent.tools.pandadoc.PandadocClient.TemplateSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DefaultPandadocClient implements PandadocClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPandadocClient.class);
    private static final List<TemplateSummary> STUB_TEMPLATES = List.of(
            new TemplateSummary("tpl-nda", "Mutual NDA", "2024-02-01T00:00:00Z"),
            new TemplateSummary("tpl-sow", "Statement of Work", "2024-03-15T00:00:00Z"),
            new TemplateSummary("tpl-proposal", "Sales Proposal", "2024-04-21T00:00:00Z"));

    private final RestClient restClient;
    private final PandadocProperties properties;
    private final ConcurrentMap<String, Document> stubDocuments = new ConcurrentHashMap<>();

    public DefaultPandadocClient(RestClient.Builder builder, PandadocProperties properties) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.baseUrl() != null ? properties.baseUrl() : "https://api.pandadoc.com/public/v1")
                .build();
    }

    @Override
    public List<TemplateSummary> listTemplates(String search) {
        if (!properties.isConfigured()) {
            return filterStubTemplates(search);
        }
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/templates");
                        if (search != null && !search.isBlank()) {
                            uriBuilder.queryParam("search", search);
                        }
                        return uriBuilder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, "API-Key " + properties.apiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            return parseTemplates(response);
        } catch (RestClientException ex) {
            LOGGER.error("Failed to list PandaDoc templates: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public Optional<TemplateSummary> findTemplateByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        List<TemplateSummary> candidates = listTemplates(name);
        return candidates.stream()
                .filter(summary -> summary.name() != null && summary.name().trim().equalsIgnoreCase(name.trim()))
                .findFirst()
                .or(() -> candidates.stream()
                        .filter(summary -> summary.name() != null
                                && summary.name().toLowerCase(Locale.ROOT).contains(normalized))
                        .findFirst());
    }

    @Override
    public Document createDocument(CreateDocumentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (!properties.isConfigured()) {
            Document stub = new Document(
                    UUID.randomUUID().toString(),
                    "document.draft",
                    stubUrl(UUID.randomUUID().toString()),
                    request.documentName(),
                    Instant.now().toString());
            stubDocuments.put(stub.id(), stub);
            return stub;
        }
        try {
            Map<String, Object> body = buildCreateBody(request);
            JsonNode response = restClient.post()
                    .uri("/documents")
                    .header(HttpHeaders.AUTHORIZATION, "API-Key " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            Document document = parseDocument(response, request.documentName());
            if (document.id() != null) {
                stubDocuments.put(document.id(), document);
            }
            return document;
        } catch (RestClientException ex) {
            LOGGER.error("Failed to create PandaDoc document: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public Document sendDocument(String documentId, String emailSubject, String emailMessage) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId is required");
        }
        if (!properties.isConfigured()) {
            Document updated = stubDocuments.compute(documentId, (id, existing) -> {
                Document base = existing != null
                        ? existing
                        : new Document(id, "document.draft", stubUrl(id), null, Instant.now().toString());
                return base.withStatus("document.completed");
            });
            return updated != null
                    ? updated
                    : new Document(documentId, "document.completed", stubUrl(documentId), null, Instant.now().toString());
        }
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        if (emailSubject != null && !emailSubject.isBlank()) {
            body.put("subject", emailSubject);
        }
        if (emailMessage != null && !emailMessage.isBlank()) {
            body.put("message", emailMessage);
        }
        body.put("silent", false);
        try {
            JsonNode response = restClient.post()
                    .uri("/documents/{id}/send", documentId)
                    .header(HttpHeaders.AUTHORIZATION, "API-Key " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            Document document = parseDocument(response, null);
            if (document.id() != null) {
                stubDocuments.put(document.id(), document);
            }
            return document;
        } catch (RestClientException ex) {
            LOGGER.error("Failed to send PandaDoc document {}: {}", documentId, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public Document getDocument(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId is required");
        }
        if (!properties.isConfigured()) {
            return stubDocuments.computeIfAbsent(documentId, id -> new Document(
                    id,
                    "document.completed",
                    stubUrl(id),
                    null,
                    Instant.now().toString()));
        }
        try {
            JsonNode response = restClient.get()
                    .uri("/documents/{id}", documentId)
                    .header(HttpHeaders.AUTHORIZATION, "API-Key " + properties.apiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            Document document = parseDocument(response, null);
            if (document.id() != null) {
                stubDocuments.put(document.id(), document);
            }
            return document;
        } catch (RestClientException ex) {
            LOGGER.error("Failed to fetch PandaDoc document {}: {}", documentId, ex.getMessage(), ex);
            throw ex;
        }
    }

    private List<TemplateSummary> filterStubTemplates(String search) {
        if (search == null || search.isBlank()) {
            return STUB_TEMPLATES;
        }
        String normalized = search.trim().toLowerCase(Locale.ROOT);
        return STUB_TEMPLATES.stream()
                .filter(template -> template.name() != null && template.name().toLowerCase(Locale.ROOT).contains(normalized))
                .collect(Collectors.toList());
    }

    private List<TemplateSummary> parseTemplates(JsonNode node) {
        if (node == null) {
            return Collections.emptyList();
        }
        JsonNode collection = resolveCollectionNode(node);
        if (collection == null || !collection.isArray()) {
            return Collections.emptyList();
        }
        List<TemplateSummary> templates = new ArrayList<>();
        for (JsonNode item : collection) {
            String id = text(item, "id");
            if (id == null) {
                id = text(item, "uuid");
            }
            String name = text(item, "name");
            String updated = text(item, "updated_at");
            if (updated == null) {
                updated = text(item, "date_updated");
            }
            templates.add(new TemplateSummary(id, name, updated));
        }
        return templates;
    }

    private JsonNode resolveCollectionNode(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isArray()) {
            return node;
        }
        if (node.has("results")) {
            return node.get("results");
        }
        if (node.has("data")) {
            return node.get("data");
        }
        if (node.has("templates")) {
            return node.get("templates");
        }
        return node;
    }

    private Map<String, Object> buildCreateBody(CreateDocumentRequest request) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("name", request.documentName());
        body.put("template_uuid", request.templateId());
        body.put("send", false);
        List<Map<String, Object>> recipients = request.recipients().stream()
                .map(Recipient::toRequest)
                .collect(Collectors.toCollection(ArrayList::new));
        body.put("recipients", recipients);
        if (!request.tokens().isEmpty()) {
            List<Map<String, Object>> tokens = request.tokens().entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> token = new java.util.LinkedHashMap<>();
                        token.put("name", entry.getKey());
                        token.put("value", entry.getValue());
                        return token;
                    })
                    .collect(Collectors.toCollection(ArrayList::new));
            body.put("tokens", tokens);
        }
        if (!request.metadata().isEmpty()) {
            body.put("metadata", request.metadata());
        }
        if (request.emailSubject() != null && !request.emailSubject().isBlank()) {
            body.put("email_subject", request.emailSubject());
        }
        if (request.emailMessage() != null && !request.emailMessage().isBlank()) {
            body.put("email_message", request.emailMessage());
        }
        return body;
    }

    private Document parseDocument(JsonNode node, String fallbackName) {
        if (node == null) {
            return new Document(null, null, null, fallbackName, Instant.now().toString());
        }
        String id = text(node, "id");
        if (id == null) {
            id = text(node, "uuid");
        }
        String status = text(node, "status");
        String name = text(node, "name");
        if (name == null) {
            name = fallbackName;
        }
        String url = null;
        if (node.has("links")) {
            JsonNode links = node.get("links");
            if (links.has("document")) {
                url = text(links, "document");
            } else if (links.has("session") && links.get("session").isArray() && links.get("session").size() > 0) {
                url = text(links.get("session").get(0), "link");
            }
        }
        if (url == null) {
            url = text(node, "document_url");
        }
        if (url == null && id != null) {
            url = stubUrl(id);
        }
        String updated = text(node, "date_modified");
        if (updated == null) {
            updated = text(node, "date_created");
        }
        if (updated == null) {
            updated = Instant.now().toString();
        }
        return new Document(id, status, url, name, updated);
    }

    private String text(JsonNode node, String field) {
        if (node == null || field == null) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText(null);
    }

    private String stubUrl(String id) {
        return "https://stub.pandadoc.local/doc/" + Objects.toString(id, UUID.randomUUID().toString());
    }
}

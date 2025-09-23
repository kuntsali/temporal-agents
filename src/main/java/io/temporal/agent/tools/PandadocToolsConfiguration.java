package io.temporal.agent.tools;

import io.temporal.agent.model.tools.ToolArgument;
import io.temporal.agent.model.tools.ToolDefinition;
import io.temporal.agent.tools.pandadoc.PandadocClient;
import io.temporal.agent.tools.pandadoc.PandadocClient.CreateDocumentRequest;
import io.temporal.agent.tools.pandadoc.PandadocClient.Document;
import io.temporal.agent.tools.pandadoc.PandadocClient.Recipient;
import io.temporal.agent.tools.pandadoc.PandadocClient.TemplateSummary;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PandadocToolsConfiguration {

    private final PandadocClient pandadocClient;

    public PandadocToolsConfiguration(ToolRegistry toolRegistry, PandadocClient pandadocClient) {
        this.pandadocClient = pandadocClient;
        toolRegistry.register(listTemplatesDefinition(), this::listTemplates);
        toolRegistry.register(createDocumentDefinition(), this::createDocument);
        toolRegistry.register(sendDocumentDefinition(), this::sendDocument);
        toolRegistry.register(getDocumentDefinition(), this::getDocumentStatus);
    }

    private ToolDefinition listTemplatesDefinition() {
        List<ToolArgument> args = List.of(
                new ToolArgument("search", "string", "Optional search string to filter templates by name"));
        return new ToolDefinition("ListPandadocTemplates", "List available PandaDoc templates", args);
    }

    private ToolDefinition createDocumentDefinition() {
        List<ToolArgument> args = List.of(
                new ToolArgument("template_id", "string", "Exact PandaDoc template UUID to use"),
                new ToolArgument("template_name", "string", "Template name if the UUID is unknown"),
                new ToolArgument("document_name", "string", "Name for the generated document"),
                new ToolArgument("signers", "array", "Array of signer objects with email, role, and optional name"),
                new ToolArgument("tokens", "object", "Map of template token values keyed by token name"),
                new ToolArgument("email_subject", "string", "Optional subject used when emailing the document"),
                new ToolArgument("email_message", "string", "Optional body used when emailing the document"),
                new ToolArgument("metadata", "object", "Optional metadata map stored with the document"),
                new ToolArgument("company", "string", "Optional company name metadata"));
        return new ToolDefinition("CreatePandadocDocument", "Create a PandaDoc draft document from a template", args);
    }

    private ToolDefinition sendDocumentDefinition() {
        List<ToolArgument> args = List.of(
                new ToolArgument("document_id", "string", "Identifier of the PandaDoc document to send"),
                new ToolArgument("email_subject", "string", "Optional subject override"),
                new ToolArgument("email_message", "string", "Optional email body override"));
        return new ToolDefinition("SendPandadocDocument", "Send a PandaDoc document for e-signature", args);
    }

    private ToolDefinition getDocumentDefinition() {
        List<ToolArgument> args = List.of(
                new ToolArgument("document_id", "string", "Identifier of the PandaDoc document to inspect"));
        return new ToolDefinition("GetPandadocDocumentStatus", "Fetch the latest PandaDoc document status", args);
    }

    private Map<String, Object> listTemplates(Map<String, Object> args) {
        String search = stringArg(args.get("search"));
        List<TemplateSummary> templates = pandadocClient.listTemplates(search);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("templates", templates.stream().map(TemplateSummary::toMap).collect(Collectors.toList()));
        response.put("total", templates.size());
        return response;
    }

    private Map<String, Object> createDocument(Map<String, Object> args) {
        String templateId = stringArg(args.get("template_id"));
        String templateName = stringArg(args.get("template_name"));
        if (isBlank(templateId)) {
            templateId = resolveTemplateId(templateName);
        }
        if (isBlank(templateId)) {
            throw new IllegalArgumentException("template_id or template_name is required");
        }
        String documentName = stringArg(args.get("document_name"));
        if (isBlank(documentName)) {
            throw new IllegalArgumentException("document_name is required");
        }
        List<Recipient> recipients = extractRecipients(args.get("signers"));
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("At least one signer is required");
        }
        Map<String, String> tokens = extractTokens(args.get("tokens"));
        Map<String, Object> metadata = extractMetadata(args.get("metadata"));
        String company = stringArg(args.get("company"));
        if (!isBlank(company)) {
            metadata.putIfAbsent("company", company);
        }
        String emailSubject = stringArg(args.get("email_subject"));
        String emailMessage = stringArg(args.get("email_message"));

        CreateDocumentRequest request = new CreateDocumentRequest(templateId, documentName, recipients, tokens, metadata, emailSubject, emailMessage);
        Document document = pandadocClient.createDocument(request);
        return document.toMap();
    }

    private Map<String, Object> sendDocument(Map<String, Object> args) {
        String documentId = requireString(args.get("document_id"), "document_id is required");
        String emailSubject = stringArg(args.get("email_subject"));
        String emailMessage = stringArg(args.get("email_message"));
        Document document = pandadocClient.sendDocument(documentId, emailSubject, emailMessage);
        return document.toMap();
    }

    private Map<String, Object> getDocumentStatus(Map<String, Object> args) {
        String documentId = requireString(args.get("document_id"), "document_id is required");
        Document document = pandadocClient.getDocument(documentId);
        return document.toMap();
    }

    private String resolveTemplateId(String templateName) {
        if (isBlank(templateName)) {
            return null;
        }
        Optional<TemplateSummary> summary = pandadocClient.findTemplateByName(templateName);
        return summary.map(TemplateSummary::id).orElse(null);
    }

    private List<Recipient> extractRecipients(Object value) {
        List<Recipient> recipients = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object element : list) {
                parseRecipient(element).ifPresent(recipients::add);
            }
        } else {
            parseRecipient(value).ifPresent(recipients::add);
        }
        return recipients;
    }

    private Optional<Recipient> parseRecipient(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Optional.empty();
        }
        String email = stringArg(map.get("email"));
        String role = stringArg(map.get("role"));
        if (isBlank(email) || isBlank(role)) {
            throw new IllegalArgumentException("signers require email and role");
        }
        String firstName = stringArg(map.get("first_name"));
        String lastName = stringArg(map.get("last_name"));
        if (isBlank(firstName) && isBlank(lastName)) {
            String name = stringArg(map.get("name"));
            if (!isBlank(name)) {
                String[] parts = name.trim().split(" ", 2);
                firstName = parts[0];
                if (parts.length > 1) {
                    lastName = parts[1];
                }
            }
        }
        return Optional.of(new Recipient(email, role, firstName, lastName));
    }

    private Map<String, String> extractTokens(Object value) {
        Map<String, String> tokens = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = Objects.toString(entry.getKey(), null);
                if (key != null) {
                    tokens.put(key, stringArg(entry.getValue()));
                }
            }
        } else if (value instanceof List<?> list) {
            for (Object element : list) {
                if (element instanceof Map<?, ?> entryMap) {
                    String name = stringArg(entryMap.get("name"));
                    String tokenValue = stringArg(entryMap.get("value"));
                    if (!isBlank(name)) {
                        tokens.put(name, tokenValue);
                    }
                }
            }
        }
        return tokens;
    }

    private Map<String, Object> extractMetadata(Object value) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = Objects.toString(entry.getKey(), null);
                if (key != null) {
                    metadata.put(key, entry.getValue());
                }
            }
        }
        return metadata;
    }

    private String requireString(Object value, String message) {
        String result = stringArg(value);
        if (isBlank(result)) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    private String stringArg(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str.trim();
        }
        return Objects.toString(value, null);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

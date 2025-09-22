package io.temporal.agent.tools.pandadoc;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PandadocClient {

    List<TemplateSummary> listTemplates(String search);

    Optional<TemplateSummary> findTemplateByName(String name);

    Document createDocument(CreateDocumentRequest request);

    Document sendDocument(String documentId, String emailSubject, String emailMessage);

    Document getDocument(String documentId);

    record TemplateSummary(String id, String name, String updatedAt) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("name", name);
            map.put("updated_at", updatedAt);
            return map;
        }
    }

    record Recipient(String email, String role, String firstName, String lastName) {
        public Map<String, Object> toRequest() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("email", email);
            map.put("role", role);
            map.put("first_name", firstName != null && !firstName.isBlank() ? firstName : "Signer");
            map.put("last_name", lastName != null ? lastName : "");
            return map;
        }
    }

    record CreateDocumentRequest(
            String templateId,
            String documentName,
            List<Recipient> recipients,
            Map<String, String> tokens,
            Map<String, Object> metadata,
            String emailSubject,
            String emailMessage) {
    }

    record Document(String id, String status, String documentUrl, String name, String updatedAt) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("document_id", id);
            map.put("status", status);
            map.put("document_url", documentUrl);
            if (name != null) {
                map.put("name", name);
            }
            if (updatedAt != null) {
                map.put("updated_at", updatedAt);
            }
            map.put("completed", status != null && status.contains("completed"));
            return map;
        }

        public Document withStatus(String newStatus) {
            return new Document(id, newStatus, documentUrl, name,
                    updatedAt != null ? updatedAt : Instant.now().toString());
        }
    }
}

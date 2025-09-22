package io.temporal.agent.workflow.documents;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface DocumentActivities {

    PandadocDocument createDocument(DocumentCreationInput input);

    PandadocDocument sendDocument(DocumentSendInput input);

    PandadocDocument getDocument(DocumentStatusInput input);

    record DocumentCreationInput(
            String templateId,
            String documentName,
            List<RecipientInput> recipients,
            Map<String, String> tokens,
            Map<String, Object> metadata,
            String emailSubject,
            String emailMessage) {

        public String getTemplateId() {
            return templateId;
        }

        public String getDocumentName() {
            return documentName;
        }

        public List<RecipientInput> getRecipients() {
            return recipients != null ? recipients : Collections.emptyList();
        }

        public Map<String, String> getTokens() {
            return tokens != null ? tokens : Collections.emptyMap();
        }

        public Map<String, Object> getMetadata() {
            return metadata != null ? metadata : Collections.emptyMap();
        }

        public String getEmailSubject() {
            return emailSubject;
        }

        public String getEmailMessage() {
            return emailMessage;
        }
    }

    record RecipientInput(String email, String role, String firstName, String lastName) {

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }
    }

    record DocumentSendInput(String documentId, String emailSubject, String emailMessage) {

        public String getDocumentId() {
            return documentId;
        }

        public String getEmailSubject() {
            return emailSubject;
        }

        public String getEmailMessage() {
            return emailMessage;
        }
    }

    record DocumentStatusInput(String documentId) {

        public String getDocumentId() {
            return documentId;
        }
    }

    record PandadocDocument(
            String documentId,
            String status,
            String documentUrl,
            String name,
            String updatedAt,
            boolean completed) {

        public String getDocumentId() {
            return documentId;
        }

        public String getStatus() {
            return status;
        }

        public String getDocumentUrl() {
            return documentUrl;
        }

        public String getName() {
            return name;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
}

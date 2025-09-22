package io.temporal.agent.workflow.documents;

import io.temporal.agent.tools.pandadoc.PandadocClient;
import io.temporal.agent.tools.pandadoc.PandadocClient.CreateDocumentRequest;
import io.temporal.agent.tools.pandadoc.PandadocClient.Document;
import io.temporal.agent.tools.pandadoc.PandadocClient.Recipient;
import io.temporal.agent.workflow.documents.DocumentActivities.DocumentCreationInput;
import io.temporal.agent.workflow.documents.DocumentActivities.DocumentSendInput;
import io.temporal.agent.workflow.documents.DocumentActivities.DocumentStatusInput;
import io.temporal.agent.workflow.documents.DocumentActivities.PandadocDocument;
import io.temporal.agent.workflow.documents.DocumentActivities.RecipientInput;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DocumentActivitiesImpl implements DocumentActivities {

    private final PandadocClient pandadocClient;

    public DocumentActivitiesImpl(PandadocClient pandadocClient) {
        this.pandadocClient = pandadocClient;
    }

    @Override
    public PandadocDocument createDocument(DocumentCreationInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input is required");
        }
        CreateDocumentRequest request = new CreateDocumentRequest(
                input.getTemplateId(),
                input.getDocumentName(),
                toRecipients(input.getRecipients()),
                input.getTokens(),
                input.getMetadata(),
                input.getEmailSubject(),
                input.getEmailMessage());
        Document document = pandadocClient.createDocument(request);
        return toPandadocDocument(document);
    }

    @Override
    public PandadocDocument sendDocument(DocumentSendInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input is required");
        }
        Document document = pandadocClient.sendDocument(
                input.getDocumentId(),
                input.getEmailSubject(),
                input.getEmailMessage());
        return toPandadocDocument(document);
    }

    @Override
    public PandadocDocument getDocument(DocumentStatusInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input is required");
        }
        Document document = pandadocClient.getDocument(input.getDocumentId());
        return toPandadocDocument(document);
    }

    private List<Recipient> toRecipients(List<RecipientInput> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return List.of();
        }
        return recipients.stream()
                .filter(Objects::nonNull)
                .map(recipient -> new Recipient(
                        recipient.getEmail(),
                        recipient.getRole(),
                        recipient.getFirstName(),
                        recipient.getLastName()))
                .toList();
    }

    private PandadocDocument toPandadocDocument(Document document) {
        if (document == null) {
            return null;
        }
        String status = document.status();
        boolean completed = status != null && status.toLowerCase().contains("completed");
        return new PandadocDocument(
                document.id(),
                status,
                document.documentUrl(),
                document.name(),
                document.updatedAt(),
                completed);
    }
}

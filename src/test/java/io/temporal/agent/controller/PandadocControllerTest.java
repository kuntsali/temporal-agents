package io.temporal.agent.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.temporal.agent.tools.pandadoc.PandadocClient;
import io.temporal.agent.tools.pandadoc.PandadocClient.TemplateSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PandadocControllerTest {

    private final PandadocClient pandadocClient = Mockito.mock(PandadocClient.class);
    private final PandadocController controller = new PandadocController(pandadocClient);

    @Test
    void listTemplatesReturnsData() {
        List<TemplateSummary> templates = List.of(new TemplateSummary("tpl-123", "NDA", "2024-01-01T00:00:00Z"));
        when(pandadocClient.listTemplates(null)).thenReturn(templates);

        PandadocController.TemplateListResponse response = controller.listTemplates(null);

        assertEquals(1, response.total());
        assertEquals(templates, response.templates());
    }

    @Test
    void listTemplatesTrimsSearchInput() {
        when(pandadocClient.listTemplates("nda")).thenReturn(List.of());

        controller.listTemplates(" nda ");

        verify(pandadocClient).listTemplates("nda");
    }
}

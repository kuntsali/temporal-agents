package io.temporal.agent.controller;

import io.temporal.agent.tools.pandadoc.PandadocClient;
import io.temporal.agent.tools.pandadoc.PandadocClient.TemplateSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pandadoc")
public class PandadocController {

    private final PandadocClient pandadocClient;

    public PandadocController(PandadocClient pandadocClient) {
        this.pandadocClient = pandadocClient;
    }

    @GetMapping("/templates")
    public TemplateListResponse listTemplates(@RequestParam(value = "search", required = false) String search) {
        String normalizedSearch = search != null && !search.isBlank() ? search.trim() : null;
        List<TemplateSummary> templates = pandadocClient.listTemplates(normalizedSearch);
        return new TemplateListResponse(templates, templates.size());
    }

    public record TemplateListResponse(List<TemplateSummary> templates, int total) {}
}

package io.temporal.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiDeserializationTest {

    @Test
    void chatCompletionMessageIgnoresAnnotations() throws Exception {
        TemporalConfiguration configuration = new TemporalConfiguration();
        ObjectMapper objectMapper = configuration.objectMapper(new Jackson2ObjectMapperBuilder());

        String response = """
                {
                  \"id\": \"chatcmpl-123\",
                  \"object\": \"chat.completion\",
                  \"created\": 1,
                  \"model\": \"gpt-4o-mini\",
                  \"choices\": [
                    {
                      \"index\": 0,
                      \"message\": {
                        \"role\": \"assistant\",
                        \"content\": \"Hello!\",
                        \"annotations\": [
                          {
                            \"type\": \"file_path\",
                            \"text\": \"ignored\"
                          }
                        ]
                      },
                      \"logprobs\": null,
                      \"finish_reason\": \"stop\"
                    }
                  ],
                  \"usage\": {
                    \"prompt_tokens\": 1,
                    \"completion_tokens\": 1,
                    \"total_tokens\": 2
                  }
                }
                """;

        OpenAiApi.ChatCompletion completion = objectMapper.readValue(response, OpenAiApi.ChatCompletion.class);

        assertThat(completion).isNotNull();
        assertThat(completion.choices()).hasSize(1);
        assertThat(completion.choices().getFirst().message().content()).isEqualTo("Hello!");
    }
}

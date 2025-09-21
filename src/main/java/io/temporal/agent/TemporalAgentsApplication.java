package io.temporal.agent;

import io.temporal.agent.config.TemporalProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({TemporalProperties.class})
public class TemporalAgentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemporalAgentsApplication.class, args);
    }
}

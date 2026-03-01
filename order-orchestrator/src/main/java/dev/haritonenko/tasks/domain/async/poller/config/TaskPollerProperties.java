package dev.haritonenko.tasks.domain.async.poller.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "task-execution.poller")
public class TaskPollerProperties {
    private int batchSize;
    private Duration retryDelay;
}

package dev.haritonenko.tasks.domain.async.dispatcher.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "task-execution.dispatcher")
public class TaskDispatcherProperties {
    private Duration retryDelay;
    private int threadPoolSize;
    private int queueCapacity;
    private int maxAttempts;
}
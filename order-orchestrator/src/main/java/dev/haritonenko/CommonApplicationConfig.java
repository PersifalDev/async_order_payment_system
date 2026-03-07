package dev.haritonenko;

import dev.haritonenko.tasks.domain.async.dispatcher.config.TaskDispatcherProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class CommonApplicationConfig {


    @Bean(destroyMethod = "shutdown")
    public ExecutorService taskDispatcherThreadPool(
            TaskDispatcherProperties properties
    ) {
        int poolSize = properties.getThreadPoolSize();
        int queueCapacity = properties.getQueueCapacity();

        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService externalHttpThreadPool(
            TaskDispatcherProperties properties
    ) {
        return new ThreadPoolExecutor(
                properties.getThreadPoolSize(),
                properties.getThreadPoolSize(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>( properties.getQueueCapacity()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
package com.luna.deepluna.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous task execution.
 * Provides thread pools for different types of agent operations.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("agentExecutor")
    public Executor agentExecutor() {
        return buildExecutor("agent-exec-", 4, 8, 200);
    }

    @Bean("retrievalExecutor")
    public Executor retrievalExecutor() {
        return buildExecutor("retrieval-exec-", 2, 4, 100);
    }

    @Bean("chatHistoryExecutor")
    public Executor chatHistoryExecutor() {
        return buildExecutor("chat-history-exec-", 2, 4, 500);
    }

    @Bean("persistenceExecutor")
    public Executor persistenceExecutor() {
        return buildExecutor("persistence-exec-", 2, 4, 500);
    }

    private Executor buildExecutor(String threadNamePrefix, int corePoolSize, int maxPoolSize, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

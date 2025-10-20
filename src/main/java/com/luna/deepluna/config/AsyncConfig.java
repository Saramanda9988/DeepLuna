package com.luna.deepluna.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for asynchronous task execution.
 * Provides thread pools for different types of agent operations.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * Main executor for agent tasks.
     * Used by the Supervisor for orchestrating agent execution.
     */
    @Bean("agentExecutor")
    public ExecutorService agentExecutor() {
        return Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "agent-pool-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Specialized executor for retrieval operations.
     * Used for parallel information gathering from multiple sources.
     */
    @Bean("retrievalExecutor")
    public ExecutorService retrievalExecutor() {
        return Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "retrieval-pool-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }
}
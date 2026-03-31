package com.aigreentick.services.broadcast.config;

import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling; 

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@Slf4j
public class ExecutorConfig {

    @Value("${campaign.max-concurrent-users:100}")
    private int maxConcurrentUsers;

    @Value("${campaign.executor.core-pool-size:500}")
    private int executorCorePoolSize;

    @Value("${campaign.executor.max-pool-size:2000}")
    private int executorMaxPoolSize;

    @Value("${campaign.executor.queue-capacity:10000}")
    private int executorQueueCapacity;

    @Bean(name = "broadcastExecutor", destroyMethod = "shutdown")
    public ExecutorService broadcastExecutor() {
        int poolSize = Math.max(100, maxConcurrentUsers);
        log.info("Initializing broadcastExecutor with pool size: {}", poolSize);
        return Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r);
            t.setName("broadcast-" + t.getId());
            t.setDaemon(false);
            return t;
        });
    }

    @Bean(name = "whatsappExecutor", destroyMethod = "shutdown")
    public ExecutorService whatsappExecutor() {
        log.info("Initializing whatsappExecutor: core={} max={} queue={}",
                executorCorePoolSize, executorMaxPoolSize, executorQueueCapacity);

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                executorCorePoolSize,
                executorMaxPoolSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(executorQueueCapacity),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("whatsapp-" + t.getId());
                    t.setDaemon(false);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                        log.warn("WhatsApp executor queue full! Queue: {}/{}, Active: {}/{}",
                                e.getQueue().size(), executorQueueCapacity,
                                e.getActiveCount(), e.getMaximumPoolSize());
                        super.rejectedExecution(r, e);
                    }
                }
        );
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
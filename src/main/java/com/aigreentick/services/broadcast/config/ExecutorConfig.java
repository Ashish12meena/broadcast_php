package com.aigreentick.services.broadcast.config;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

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

    @Value("${campaign.semaphore-cleanup-enabled:true}")
    private boolean semaphoreCleanupEnabled;

    private final ConcurrentHashMap<String, Integer> activeCampaignsPerUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Semaphore> userSemaphoresMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> semaphoreLastUsedMap = new ConcurrentHashMap<>();

    private final AtomicLong totalTasksRejected = new AtomicLong(0);

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
                        totalTasksRejected.incrementAndGet();
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

    @Bean(name = "maintenanceExecutor", destroyMethod = "shutdown")
    public ScheduledExecutorService maintenanceExecutor() {
        return Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setName("maintenance-" + t.getId());
            t.setDaemon(true);
            return t;
        });
    }

    @Bean
    public ConcurrentHashMap<String, Semaphore> userSemaphores() {
        return userSemaphoresMap;
    }

    @Bean
    public ConcurrentHashMap<String, Long> semaphoreLastUsed() {
        return semaphoreLastUsedMap;
    }

    public static Semaphore getSemaphoreForUser(
            ConcurrentHashMap<String, Semaphore> userSemaphores,
            ConcurrentHashMap<String, Long> semaphoreLastUsed,
            String phoneNumberId) {

        semaphoreLastUsed.put(phoneNumberId, System.currentTimeMillis());
        return userSemaphores.computeIfAbsent(
                phoneNumberId,
                key -> new Semaphore(ConfigConstants.MAX_CONCURRENT_WHATSAPP_REQUESTS));
    }

    public void incrementActiveCampaigns(String phoneNumberId) {
        activeCampaignsPerUser.compute(phoneNumberId, (k, v) -> v == null ? 1 : v + 1);
    }

    public void decrementActiveCampaigns(String phoneNumberId) {
        activeCampaignsPerUser.compute(phoneNumberId, (k, v) -> {
            if (v == null || v <= 1) return null;
            return v - 1;
        });
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupInactiveSemaphores() {
        if (!semaphoreCleanupEnabled) return;

        long now = System.currentTimeMillis();
        long inactivityThreshold = TimeUnit.HOURS.toMillis(6);
        int removed = 0;

        for (var entry : userSemaphoresMap.entrySet()) {
            String phoneNumberId = entry.getKey();
            Semaphore semaphore = entry.getValue();

            Integer active = activeCampaignsPerUser.get(phoneNumberId);
            if (active != null && active > 0) continue;

            if (semaphore.availablePermits() == ConfigConstants.MAX_CONCURRENT_WHATSAPP_REQUESTS) {
                Long lastUsed = semaphoreLastUsedMap.get(phoneNumberId);
                if (lastUsed != null && (now - lastUsed) > inactivityThreshold) {
                    if (activeCampaignsPerUser.get(phoneNumberId) == null) {
                        if (userSemaphoresMap.remove(phoneNumberId) != null) {
                            semaphoreLastUsedMap.remove(phoneNumberId);
                            removed++;
                        }
                    }
                }
            }
        }

        if (removed > 0) {
            log.info("Semaphore cleanup: removed={} remaining={}", removed, userSemaphoresMap.size());
        }
    }
}

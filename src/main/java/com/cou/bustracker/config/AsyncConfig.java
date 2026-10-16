package com.cou.bustracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dedicated executor for background work (push notifications today).
 *
 * <p>Notification sends must never share the Tomcat request threads: an FCM
 * round trip costs 100-500 ms per batch and would otherwise hold a request —
 * and, when triggered from a write path, an open DB transaction — hostage.
 *
 * <p>Sizing is deliberately small: schedule changes are rare (a handful per
 * day), so 2-4 threads are plenty. The queue absorbs bursts; if it ever fills,
 * {@link ThreadPoolExecutor.CallerRunsPolicy} degrades to running the task on
 * the submitting thread instead of silently dropping the notification.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notify-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}

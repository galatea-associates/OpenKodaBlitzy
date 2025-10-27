/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR
A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.core.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


/**
 * Spring configuration class for asynchronous task execution and scheduled job support.
 * <p>
 * This configuration class enables two critical Spring framework capabilities:
 * <ul>
 * <li>{@code @EnableAsync} - Enables asynchronous method execution via {@code @Async} annotation</li>
 * <li>{@code @EnableScheduling} - Enables scheduled job execution via {@code @Scheduled} annotation</li>
 * </ul>
 * </p>
 * <p>
 * Configures two thread pool executors:
 * <ul>
 * <li>{@link ThreadPoolTaskExecutor} for async operations - core 5 threads, max 10 threads, queue capacity 250 tasks</li>
 * <li>{@link ThreadPoolTaskScheduler} for scheduled jobs - pool size 5 threads</li>
 * </ul>
 * </p>
 * <p>
 * This configuration is used by JobsScheduler and background job implementations across the application,
 * including EmailSenderJob, WebhookJob, SearchIndexUpdaterJob, and SystemHealthAlertJob.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ThreadPoolTaskExecutor
 * @see ThreadPoolTaskScheduler
 * @see EnableAsync
 * @see EnableScheduling
 */
@Configuration
@EnableAsync
@EnableScheduling
public class JobConfig {

    /**
     * Creates thread pool task executor for {@code @Async} method execution.
     * <p>
     * Configuration details:
     * <ul>
     * <li>Core pool size: 5 threads - minimum number of threads maintained in the pool</li>
     * <li>Maximum pool size: 10 threads - maximum number of threads allowed in the pool</li>
     * <li>Queue capacity: 250 tasks - number of tasks that can be queued when all threads are busy</li>
     * <li>Thread name prefix: 'async-' - for identification in logs and monitoring</li>
     * </ul>
     * </p>
     * <p>
     * This executor is used by Spring to execute methods annotated with {@code @Async} throughout
     * the application. When an async method is invoked, Spring submits it to this thread pool for
     * asynchronous background processing.
     * </p>
     *
     * @return configured ThreadPoolTaskExecutor for async method execution
     */
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(250);
        return executor;
    }

    /**
     * Creates thread pool task scheduler for {@code @Scheduled} method execution.
     * <p>
     * Configuration details:
     * <ul>
     * <li>Pool size: 5 threads - number of threads available for scheduled task execution</li>
     * <li>Bean name: 'taskScheduler' - required by Spring scheduling infrastructure</li>
     * </ul>
     * </p>
     * <p>
     * This scheduler is used by JobsScheduler to execute scheduled background jobs including:
     * <ul>
     * <li>EmailSenderJob - sends queued emails on schedule</li>
     * <li>WebhookJob - processes webhook deliveries</li>
     * <li>SearchIndexUpdaterJob - maintains search index consistency</li>
     * <li>SystemHealthAlertJob - monitors system health and sends alerts</li>
     * </ul>
     * Jobs are executed on cron schedules configured via {@code @Scheduled} annotations.
     * </p>
     *
     * @return configured ThreadPoolTaskScheduler for scheduled job execution
     */
    @Bean("taskScheduler")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        return scheduler;
    }
}

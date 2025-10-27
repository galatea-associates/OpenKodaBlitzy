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

package com.openkoda.core.tracker;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * AspectJ aspect that instruments scheduled jobs with correlation IDs for log tracing across job executions.
 * <p>
 * Intercepts all methods annotated with {@code @Scheduled}, injects a unique correlation ID into SLF4J MDC
 * before execution, and cleans up MDC after completion (including exception cases). Enables distributed tracing
 * for scheduled and cron jobs by ensuring each job execution has a unique identifier in log messages.
 * Uses Spring AOP with AspectJ annotations for declarative pointcut definitions.
 * </p>
 * <p>
 * The pointcut expression {@code @annotation(org.springframework.scheduling.annotation.Scheduled)} matches all
 * methods with Spring's {@code @Scheduled} annotation. This aspect is discovered via {@code @Component} and
 * {@code @Aspect} scanning, woven at runtime by Spring AOP proxy mechanism.
 * </p>
 * <p>
 * Example usage - automatic instrumentation:
 * <pre>{@code
 * @Scheduled(cron = "0 0 * * * *")
 * public void hourlyReport() {
 *     // Job ID automatically in MDC, appears in all log messages
 * }
 * }</pre>
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @since 2019-07-26
 * @version 1.7.1
 * @see RequestIdHolder
 * @see org.slf4j.MDC
 * @see org.springframework.scheduling.annotation.Scheduled
 */
@Aspect
@Component
public class TrackJobAspect {

    /**
     * Injects unique job correlation ID into MDC before scheduled method execution.
     * <p>
     * Before advice that executes before any {@code @Scheduled} method. Generates a unique job ID via
     * {@link RequestIdHolder#generate()} and stores it in SLF4J MDC under key {@code PARAM_CRON_JOB_ID}.
     * The ID propagates through all logging statements during job execution, enabling trace correlation.
     * Thread-safe as MDC is ThreadLocal-based.
     * </p>
     * <p>
     * Side effects: Modifies thread's MDC context by adding job ID entry.
     * </p>
     *
     * @see RequestIdHolder#generate()
     * @see org.slf4j.MDC#put(String, String)
     */
    @Before("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void setJobIdForThread() {
        MDC.put(RequestIdHolder.PARAM_CRON_JOB_ID, RequestIdHolder.generate());
    }

    /**
     * Clears MDC context after scheduled method completion.
     * <p>
     * After advice that executes after any {@code @Scheduled} method completes (including exception cases).
     * Calls {@link org.slf4j.MDC#clear()} to remove all MDC entries, preventing job ID leakage when thread
     * is returned to executor pool and reused for subsequent jobs. Critical for thread pool hygiene in
     * scheduled task executors.
     * </p>
     * <p>
     * Side effects: Clears all entries from thread's MDC context.
     * </p>
     * <p>
     * Note: Executes even if scheduled method throws exception, ensuring cleanup in all cases.
     * </p>
     *
     * @see org.slf4j.MDC#clear()
     */
    @After("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void clearJobIdForThread() {
        MDC.clear();
    }
}

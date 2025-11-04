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

package com.openkoda.core.job;

import com.openkoda.core.service.RestClientService;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.task.HttpRequestTask;
import com.openkoda.repository.task.HttpRequestTaskRepository;
import com.openkoda.repository.task.TaskRepository;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

/**
 * Scheduled background job that delivers HTTP request tasks to webhook endpoints.
 * <p>
 * This job is invoked by {@link JobsScheduler} with fixed delay scheduling (initial delay 10000ms, fixed delay 5000ms).
 * It processes webhook delivery tasks by claiming up to 100 oldest ready tasks atomically, then sends each via synchronous HTTP POST.
 * 
 * <p>
 * The job uses an atomic task claiming pattern through {@code findTasksAndSetStateDoing()}, which wraps
 * {@code findByCanBeStartedTrue(OLDEST_100)} to claim tasks and set their state to DOING in a single operation.
 * This prevents concurrent job executions from processing the same tasks.
 * 
 * <p>
 * HTTP delivery is performed synchronously using Spring {@link RestTemplate}. Each task includes a webhook URL,
 * JSON payload, and optional custom headers. Success is determined by HTTP 200 OK status, which triggers task completion.
 * Non-200 responses mark tasks as failed and log the error details.
 * 
 * <p>
 * This class implements {@link LoggingComponentWithRequestId} to enable request-id-aware tracing throughout the webhook delivery lifecycle.
 * 
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @since 2019-07-02
 * @version 1.7.1
 * @see JobsScheduler
 * @see HttpRequestTaskRepository
 * @see RestClientService
 * @see LoggingComponentWithRequestId
 */
@Component
public class PostMessagesToWebhookJob implements LoggingComponentWithRequestId {

    /**
     * Repository for persisting and querying {@link HttpRequestTask} entities.
     * <p>
     * Provides atomic task claiming via {@code findTasksAndSetStateDoing()} to prevent concurrent processing.
     * Also manages task state transitions (DOING, COMPLETED, FAILED) throughout the webhook delivery lifecycle.
     * 
     */
    @Inject
    private HttpRequestTaskRepository httpRequestTaskRepository;

    /**
     * Spring RestTemplate for executing HTTP POST requests to webhook endpoints.
     * <p>
     * Initialized in {@link #init()} method without custom configuration, using default timeouts and no authentication.
     * Used for synchronous webhook delivery operations.
     * 
     */
    private RestTemplate restTemplate;

    /**
     * Initializes the RestTemplate for webhook HTTP POST operations.
     * <p>
     * This lifecycle callback is invoked by Spring after dependency injection completes.
     * Creates a new RestTemplate instance with default configuration (standard timeouts, no custom authentication).
     * 
     * <p>
     * The RestTemplate is used for all synchronous HTTP POST requests to webhook endpoints in the {@link #send()} method.
     * 
     */
    @PostConstruct
    private void init() {
        debug("[init] Preparing RestTemplate object with headers");
        restTemplate = new RestTemplate();
    }

    /**
     * Processes and delivers webhook HTTP requests to their assigned endpoints.
     * <p>
     * This method runs within a Spring transaction and performs the following workflow:
     * 
     * <ol>
     *   <li>Atomically claims up to 100 oldest ready tasks via {@code findTasksAndSetStateDoing()}</li>
     *   <li>For each claimed task: calls {@code start()} and saves state as DOING</li>
     *   <li>Constructs HTTP POST request using {@link RestClientService#prepareHttpHeaders(HttpRequestTask)} for headers,
     *       {@code getJson()} for payload, and {@code getRequestUrl()} for endpoint</li>
     *   <li>Sends synchronous HTTP POST via {@link RestTemplate#postForEntity}</li>
     *   <li>On HTTP 200 OK: sets {@code dateSent}, calls {@code complete()}, and saves task</li>
     *   <li>On non-200 status: calls {@code fail()}, logs error with status code and response body, and saves task</li>
     * </ol>
     * <p>
     * The atomic claiming pattern ensures concurrent scheduler executions do not process the same tasks.
     * Task state transitions (null → DOING → COMPLETED/FAILED) are persisted throughout the lifecycle.
     * 
     * <p>
     * If an exception occurs during HTTP POST (network errors, timeouts), the transaction rolls back and the
     * task remains in ready state for retry on the next scheduler execution.
     * 
     *
     * @throws org.springframework.web.client.RestClientException if HTTP POST fails due to network errors or timeouts
     * @see HttpRequestTaskRepository#findTasksAndSetStateDoing
     * @see RestClientService#prepareHttpHeaders(HttpRequestTask)
     * @see HttpRequestTask#start()
     * @see HttpRequestTask#complete()
     * @see HttpRequestTask#fail()
     */
    @Transactional
    public void send() {
        trace("[send] to Webhook");

        // Retrieve the oldest 100 HttpRequestObjects stored in database to be sent (date sent must be null)
        Page<HttpRequestTask> httpRequestsToSend = httpRequestTaskRepository.findTasksAndSetStateDoing(
                () -> httpRequestTaskRepository.findByCanBeStartedTrue(TaskRepository.OLDEST_100) );

        httpRequestsToSend.forEach(httpRequestTask -> {
            httpRequestTask.start();
            httpRequestTaskRepository.save(httpRequestTask);
            HttpHeaders httpHeaders = RestClientService.prepareHttpHeaders(httpRequestTask);
            HttpEntity<String> httpEntity = new HttpEntity<>(httpRequestTask.getJson(), httpHeaders);
            ResponseEntity<String> webhookResponse = restTemplate.postForEntity(httpRequestTask.getRequestUrl(), httpEntity, String.class);
            //FIXME: [adrysch] why do we check for 'ok' or '1'?
            //if (webhookResponse.getStatusCode().equals(HttpStatus.OK) && (webhookResponse.getBody().equals("ok") || webhookResponse.getBody().equals("1"))) {
            if (webhookResponse.getStatusCode().equals(HttpStatus.OK)) {
                httpRequestTask.setDateSent(new Date());
                httpRequestTask.complete();
            } else {
                httpRequestTask.fail();
                error("Notification couldn't have been sent due to {} {}", webhookResponse.getStatusCodeValue(), webhookResponse.getBody());
            }
            httpRequestTaskRepository.save(httpRequestTask);
        });
    }
}

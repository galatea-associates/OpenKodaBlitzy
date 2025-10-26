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

import com.openkoda.core.service.email.EmailSender;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.task.Email;
import com.openkoda.repository.task.EmailRepository;
import com.openkoda.repository.task.TaskRepository;
import jakarta.inject.Inject;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled background job that processes and sends queued email tasks.
 * <p>
 * This job is invoked by {@link JobsScheduler} with a fixed-delay schedule:
 * initial delay of 10000ms and fixed delay of 5000ms between executions.
 * It implements an atomic task claiming pattern to safely process emails
 * in a concurrent environment.
 * </p>
 * <p>
 * The job uses repository-based locking via {@link EmailRepository#findTasksAndSetStateDoing}
 * to claim up to 10 ready email tasks and atomically set their state to DOING.
 * This prevents multiple job instances from processing the same email.
 * </p>
 * <p>
 * Each execution runs within a Spring transaction boundary (via {@code @Transactional}).
 * Successful email delivery updates the task state, while exceptions cause
 * transaction rollback and task state reversion for retry.
 * </p>
 * <p>
 * This class implements {@link LoggingComponentWithRequestId} to enable
 * request-id-aware tracing for debugging and audit trail purposes.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see JobsScheduler
 * @see EmailSender
 * @see EmailRepository
 * @see LoggingComponentWithRequestId
 */
@Component
public class EmailSenderJob  implements LoggingComponentWithRequestId {

    /**
     * Email delivery service that handles actual email transmission via SMTP.
     * Responsible for connecting to the configured mail server and sending
     * email content to recipients.
     */
    @Inject
    EmailSender emailSender;

    /**
     * Repository for persisting {@link Email} task entities and providing
     * atomic task claiming capabilities. The {@code findTasksAndSetStateDoing}
     * method enables safe concurrent task processing by atomically updating
     * task state during retrieval.
     */
    @Inject
    EmailRepository emailRepository;

    /**
     * Processes a batch of queued email tasks and sends them via the configured mail server.
     * <p>
     * This method executes within a Spring-managed transaction. It claims up to 10 oldest
     * ready email tasks using an atomic claiming mechanism that prevents concurrent processing.
     * The {@code findTasksAndSetStateDoing} method wraps {@code findByCanBeStartedTrue(OLDEST_10)}
     * to atomically retrieve emails and set their state to DOING.
     * </p>
     * <p>
     * <b>Separate Transaction Pattern:</b> The claiming operation occurs in a separate
     * transaction, so each email must be re-read in the execution transaction to obtain
     * a managed entity instance. This ensures proper JPA entity lifecycle management.
     * </p>
     * <p>
     * <b>Error Handling:</b> If {@link EmailSender#sendMail} throws an exception,
     * the transaction rolls back and the task state reverts to ready, enabling retry
     * on the next job execution. Successfully sent emails are saved with updated state.
     * </p>
     * <p>
     * Processing steps:
     * <ol>
     *   <li>Claim up to 10 oldest ready email tasks (atomic state transition to DOING)</li>
     *   <li>For each claimed email, re-read from repository to get managed entity</li>
     *   <li>Send email via {@link EmailSender}</li>
     *   <li>Save updated email entity with new state</li>
     * </ol>
     * </p>
     *
     * @throws RuntimeException if email delivery fails, causing transaction rollback
     * @see EmailRepository#findTasksAndSetStateDoing
     * @see EmailRepository#findByCanBeStartedTrue
     * @see TaskRepository#OLDEST_10
     * @see EmailSender#sendMail
     */
    @Transactional
    public void send() {
        trace("[send email job]");
        Page<Email> emails = emailRepository.findTasksAndSetStateDoing( () -> emailRepository.findByCanBeStartedTrue(TaskRepository.OLDEST_10) );
        for (Email e : emails.getContent()) {
            //as it's a separate transaction, we need to re-read the email
            Email e2 = emailRepository.findById(e.getId()).get();
            emailSender.sendMail(e2);
            emailRepository.save(e2);
        }
    }

}

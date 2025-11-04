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

package com.openkoda.model.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openkoda.model.Organization;
import com.openkoda.model.common.AuditableEntityOrganizationRelated;
import com.openkoda.model.common.ModelConstants;
import com.openkoda.model.common.TimestampedEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;

/**
 * Abstract base entity for background job execution and asynchronous task processing.
 * <p>
 * This class provides the foundation for implementing scheduled and asynchronous tasks within OpenKoda.
 * Tasks support automatic retry logic with exponential backoff, multi-tenant isolation, and polymorphic
 * task types through JPA single-table inheritance.
 * 
 *
 * <b>JPA Mapping</b>
 * <p>
 * Persisted to {@code tasks} table using {@code @Inheritance(strategy=SINGLE_TABLE)} with
 * {@code @DiscriminatorColumn(name="type")} for polymorphic task types. Concrete subclasses include
 * {@link Email} and {@link HttpRequestTask}, differentiated by discriminator values.
 * 
 *
 * <b>Task Lifecycle</b>
 * <p>
 * Tasks progress through the following state transitions:
 * 
 * <pre>
 * NEW → DOING → DONE (successful completion)
 *           ↓
 *         FAILED → (retry) → DOING
 *           ↓
 *     FAILED_PERMANENTLY (max attempts exceeded)
 * </pre>
 *
 * <b>Execution Workflow</b>
 * <ol>
 *   <li>Task created with state=NEW and scheduled startAfter time</li>
 *   <li>Background worker queries for tasks where {@code canBeStarted=true}</li>
 *   <li>Worker calls {@link #start()} to transition to DOING state</li>
 *   <li>Task executes business logic in subclass implementation</li>
 *   <li>On success: {@link #complete()} transitions to DONE</li>
 *   <li>On failure: {@link #fail()} transitions to FAILED (retriable) or FAILED_PERMANENTLY</li>
 * </ol>
 *
 * <b>Retry Logic</b>
 * <p>
 * Failed tasks automatically retry up to {@code MAX_ATTEMPTS_DEFAULT} (5 attempts) times. After each
 * failure, the task transitions to FAILED state and becomes eligible for retry when {@code canBeStarted}
 * evaluates to true. Tasks exceeding maximum retry attempts transition to FAILED_PERMANENTLY and will
 * not be retried.
 * 
 *
 * <b>Multi-Tenancy</b>
 * <p>
 * Implements {@link AuditableEntityOrganizationRelated} for organization-scoped task isolation. Tasks
 * are associated with an {@link Organization} via {@code organizationId} field for write operations
 * and {@code organization} relation for read operations.
 * 
 *
 * <b>Task Scheduling</b>
 * <p>
 * The {@code startAfter} field controls execution timing. Tasks become eligible for execution when
 * {@code current_timestamp > startAfter} and state is NEW or FAILED. This eligibility is computed
 * via {@code @Formula} for efficient database-side filtering.
 * 
 *
 * <b>Thread Safety</b>
 * <p>
 * State mutations ({@link #start()}, {@link #fail()}, {@link #complete()}) are not synchronized and
 * rely on JPA transaction isolation. Concurrent execution of the same task should be prevented by
 * background worker coordination mechanisms.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.core.job
 * @see Email
 * @see HttpRequestTask
 * @see AuditableEntityOrganizationRelated
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class Task extends TimestampedEntity implements AuditableEntityOrganizationRelated {

    /**
     * SQL formula constant for computing the {@code referenceString} field.
     * <p>
     * Uses {@code DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA} to generate a computed
     * audit reference string for cross-entity indexing and tracking. This formula is applied
     * via {@code @Formula} annotation on the {@link #referenceString} field.
     * 
     *
     * @see #referenceString
     */
    public static final String REFERENCE_FORMULA = DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA;

    /**
     * Primary key for the task entity.
     * <p>
     * Generated using {@code ORGANIZATION_RELATED_ID_GENERATOR} sequence with {@code initialValue}
     * set to {@code INITIAL_ORGANIZATION_RELATED_VALUE} and {@code allocationSize=10} for batch
     * allocation optimization. This allocation size allows JPA to pre-fetch 10 sequence values,
     * reducing database round-trips during bulk task creation.
     * 
     *
     * @see ModelConstants#INITIAL_ORGANIZATION_RELATED_VALUE
     */
    @Id
    @SequenceGenerator(name = ORGANIZATION_RELATED_ID_GENERATOR, sequenceName = ORGANIZATION_RELATED_ID_GENERATOR, initialValue = ModelConstants.INITIAL_ORGANIZATION_RELATED_VALUE, allocationSize = 10)
    @GeneratedValue(generator = ORGANIZATION_RELATED_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Default maximum retry attempts before task fails permanently.
     * <p>
     * Tasks failing during execution will retry up to this limit (5 attempts). After exceeding
     * this threshold, the task transitions to {@code FAILED_PERMANENTLY} state and will not be
     * retried. Subclasses can override {@link #getMaxAttempts()} to customize retry behavior.
     * 
     *
     * @see #getMaxAttempts()
     * @see TaskState#FAILED_PERMANENTLY
     */
    private static final int MAX_ATTEMPTS_DEFAULT = 5;

    /**
     * Enumeration of task lifecycle states.
     * <p>
     * Defines the possible states a task can be in during its execution lifecycle, from creation
     * through completion or permanent failure.
     * 
     */
    public enum TaskState {
        /**
         * Task has been created but not yet started. Initial state for all new tasks.
         * Tasks in this state are eligible for execution when {@code startAfter} time is reached.
         */
        NEW,

        /**
         * Task is currently executing. Set when background worker calls {@link #start()}.
         * Tasks in this state should not be picked up by other workers.
         */
        DOING,

        /**
         * Task completed successfully. Terminal state indicating successful execution.
         * Tasks in this state will not be processed again.
         */
        DONE,

        /**
         * Task execution failed but can be retried. Set when {@code attempts < MAX_ATTEMPTS}.
         * Tasks in this state become eligible for retry when {@code startAfter} conditions are met.
         */
        FAILED,

        /**
         * Task has exhausted all retry attempts and will not retry. Terminal failure state.
         * Set when {@code attempts >= MAX_ATTEMPTS} after a failure. Requires manual intervention.
         */
        FAILED_PERMANENTLY
    }

    /**
     * Current retry attempt count for this task.
     * <p>
     * Initialized to 0 in constructor. Incremented each time {@link #start()} is called.
     * Compared against {@link #getMaxAttempts()} to determine if task should transition to
     * {@code FAILED_PERMANENTLY} state after a failure.
     * 
     *
     * @see #start()
     * @see #fail()
     * @see #getMaxAttempts()
     */
    private int attempts;

    /**
     * Scheduled execution time for this task.
     * <p>
     * Tasks become eligible for execution when {@code current_timestamp > startAfter} and state
     * is {@code NEW} or {@code FAILED}. This allows for delayed task execution and scheduled jobs.
     * Persisted to {@code start_after} database column.
     * 
     * <p>
     * Default value: {@link LocalDateTime#now()} (immediate execution) when using no-arg constructor.
     * Can be set to future time for delayed execution via {@link #Task(LocalDateTime)} constructor.
     * 
     *
     * @see #canBeStarted
     * @see LocalDateTime
     */
    @Column(name = "start_after")
    private LocalDateTime startAfter;

    /**
     * Current lifecycle state of this task.
     * <p>
     * Persisted as {@code @Enumerated(STRING)} for human-readable database values. State transitions
     * are managed by {@link #start()}, {@link #fail()}, and {@link #complete()} methods. Initial
     * state is {@code NEW}, set in constructor.
     * 
     *
     * @see TaskState
     * @see #start()
     * @see #fail()
     * @see #complete()
     */
    @Enumerated(STRING)
    private TaskState state;

    /**
     * Computed flag indicating whether this task is eligible for execution.
     * <p>
     * Calculated via {@code @Formula} for efficient database-side filtering:
     * {@code (current_timestamp > start_after AND (state = 'NEW' OR state = 'FAILED'))}
     * 
     * <p>
     * Returns {@code true} when:
     * 
     * <ul>
     *   <li>Current database timestamp is after the {@code startAfter} scheduled time, AND</li>
     *   <li>Task state is either {@code NEW} (not yet started) or {@code FAILED} (ready for retry)</li>
     * </ul>
     * <p>
     * This computed field enables background workers to efficiently query for executable tasks
     * using a single database filter without loading task entities into memory.
     * 
     *
     * @see #startAfter
     * @see TaskState#NEW
     * @see TaskState#FAILED
     */
    @Formula("(current_timestamp > start_after AND (state = 'NEW' OR state = 'FAILED'))")
    private boolean canBeStarted;

    /**
     * Associated organization for multi-tenant task isolation.
     * <p>
     * {@code @ManyToOne} association to {@link Organization} entity, used for read operations.
     * Marked {@code @JsonIgnore} to prevent circular serialization. Join column {@code organization_id}
     * is marked {@code insertable=false, updatable=false} because writes are performed via the
     * {@link #organizationId} scalar field.
     * 
     * <p>
     * <strong>Note:</strong> TODO Rule 4.4 indicates this should be marked with {@code FetchType.LAZY}
     * for performance optimization to avoid unnecessary eager loading of organization data.
     * 
     *
     * @see Organization
     * @see #organizationId
     * @see AuditableEntityOrganizationRelated
     */
    //TODO Rule 4.4: should be marked with FetchType = LAZY
    @JsonIgnore
    @ManyToOne(optional = true)
    @JoinColumn(nullable = true, insertable = false, updatable = false, name = ORGANIZATION_ID)
    private Organization organization;

    /**
     * Organization ID for multi-tenant task scoping.
     * <p>
     * Scalar organization identifier used for write operations (inserts and updates). The corresponding
     * {@link #organization} relation is used for read operations. Both fields map to the same database
     * column {@code organization_id} but serve different purposes in the entity lifecycle.
     * 
     * <p>
     * Nullable to support tasks not scoped to a specific organization (system-wide tasks).
     * 
     *
     * @see #organization
     * @see AuditableEntityOrganizationRelated#getOrganizationId()
     */
    @Column(nullable = true, name = ORGANIZATION_ID)
    private Long organizationId;

    /**
     * Computed reference string for auditing and cross-entity indexing.
     * <p>
     * Calculated via {@code @Formula} using {@link #REFERENCE_FORMULA} which applies
     * {@code DEFAULT_ORGANIZATION_RELATED_REFERENCE_FIELD_FORMULA}. This provides a consistent
     * reference identifier used by the auditing subsystem for tracking entity changes and
     * establishing relationships across entity types.
     * 
     * <p>
     * This field is read-only and computed by the database on each entity load. It is not
     * persisted as a physical column but generated dynamically via SQL formula.
     * 
     *
     * @see #REFERENCE_FORMULA
     * @see AuditableEntityOrganizationRelated#getReferenceString()
     */
    @Formula(REFERENCE_FORMULA)
    private String referenceString;

    /**
     * Creates a new task with default settings for immediate execution.
     * <p>
     * Initializes the task with:
     * 
     * <ul>
     *   <li>{@code attempts = 0} - No prior execution attempts</li>
     *   <li>{@code state = NEW} - Ready for first execution</li>
     *   <li>{@code startAfter = LocalDateTime.now()} - Eligible for immediate execution</li>
     * </ul>
     * <p>
     * Use this constructor when creating tasks that should be executed as soon as a background
     * worker becomes available.
     * 
     *
     * @see LocalDateTime#now()
     * @see TaskState#NEW
     */
    public Task() {
        attempts = 0;
        state = TaskState.NEW;
        startAfter = LocalDateTime.now();
    }

    /**
     * Creates a new task with custom scheduled execution time.
     * <p>
     * Initializes the task with:
     * 
     * <ul>
     *   <li>{@code attempts = 0} - No prior execution attempts</li>
     *   <li>{@code state = NEW} - Ready for first execution</li>
     *   <li>{@code startAfter} - Custom scheduled execution time</li>
     * </ul>
     * <p>
     * Use this constructor when creating tasks that should execute at a specific future time,
     * such as scheduled jobs or delayed processing. The task becomes eligible for execution
     * when {@code current_timestamp > startAfter}.
     * 
     *
     * @param startAfter the scheduled execution time; must not be null
     * @see #canBeStarted
     * @see TaskState#NEW
     */
    public Task(LocalDateTime startAfter) {
        attempts = 0;
        state = TaskState.NEW;
        this.startAfter = startAfter;
    }

    /**
     * Transitions the task to executing state and increments the attempt counter.
     * <p>
     * This method should be called by background workers when beginning task execution. It performs:
     * 
     * <ol>
     *   <li>Increments {@code attempts} counter to track retry count</li>
     *   <li>Transitions {@code state} to {@code DOING} to mark task as executing</li>
     * </ol>
     * <p>
     * <strong>Thread Safety:</strong> This method is not synchronized and relies on JPA transaction
     * isolation to prevent concurrent execution. Background worker coordination should ensure only
     * one worker calls this method per task at a time.
     * 
     * <p>
     * <strong>Usage:</strong> Call this method within a transaction context before executing the
     * task's business logic. Follow with either {@link #complete()} on success or {@link #fail()}
     * on failure.
     * 
     *
     * @see #complete()
     * @see #fail()
     * @see TaskState#DOING
     */
    public void start() {
        attempts++;
        state = TaskState.DOING;
    }

    /**
     * Handles task execution failure with automatic retry logic.
     * <p>
     * Transitions the task state based on remaining retry attempts:
     * 
     * <ul>
     *   <li>If {@code attempts >= getMaxAttempts()}: transitions to {@code FAILED_PERMANENTLY}
     *       (terminal state, no further retries)</li>
     *   <li>If {@code attempts < getMaxAttempts()}: transitions to {@code FAILED}
     *       (retriable state, task becomes eligible for retry)</li>
     * </ul>
     * <p>
     * <strong>Usage:</strong> Call this method when task execution encounters an error or exception.
     * This should be done within a transaction context to ensure atomic state updates. The task
     * will automatically become eligible for retry when in {@code FAILED} state and {@code canBeStarted}
     * evaluates to true.
     * 
     * <p>
     * <strong>Recovery:</strong> Tasks in {@code FAILED_PERMANENTLY} state require manual intervention
     * to reset or investigate the underlying issue. Consider logging detailed failure information
     * before calling this method.
     * 
     *
     * @see #getMaxAttempts()
     * @see TaskState#FAILED
     * @see TaskState#FAILED_PERMANENTLY
     * @see #canBeStarted
     */
    public void fail() {
        if(attempts >= getMaxAttempts()) {
            state = TaskState.FAILED_PERMANENTLY;
        } else {
            state = TaskState.FAILED;
        }
    }

    /**
     * Transitions the task to completed state indicating successful execution.
     * <p>
     * Sets {@code state} to {@code DONE}, marking the task as successfully completed. This is a
     * terminal state - the task will not be processed again by background workers.
     * 
     * <p>
     * <strong>Usage:</strong> Call this method after successfully executing the task's business logic,
     * within the same transaction context. This ensures atomic completion and prevents the task from
     * being picked up by other workers.
     * 
     *
     * @see TaskState#DONE
     * @see #start()
     */
    public void complete() {
        state = TaskState.DONE;
    }

    /**
     * Returns the maximum number of retry attempts before permanent failure.
     * <p>
     * Default implementation returns {@code MAX_ATTEMPTS_DEFAULT} (5 attempts). Subclasses can
     * override this method to implement custom retry policies based on task type, priority, or
     * other business requirements.
     * 
     * <p>
     * This value is compared against {@code attempts} in the {@link #fail()} method to determine
     * whether the task should transition to {@code FAILED} (retriable) or {@code FAILED_PERMANENTLY}
     * (terminal) state.
     * 
     *
     * @return the maximum retry attempts; default is 5
     * @see #MAX_ATTEMPTS_DEFAULT
     * @see #fail()
     * @see #attempts
     */
    public int getMaxAttempts() {
        return MAX_ATTEMPTS_DEFAULT;
    }

    /**
     * Returns the unique identifier for this task.
     * <p>
     * The task ID is generated using the {@code ORGANIZATION_RELATED_ID_GENERATOR} sequence
     * with batch allocation optimization ({@code allocationSize=10}).
     * 
     *
     * @return the task primary key, or null if not yet persisted
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * Returns the current retry attempt count.
     * <p>
     * This value is incremented each time {@link #start()} is called. It is compared against
     * {@link #getMaxAttempts()} to determine retry eligibility after failures.
     * 
     *
     * @return the number of execution attempts for this task (0 for new tasks)
     * @see #start()
     * @see #getMaxAttempts()
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * Returns the current lifecycle state of this task.
     * <p>
     * State progresses through: NEW → DOING → DONE/FAILED/FAILED_PERMANENTLY.
     * State transitions are managed by {@link #start()}, {@link #complete()}, and {@link #fail()}.
     * 
     *
     * @return the current task state (never null)
     * @see TaskState
     * @see #start()
     * @see #complete()
     * @see #fail()
     */
    public TaskState getState() {
        return state;
    }

    /**
     * Returns whether this task is eligible for execution.
     * <p>
     * This value is computed via {@code @Formula} on the database side using the expression:
     * {@code (current_timestamp > start_after AND (state = 'NEW' OR state = 'FAILED'))}.
     * 
     * <p>
     * Returns {@code true} when the task meets all conditions:
     * 
     * <ul>
     *   <li>Current database time is after the scheduled {@code startAfter} time</li>
     *   <li>Task state is either {@code NEW} (not yet executed) or {@code FAILED} (ready for retry)</li>
     * </ul>
     * <p>
     * Background workers use this field to efficiently filter executable tasks in database queries
     * without loading entities into memory.
     * 
     *
     * @return true if the task is eligible for execution; false otherwise
     * @see #canBeStarted
     * @see #startAfter
     * @see TaskState#NEW
     * @see TaskState#FAILED
     */
    public boolean isCanBeStarted() {
        return canBeStarted;
    }

    /**
     * Returns the scheduled execution time for this task.
     * <p>
     * Tasks become eligible for execution when the current database timestamp exceeds this value.
     * Used in combination with task state to determine {@link #canBeStarted} eligibility.
     * 
     *
     * @return the scheduled execution time (never null)
     * @see #setStartAfter(LocalDateTime)
     * @see #canBeStarted
     */
    public LocalDateTime getStartAfter() {
        return startAfter;
    }

    /**
     * Sets the scheduled execution time for this task.
     * <p>
     * Use this method to reschedule a task for delayed or future execution. The task will become
     * eligible when the current database timestamp exceeds this value and the task state is
     * {@code NEW} or {@code FAILED}.
     * 
     *
     * @param startAfter the scheduled execution time; must not be null
     * @see #getStartAfter()
     * @see #canBeStarted
     */
    public void setStartAfter(LocalDateTime startAfter) {
        this.startAfter = startAfter;
    }

    /**
     * Increments the attempt counter without changing task state.
     * <p>
     * This method provides manual control over the attempt count, useful for custom retry logic
     * or debugging scenarios. Unlike {@link #start()}, this method does not transition the task
     * to {@code DOING} state.
     * 
     * <p>
     * <strong>Note:</strong> In normal operation, use {@link #start()} which both increments
     * attempts and updates state atomically.
     * 
     *
     * @see #start()
     * @see #getAttempts()
     */
    public void increaseAttempt() {
        this.attempts++;
    }

    /**
     * Returns the organization ID for multi-tenant task scoping.
     * <p>
     * Implements {@link AuditableEntityOrganizationRelated#getOrganizationId()} to provide
     * tenant isolation. Tasks scoped to a specific organization use this ID for filtering
     * and authorization checks.
     * 
     *
     * @return the organization ID, or null for system-wide tasks
     * @see #setOrganizationId(Long)
     * @see #organization
     * @see AuditableEntityOrganizationRelated
     */
    @Override
    public Long getOrganizationId() {
        return organizationId;
    }

    /**
     * Returns the computed reference string for auditing and tracking.
     * <p>
     * Implements {@link AuditableEntityOrganizationRelated#getReferenceString()} to provide
     * a consistent identifier used by the auditing subsystem. This value is computed dynamically
     * via {@code @Formula} using {@link #REFERENCE_FORMULA}.
     * 
     *
     * @return the computed reference string (read-only, database-generated)
     * @see #REFERENCE_FORMULA
     * @see AuditableEntityOrganizationRelated
     */
    @Override
    public String getReferenceString() {
        return referenceString;
    }

    /**
     * Sets the organization ID for multi-tenant task scoping.
     * <p>
     * Assigns this task to a specific organization for tenant isolation. This scalar field is
     * used for write operations (inserts and updates), while the {@link #organization} relation
     * is used for read operations.
     * 
     *
     * @param organizationId the organization ID, or null for system-wide tasks
     * @see #getOrganizationId()
     * @see #organization
     */
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Returns the associated organization entity for this task.
     * <p>
     * Provides access to the full {@link Organization} entity via {@code @ManyToOne} relation.
     * This is a read-only association - updates should use {@link #setOrganizationId(Long)} to
     * modify the organization assignment.
     * 
     * <p>
     * <strong>Note:</strong> This association may be lazy-loaded depending on fetch strategy.
     * Accessing this field outside of an active transaction may result in {@code LazyInitializationException}.
     * 
     *
     * @return the associated organization entity, or null if not scoped to an organization or not yet loaded
     * @see #getOrganizationId()
     * @see #setOrganizationId(Long)
     * @see Organization
     */
    public Organization getOrganization() {
        return organization;
    }
}

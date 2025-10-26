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

package com.openkoda.core.service.event;

import com.openkoda.dto.NotificationDto;
import com.openkoda.dto.OrganizationDto;
import com.openkoda.dto.RegisteredUserDto;
import com.openkoda.dto.payment.PlanDto;
import com.openkoda.dto.system.ScheduledSchedulerDto;
import com.openkoda.dto.user.BasicUser;
import com.openkoda.dto.user.UserRoleDto;
import com.openkoda.model.component.Scheduler;
import com.openkoda.model.component.event.EventListenerEntry;

import java.io.File;
import java.time.LocalDateTime;

/**
 * Defines canonical application-wide event type registry with typed payload descriptors for type-safe event publishing and consumption.
 * <p>
 * This class extends {@link AbstractApplicationEvent} to leverage event registry and metadata management capabilities.
 * Each public static final field represents a distinct event type with associated payload Class for compile-time type safety.
 * All event descriptors are instantiated during static class initialization and registered in AbstractApplicationEvent.eventList,
 * providing a centralized registry of all available application events.
 * </p>
 * <p>
 * <b>Architecture:</b> Event constants are used with ApplicationEventService.emitEvent() for publishing events and 
 * EventListenerService.registerEventListener() for subscribing to events. The typed payload ensures type safety at compile time
 * while the event name provides runtime identification.
 * </p>
 * <p>
 * <b>Thread Safety:</b> Static initialization is thread-safe per Java specification. Some non-final fields 
 * (NOTIFICATION_CREATED, BACKUP_CREATED, SCHEDULER_EXECUTED) allow controlled replacement in test scenarios.
 * </p>
 * <p>
 * Example usage - Event publishing:
 * <pre>{@code
 * services.applicationEvent.emitEvent(ApplicationEvent.USER_CREATED, basicUser);
 * }</pre>
 * </p>
 * <p>
 * Example usage - Listener registration:
 * <pre>{@code
 * services.eventListener.registerListener(ApplicationEvent.ORGANIZATION_CREATED, this::handleOrgCreated);
 * }</pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @since 1.7.1
 * @see AbstractApplicationEvent
 * @see com.openkoda.core.service.ApplicationEventService
 * @see com.openkoda.core.service.event.EventListenerService
 */
public class ApplicationEvent<T> extends AbstractApplicationEvent<T> {

    /**
     * Emitted when new user account created; payload contains BasicUser with credentials and profile; published by UserService.createUser().
     */
    public static final ApplicationEvent<BasicUser> USER_CREATED = new ApplicationEvent(BasicUser.class, "USER_CREATED");
    
    /**
     * Emitted after successful user self-registration; payload contains RegisteredUserDto with verification token; published by RegistrationController.
     */
    public static final ApplicationEvent<RegisteredUserDto> USER_REGISTERED = new ApplicationEvent(RegisteredUserDto.class, "USER_REGISTERED");
    
    /**
     * Emitted when user profile or settings updated; payload contains BasicUser with updated fields; published by UserService.updateUser().
     */
    public static final ApplicationEvent<BasicUser> USER_MODIFIED = new ApplicationEvent(BasicUser.class, "USER_MODIFIED");
    
    /**
     * Emitted when user account deleted/deactivated; payload contains BasicUser with pre-deletion state; published by UserService.deleteUser().
     */
    public static final ApplicationEvent<BasicUser> USER_DELETED = new ApplicationEvent(BasicUser.class, "USER_DELETED");
    
    /**
     * Emitted after email verification completed; payload contains BasicUser with verified flag; published by RegistrationController.
     */
    public static final ApplicationEvent<BasicUser> USER_VERIFIED = new ApplicationEvent(BasicUser.class, "USER_VERIFIED");
    
    /**
     * Emitted on successful authentication; payload contains BasicUser for audit logging and session tracking; published by AuthenticationSuccessHandler.
     */
    public static final ApplicationEvent<BasicUser> USER_LOGGED_IN = new ApplicationEvent(BasicUser.class, "USER_LOGGED_IN");
    
    /**
     * Emitted when new tenant organization provisioned; payload contains OrganizationDto with initial configuration; published by OrganizationService.createOrganization().
     */
    public static final ApplicationEvent<OrganizationDto> ORGANIZATION_CREATED = new ApplicationEvent(OrganizationDto.class, "ORGANIZATION_CREATED");
    
    /**
     * Emitted when organization settings/branding updated; payload contains OrganizationDto with modified fields; published by OrganizationService.updateOrganization().
     */
    public static final ApplicationEvent<OrganizationDto> ORGANIZATION_MODIFIED = new ApplicationEvent(OrganizationDto.class, "ORGANIZATION_MODIFIED");
    
    /**
     * Emitted when organization removed; payload contains OrganizationDto with pre-deletion state; published by OrganizationService.removeOrganization().
     */
    public static final ApplicationEvent<OrganizationDto> ORGANIZATION_DELETED = new ApplicationEvent(OrganizationDto.class, "ORGANIZATION_DELETED");
    
    /**
     * Emitted when user assigned to role; payload contains UserRoleDto with user/role/organization association; published by RoleService.
     */
    public static final ApplicationEvent<UserRoleDto> USER_ROLE_CREATED = new ApplicationEvent(UserRoleDto.class, "USER_ROLE_CREATED");
    
    /**
     * Emitted when user role permissions updated; payload contains UserRoleDto with modified privileges; published by RoleService.
     */
    public static final ApplicationEvent<UserRoleDto> USER_ROLE_MODIFIED = new ApplicationEvent(UserRoleDto.class, "USER_ROLE_MODIFIED");
    
    /**
     * Emitted when user removed from role; payload contains UserRoleDto with pre-deletion association; published by RoleService.
     */
    public static final ApplicationEvent<UserRoleDto> USER_ROLE_DELETED = new ApplicationEvent(UserRoleDto.class, "USER_ROLE_DELETED");
    
    /**
     * Emitted when new event consumer registered dynamically; payload contains EventListenerEntry with consumer metadata; published by EventListenerService.registerListener().
     */
    public static final ApplicationEvent<EventListenerEntry> EVENT_LISTENER_CREATED = new ApplicationEvent(EventListenerEntry.class, "EVENT_LISTENER_CREATED");
    
    /**
     * Emitted when event consumer configuration updated; payload contains EventListenerEntry with modified settings; published by EventListenerService.
     */
    public static final ApplicationEvent<EventListenerEntry> EVENT_LISTENER_MODIFIED = new ApplicationEvent(EventListenerEntry.class, "EVENT_LISTENER_MODIFIED");
    
    /**
     * Emitted when event consumer unregistered; payload contains EventListenerEntry with pre-deletion state; published by EventListenerService.
     */
    public static final ApplicationEvent<EventListenerEntry> EVENT_LISTENER_DELETED = new ApplicationEvent(EventListenerEntry.class, "EVENT_LISTENER_DELETED");
    
    /**
     * Emitted when trial subscription activated; payload contains PlanDto with trial period details; published by SubscriptionService.
     */
    public static final ApplicationEvent<PlanDto> TRIAL_ACTIVATED = new ApplicationEvent<>(PlanDto.class, "TRIAL_ACTIVATED");
    
    /**
     * Emitted when trial subscription expires; payload contains PlanDto with expiration timestamp; published by SchedulerService/SubscriptionService.
     */
    public static final ApplicationEvent<PlanDto> TRIAL_EXPIRED = new ApplicationEvent<>(PlanDto.class, "TRIAL_EXPIRED");

    /**
     * Emitted on uncaught exceptions or critical errors; payload contains NotificationDto with error details and stack trace; published by GlobalExceptionHandler.
     */
    public static final ApplicationEvent<NotificationDto> APPLICATION_ERROR = new ApplicationEvent<>(NotificationDto.class, "APPLICATION_ERROR");
    
    /**
     * Emitted after successful Spring context initialization; payload contains LocalDateTime of startup timestamp; published by ApplicationReadyEvent listener.
     */
    public static final ApplicationEvent<LocalDateTime> APPLICATION_STARTED = new ApplicationEvent<>(LocalDateTime.class, "APPLICATION_STARTED");

    /**
     * Emitted when notification queued for delivery; payload contains NotificationDto with recipient and content; published by NotificationService.
     * Note: Non-final to allow test overrides.
     */
    public static ApplicationEvent<NotificationDto> NOTIFICATION_CREATED = new ApplicationEvent<>(NotificationDto.class, "NOTIFICATION_CREATED");
    
    /**
     * Event emitted in {@link com.openkoda.core.service.BackupService} when application backup is finished.
     * Emitted along with the tar backup file.
     * Note: Non-final to allow test overrides.
     */
    public static ApplicationEvent<File> BACKUP_CREATED = new ApplicationEvent(File.class, "BACKUP_CREATED");

    /**
     * Event emitted in {@link com.openkoda.core.service.BackupService} after successfully performing a secure copy of backup archive file.
     * Payload contains String destination path; published by BackupService.
     */
    public static final ApplicationEvent<String> BACKUP_FILE_COPIED = new ApplicationEvent<>(String.class, "BACKUP_FILE_COPIED");

    /**
     * This event is used when running {@link Scheduler}.
     * The scheduled task emits SCHEDULER_EXECUTED event along with String parameter which invokes proper consumers
     * on the basis of the String value.
     * Payload contains ScheduledSchedulerDto with task identifier and parameters; published by SchedulerService.SchedulerTask.
     * Note: Non-final to allow test overrides.
     */
    public static ApplicationEvent<ScheduledSchedulerDto> SCHEDULER_EXECUTED = new ApplicationEvent(ScheduledSchedulerDto.class, "SCHEDULER_EXECUTED");


    /**
     * Protected constructor invoked only during static initialization to create canonical event constants.
     * <p>
     * This constructor delegates to the superclass to register the event descriptor in the global event registry,
     * associating the event name with its typed payload class for runtime validation and type-safe event handling.
     * </p>
     *
     * @param eventClass Runtime Class representing typed payload for compile-time type safety and runtime validation
     * @param eventName Unique canonical event identifier matching constant name convention (e.g., 'USER_CREATED')
     * @see AbstractApplicationEvent for superclass constructor documentation and registry mechanism
     */
    protected ApplicationEvent(Class<T> eventClass, String eventName) {
        super(eventClass, eventName);
    }

}

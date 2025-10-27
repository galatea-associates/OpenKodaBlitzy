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

package com.openkoda.core.service;

import com.openkoda.core.configuration.WebSocketConfig;
import com.openkoda.core.helper.ReadableCode;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.User;
import com.openkoda.repository.user.UserRepository;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sends messages to WebSocket channels for real-time communication with connected clients.
 * <p>
 * Channel name is a logical name for a messaging queue. There are two types of channels:
 * - broadcast channel: messages sent to all subscribers
 * - user channel: messages sent to specific authenticated users
 * </p>
 * <p>
 * For broadcast channel:
 * - on client side subscribe to channel eg. '/queue/broadcast-channel'
 * - on server/sender side, use sendToChannel method with channelName == '/queue/broadcast-channel'
 * </p>
 * <p>
 * For user channel:
 * - on client side subscribe to channel eg. '/user/queue/user-notifications'
 * - on server/sender side, use sendToUserChannel with channelName == '/queue/user-notifications'
 * (note: exclude /user prefix in server-side calls, it is added automatically)
 * </p>
 * <p>
 * This service wraps Spring's {@link SimpMessagingTemplate} for STOMP messaging operations.
 * Channel names are validated to ensure they start with the configured prefix from {@link WebSocketConfig#CHANNEL_PREFIX}.
 * User email resolution is performed via {@link UserRepository} when userId or User entity is provided.
 * Payload objects are automatically serialized to JSON via Jackson for WebSocket transmission.
 * </p>
 * <p>
 * Usage examples:
 * <pre>{@code
 * websocketService.sendToChannel("/queue/broadcast-channel", notification);
 * websocketService.sendToUserChannel(userId, "/queue/user-notifications", message);
 * }</pre>
 * </p>
 * <p>
 * <b>Warning:</b> Methods return true even if the user is not connected or the channel is invalid.
 * No delivery confirmation is provided.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SimpMessagingTemplate
 * @see WebSocketConfig
 * @see UserRepository
 * @see ReadableCode
 * @see LoggingComponentWithRequestId
 */
@Service
public class WebsocketService implements ReadableCode, LoggingComponentWithRequestId {

    /**
     * Spring STOMP messaging template for WebSocket operations.
     * Used to send messages to broadcast channels and user-specific destinations.
     */
    @Inject
    private SimpMessagingTemplate messagingTemplate;

    /**
     * User repository for looking up user email addresses by user ID.
     * Required for user-targeted WebSocket messages when userId is provided instead of email.
     */
    @Inject
    private UserRepository userRepository;

    /**
     * Validates that the channel name starts with the configured channel prefix.
     * <p>
     * Logs a warning if the channel name does not start with {@link WebSocketConfig#CHANNEL_PREFIX}.
     * This typically indicates a configuration error but does not throw an exception.
     * </p>
     *
     * @param channelName the WebSocket destination channel name to validate
     */
    private void checkChannelName(String channelName) {
        if (not(StringUtils.startsWith(channelName, WebSocketConfig.CHANNEL_PREFIX))) {
            warn("[WebsocketService] Probably wrong broadcast channel name: {}", channelName);
        }
    }

    /**
     * Broadcasts a message to all subscribers of the specified channel.
     * <p>
     * All clients subscribed to the channel will receive the message.
     * The payload object is automatically serialized to JSON via Jackson.
     * Channel name is validated to ensure it starts with the configured prefix.
     * </p>
     * <p>
     * Usage example:
     * <pre>{@code
     * websocketService.sendToChannel("/queue/broadcast-channel", notification);
     * }</pre>
     * </p>
     *
     * @param channelName the WebSocket destination (e.g., '/queue/broadcast-channel')
     * @param payload the message object to send, will be serialized to JSON
     * @return always returns true (no delivery confirmation available)
     * @see #sendToChannel(String, Object, Map)
     */
    public boolean sendToChannel(String channelName, Object payload) {
        checkChannelName(channelName);
        messagingTemplate.convertAndSend(channelName, payload);
        return true;
    }

    /**
     * Broadcasts a message to all channel subscribers with custom STOMP headers.
     * <p>
     * This variant allows specifying custom message headers for metadata such as priority,
     * content-type, or application-specific attributes. Headers are transmitted as part of
     * the STOMP protocol frame.
     * </p>
     *
     * @param channelName the WebSocket destination (e.g., '/queue/broadcast-channel')
     * @param payload the message object to send, will be serialized to JSON
     * @param headers STOMP header map containing message metadata (e.g., priority, content-type)
     * @return always returns true (no delivery confirmation available)
     * @see #sendToChannel(String, Object)
     */
    public boolean sendToChannel(String channelName, Object payload, Map<String, Object> headers) {
        checkChannelName(channelName);
        messagingTemplate.convertAndSend(channelName, payload, headers);
        return true;
    }

    /**
     * Sends a message to a specific user's channel by email address.
     * <p>
     * The message is delivered only to the specified user's WebSocket session.
     * Spring Security resolves the user session from the email principal.
     * User session resolution requires Spring Security authentication with email as principal.
     * </p>
     * <p>
     * <b>Note:</b> Channel names for user-targeted messages should exclude the /user prefix,
     * as it is added automatically by convertAndSendToUser.
     * </p>
     * <p>
     * Usage example:
     * <pre>{@code
     * websocketService.sendToUserChannel("user@example.com", "/queue/user-notifications", message);
     * }</pre>
     * </p>
     *
     * @param userEmail the target user's email address (Spring Security principal)
     * @param channelName the WebSocket destination (e.g., '/queue/user-notifications'), without /user prefix
     * @param payload the message object to send, will be serialized to JSON
     * @return always returns true (no delivery confirmation, returns true even if user not connected)
     * @see #sendToUserChannel(Long, String, Object)
     * @see #sendToUserChannel(User, String, Object)
     */
    public boolean sendToUserChannel(String userEmail, String channelName, Object payload) {
        checkChannelName(channelName);
        messagingTemplate.convertAndSendToUser(userEmail, channelName, payload);
        return true;
    }

    /**
     * Sends a message to a specific user's channel by user ID.
     * <p>
     * Resolves the user's email address via {@link UserRepository#findUserEmailByUserId(Long)}
     * and delegates to the email-based overload.
     * </p>
     *
     * @param userId the target user's ID
     * @param channelName the WebSocket destination (e.g., '/queue/user-notifications'), without /user prefix
     * @param payload the message object to send, will be serialized to JSON
     * @return always returns true (no delivery confirmation, returns true even if user not connected)
     * @see #sendToUserChannel(String, String, Object)
     */
    public boolean sendToUserChannel(Long userId, String channelName, Object payload) {
        checkChannelName(channelName);
        String userEmail = userRepository.findUserEmailByUserId(userId);
        sendToUserChannel(userEmail, channelName, payload);
        return true;
    }

    /**
     * Sends a message to a specific user's channel using a User entity.
     * <p>
     * Extracts the email address from {@link User#getEmail()} and delegates
     * to the email-based overload.
     * </p>
     *
     * @param user the target User entity
     * @param channelName the WebSocket destination (e.g., '/queue/user-notifications'), without /user prefix
     * @param payload the message object to send, will be serialized to JSON
     * @return always returns true (no delivery confirmation, returns true even if user not connected)
     * @see #sendToUserChannel(String, String, Object)
     */
    public boolean sendToUserChannel(User user, String channelName, Object payload) {
        checkChannelName(channelName);
        sendToUserChannel(user.getEmail(), channelName, payload);
        return true;
    }

    /**
     * Sends a message to a specific user's channel by email with custom STOMP headers.
     * <p>
     * This variant allows specifying custom message headers for metadata.
     * The message is delivered only to the specified user's WebSocket session.
     * </p>
     *
     * @param userEmail the target user's email address (Spring Security principal)
     * @param channelName the WebSocket destination (e.g., '/queue/user-notifications'), without /user prefix
     * @param payload the message object to send, will be serialized to JSON
     * @param headers STOMP header map containing message metadata
     * @return always returns true (no delivery confirmation, returns true even if user not connected)
     * @see #sendToUserChannel(String, String, Object)
     */
    public boolean sendToUserChannel(String userEmail, String channelName, Object payload, Map<String, Object> headers) {
        checkChannelName(channelName);
        messagingTemplate.convertAndSendToUser(userEmail, channelName, payload, headers);
        return true;
    }

    /**
     * Sends a message to a specific user's channel by user ID with custom STOMP headers.
     * <p>
     * Resolves the user's email address via {@link UserRepository#findUserEmailByUserId(Long)}
     * and delegates to the email-based overload with headers.
     * </p>
     *
     * @param userId the target user's ID
     * @param channelName the WebSocket destination (e.g., '/queue/user-notifications'), without /user prefix
     * @param payload the message object to send, will be serialized to JSON
     * @param headers STOMP header map containing message metadata
     * @return always returns true (no delivery confirmation, returns true even if user not connected)
     * @see #sendToUserChannel(String, String, Object, Map)
     */
    public boolean sendToUserChannel(Long userId, String channelName, Object payload, Map<String, Object> headers) {
        checkChannelName(channelName);
        String userEmail = userRepository.findUserEmailByUserId(userId);
        sendToUserChannel(userEmail, channelName, payload, headers);
        return true;
    }

    /**
     * Sends a message to a specific user's channel using a User entity with custom STOMP headers.
     * <p>
     * Extracts the email address from {@link User#getEmail()} and delegates
     * to the email-based overload with headers.
     * </p>
     *
     * @param user the target User entity
     * @param channelName the WebSocket destination (e.g., '/queue/user-notifications'), without /user prefix
     * @param payload the message object to send, will be serialized to JSON
     * @param headers STOMP header map containing message metadata
     * @return always returns true (no delivery confirmation, returns true even if user not connected)
     * @see #sendToUserChannel(String, String, Object, Map)
     */
    public boolean sendToUserChannel(User user, String channelName, Object payload, Map<String, Object> headers) {
        checkChannelName(channelName);
        sendToUserChannel(user.getEmail(), channelName, payload, headers);
        return true;
    }

}

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

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.util.Map;

import static com.openkoda.controller.common.URLConstants._HTML;

/**
 * Spring configuration class for WebSocket support and STOMP messaging protocol.
 * <p>
 * Annotated with {@link Configuration} and {@link EnableWebSocketMessageBroker} to activate 
 * WebSocket capabilities in the application. Configures a simple in-memory message broker 
 * with /queue/ prefix for pub/sub messaging. Registers WebSocket endpoint at /html/websocket 
 * with SockJS fallback for browsers without native WebSocket support.
 * </p>
 * <p>
 * This configuration is used by LiveService for real-time updates and notifications, 
 * enabling bidirectional communication between server and clients. The STOMP protocol 
 * provides a simple text-oriented messaging protocol on top of WebSocket.
 * </p>
 * <p>
 * Example client connection:
 * <pre>{@code
 * var socket = new SockJS('/html/websocket');
 * var stompClient = Stomp.over(socket);
 * stompClient.connect({}, function(frame) {
 *     stompClient.subscribe('/queue/updates', function(message) {
 *         console.log(message.body);
 *     });
 * });
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see EnableWebSocketMessageBroker
 * @see StompEndpointRegistry
 * @see MessageBrokerRegistry
 * @see com.openkoda.uicomponent.live.LiveService
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public static final String CHANNEL_PREFIX = "/queue/";

    @Value("${application.websocket.allowed-origins}")
    public String[] allowedOrigins;

    /**
     * Configures the message broker for STOMP messaging.
     * <p>
     * Enables a simple in-memory broker with /queue/ prefix for pub/sub messaging.
     * Messages published to /queue/ destinations are broadcast to all subscribed clients.
     * The simple broker is suitable for development and low-traffic applications; for 
     * production systems with higher load, consider using a full-featured message broker 
     * like RabbitMQ or ActiveMQ.
     * </p>
     *
     * @param config MessageBrokerRegistry for configuring message broker options
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(CHANNEL_PREFIX);
    }

    /**
     * Registers STOMP protocol endpoints for WebSocket connections.
     * <p>
     * Adds /html/websocket endpoint with SockJS fallback transport for browsers that 
     * do not support native WebSocket. The endpoint is configured with a custom handshake 
     * handler that extracts the HTTP session ID and makes it available as a WebSocket 
     * session attribute, enabling session continuity between HTTP and WebSocket connections.
     * </p>
     * <p>
     * Allowed origins are configured via the {@code application.websocket.allowed-origins} 
     * property. For production deployments, this should be restricted to specific trusted 
     * domains rather than allowing all origins (*).
     * </p>
     *
     * @param registry StompEndpointRegistry for WebSocket endpoint registration
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(_HTML + "/websocket").setHandshakeHandler(
                new DefaultHandshakeHandler() {

                    public boolean beforeHandshake(
                            ServerHttpRequest request,
                            ServerHttpResponse response,
                            WebSocketHandler wsHandler,
                            Map attributes) throws Exception {
                        if (request instanceof ServletServerHttpRequest) {
                            ServletServerHttpRequest servletRequest
                                    = (ServletServerHttpRequest) request;
                            HttpSession session = servletRequest
                                    .getServletRequest().getSession();
                            attributes.put("sessionId", session.getId());
                        }
                        return true;
                    }}
        ).setAllowedOrigins(allowedOrigins).withSockJS();
    }
}
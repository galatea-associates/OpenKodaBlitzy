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

package com.openkoda.uicomponent.live;


import com.openkoda.uicomponent.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Aggregator component providing a single injection point for UI service beans.
 * <p>
 * This class serves as a consolidated entry point for commonly used service beans, simplifying
 * dependency injection for downstream controllers and components. Rather than injecting multiple
 * individual service interfaces, consumers can inject this single provider to access all UI
 * service capabilities through a unified interface.
 * </p>
 * <p>
 * All fields are final and set only in the constructor, making this class immutable after
 * construction. The immutable references yield thread-safe access to the underlying service
 * implementations, which themselves must ensure thread-safety.
 * </p>
 * <p>
 * <b>Usage Note:</b> Tests should inject/mock this provider or construct it directly with test
 * doubles for the service dependencies.
 * </p>
 * <p>
 * <b>Warning:</b> Removing or renaming this class or its fields will break consumers that
 * autowire this provider. Any changes should be coordinated across the codebase.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see DataServices
 * @see IntegrationServices
 * @see MessagesServices
 * @see UtilServices
 * @see OpenAIServices
 * @see SystemServices
 * @see MediaServices
 */
@Component
public class LiveComponentProvider {

    /**
     * Provides data repository access and form management operations.
     * This service enables CRUD operations on entities and form data processing.
     */
    public final DataServices data;
    
    /**
     * Provides Slack messaging and REST client operations.
     * This service enables integration with external systems and third-party APIs.
     */
    public final IntegrationServices integrations;
    
    /**
     * Provides email and WebSocket messaging operations.
     * This service enables sending emails and real-time messages to connected clients.
     */
    public final MessagesServices messages;
    
    /**
     * Provides utility operations for UI components.
     * This service offers common utility functions used throughout the UI layer.
     */
    public final UtilServices util;
    
    /**
     * Provides ChatGPT integration operations.
     * <p>
     * This service is optional and may be null if OpenAI is not configured.
     * The {@code @Autowired(required=false)} annotation allows the application to start
     * even when OpenAI dependencies are not available.
     * </p>
     */
    @Autowired(required = false)
    public final OpenAIServices openAI;
    
    /**
     * Provides server-side code execution and system command operations.
     * <p>
     * The actual implementation is profile-dependent: {@code LiveSystemServices} in standard
     * mode or {@code SecureLiveSystemServices} in secure mode.
     * </p>
     */
    public final SystemServices system;
    
    /**
     * Provides PDF generation and file creation operations.
     * This service enables document generation and media file processing.
     */
    public final MediaServices media;

    /**
     * Creates an immutable provider with all required service dependencies via Spring autowiring.
     * <p>
     * This constructor is invoked by Spring's dependency injection framework to instantiate
     * the provider with all necessary service implementations. All fields are set once in the
     * constructor and never modified, ensuring immutability and thread-safety.
     * </p>
     *
     * @param data the DataServices implementation for repository access and form management
     * @param integrations the IntegrationServices implementation for Slack and REST operations
     * @param messages the MessagesServices implementation for email and WebSocket messaging
     * @param util the UtilServices implementation for common utility operations
     * @param system the SystemServices implementation for server-side code execution
     *               (profile-dependent: LiveSystemServices or SecureLiveSystemServices)
     * @param openAI the OpenAIServices implementation for ChatGPT integration
     *               (optional, may be null if OpenAI is not configured)
     * @param media the MediaServices implementation for PDF generation and file creation
     */
    public LiveComponentProvider(
            @Autowired DataServices data,
            @Autowired IntegrationServices integrations,
            @Autowired MessagesServices messages,
            @Autowired UtilServices util,
            @Autowired SystemServices system,
            @Autowired(required = false) OpenAIServices openAI,
            @Autowired MediaServices media) {
        this.data = data;
        this.integrations = integrations;
        this.messages = messages;
        this.util = util;
        this.system = system;
        this.openAI = openAI;
        this.media = media;
    }

}

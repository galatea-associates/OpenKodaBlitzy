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

import com.openkoda.core.helper.ApplicationContextProvider;
import com.openkoda.core.helper.NameHelper;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import org.apache.commons.lang3.StringUtils;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Immutable runtime wrapper normalizing three event consumer modalities into a unified interface.
 * <p>
 * This class provides a uniform {@code accept()} method that abstracts the underlying consumer type,
 * enabling ApplicationEventService to dispatch events without knowledge of consumer implementation details.
 * The wrapper supports three consumer modalities:
 * </p>
 * <ol>
 *   <li>Functional {@code Consumer<T>} for lambda or method reference consumers</li>
 *   <li>Functional {@code BiConsumer<T,String[]>} for parameterized consumers with static configuration</li>
 *   <li>Reflective {@code Method} invocation for persisted EventListenerEntry with Spring bean lookup</li>
 * </ol>
 * <p>
 * All fields are final after construction, making instances thread-safe for concurrent event dispatch.
 * Instances are created by EventListenerService.registerEventConsumer() and stored in the
 * ApplicationEventService listener registry.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * // Functional consumer
 * EventConsumer&lt;User&gt; consumer = new EventConsumer&lt;&gt;(user -&gt; log.info("User: {}", user));
 * 
 * // Reflective consumer
 * Method method = UserService.class.getMethod("handleUserEvent", User.class);
 * EventConsumer&lt;User&gt; reflective = new EventConsumer&lt;&gt;(method, User.class, 0, "User handler", INTEGRATION);
 * </pre>
 * </p>
 *
 * @param <T> Event payload type consumed by this wrapper
 * @see ApplicationEventService for event dispatch using wrapped consumers
 * @see EventListenerService for consumer registration and lifecycle
 * @see EventConsumerCategory for domain categorization
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class EventConsumer<T> implements LoggingComponentWithRequestId {

    /**
     * Regex pattern ',String' for counting static parameter placeholders in method signatures.
     */
    public static final String DESCRIPTION_REGEX = ",String";
    
    /**
     * Regex pattern ', String' (with space) for parameter signature parsing.
     */
    public static final String DESCRIPTION_REGEX_WITH_SPACE = ", String";
    
    /**
     * Functional Consumer&lt;T&gt; for lambda/method reference consumers; null if other modality used.
     */
    private final Consumer<T> consumer;
    
    /**
     * Functional BiConsumer&lt;T,String[]&gt; for parameterized consumers accepting event plus static
     * configuration; null if other modality used.
     */
    private final BiConsumer<T, String []> consumerWithStaticData;
    
    /**
     * Reflective Method for persisted EventListenerEntry consumers resolved via Spring context;
     * null if functional modality used.
     */
    private final Method consumerMethod;
    
    /**
     * Human-friendly consumer description for UI presentation and debugging.
     */
    private final String description;
    
    /**
     * Count of static String parameters expected by consumer method (0-4 supported).
     */
    private final int numberOfConsumerMethodParameters;
    
    /**
     * Formatted method signature without parameter types for display (e.g., 'ClassName.methodName').
     */
    private final String methodDescription;
    
    /**
     * Runtime Class of event payload type for type verification; null for functional consumers.
     */
    private final Class<T> eventClass;

    /**
     * EventConsumerCategory for domain-based filtering and routing; null if not categorized.
     */
    private final EventConsumerCategory category;

    /**
     * Creates wrapper for functional consumer without static parameters.
     * <p>
     * This constructor is suitable for simple event handlers that only require the event payload
     * and do not need additional static configuration parameters.
     * </p>
     *
     * @param consumer Functional Consumer&lt;T&gt; for lambda or method reference event handling
     */
    public EventConsumer(Consumer<T> consumer) {
        this.consumer = consumer;
        this.consumerWithStaticData = null;
        this.consumerMethod = null;
        this.description = "";
        this.eventClass = null;
        String dryMethodDescription = NameHelper.createMethodDescription(consumerMethod);
        this.methodDescription = dryMethodDescription.replaceAll(DESCRIPTION_REGEX, "").replaceAll(DESCRIPTION_REGEX_WITH_SPACE, "");
        this.numberOfConsumerMethodParameters = StringUtils.countMatches(dryMethodDescription, DESCRIPTION_REGEX_WITH_SPACE);
        this.category = null;
    }
    /**
     * Creates wrapper for reflective method consumer.
     * <p>
     * This constructor is used by EventListenerService for persisted EventListenerEntry instances.
     * The method will be invoked on a Spring-managed bean retrieved via ApplicationContext.
     * </p>
     *
     * @param method Reflective Method to invoke on Spring-managed bean for event handling
     * @param eventClass Runtime Class of event payload type for type-safe verification
     * @param numberOfConsumerMethodParameters Count of static String parameters method expects (0-4)
     */
    public EventConsumer(Method method,
                         Class<T> eventClass,
                         int numberOfConsumerMethodParameters) {
        this.consumer = null;
        this.consumerWithStaticData = null;
        this.consumerMethod = method;
        this.eventClass = eventClass;
        this.description = "";
        String dryMethodDescription = NameHelper.createMethodDescription(consumerMethod);
        this.methodDescription = dryMethodDescription.replaceAll(DESCRIPTION_REGEX, "").replaceAll(DESCRIPTION_REGEX_WITH_SPACE, "");
        this.numberOfConsumerMethodParameters = numberOfConsumerMethodParameters;
        this.category = null;
    }

    /**
     * Creates fully-configured wrapper with metadata.
     * <p>
     * This is the primary constructor for persisted EventListenerEntry instances. It includes
     * human-friendly description and category for UI presentation and domain-based filtering.
     * </p>
     *
     * @param method Reflective Method to invoke on Spring-managed bean
     * @param eventClass Runtime Class of event payload type
     * @param numberOfConsumerMethodParameters Count of static String parameters (0-4)
     * @param description Human-friendly consumer description for UI/debugging
     * @param category EventConsumerCategory for domain-based filtering
     */
    public EventConsumer(Method method,
                         Class<T> eventClass,
                         int numberOfConsumerMethodParameters,
                         String description,
                         EventConsumerCategory category) {
        this.consumer = null;
        this.consumerWithStaticData = null;
        this.consumerMethod = method;
        this.eventClass = eventClass;
        this.description = description;
        String dryMethodDescription = NameHelper.createMethodDescription(consumerMethod);
        this.methodDescription = dryMethodDescription.replaceAll(DESCRIPTION_REGEX, "").replaceAll(DESCRIPTION_REGEX_WITH_SPACE, "");
        this.numberOfConsumerMethodParameters = numberOfConsumerMethodParameters;
        this.category = category;
    }

    /**
     * Creates wrapper for parameterized functional consumer.
     * <p>
     * This constructor is suitable for event handlers that require additional static configuration
     * parameters passed alongside the event payload.
     * </p>
     *
     * @param consumerWithStaticData Functional BiConsumer accepting event and static configuration parameters
     */
    public EventConsumer(BiConsumer<T, String[]> consumerWithStaticData) {
        this.consumerWithStaticData = consumerWithStaticData;
        this.consumer = null;
        this.consumerMethod = null;
        this.description = "";
        this.eventClass = null;
        String dryMethodDescription = NameHelper.createMethodDescription(consumerMethod);
        this.methodDescription = dryMethodDescription.replaceAll(DESCRIPTION_REGEX_WITH_SPACE, "");
        this.numberOfConsumerMethodParameters = StringUtils.countMatches(dryMethodDescription, DESCRIPTION_REGEX_WITH_SPACE);
        this.category = null;
    }

    /**
     * Dispatches event to the underlying consumer implementation.
     * <p>
     * This method abstracts the consumer type by attempting dispatch in order: functional consumer,
     * parameterized consumer with static data, then reflective method via Spring bean lookup.
     * Exceptions during reflective invocation are caught and logged with request correlation,
     * and the method returns true even on failure to prevent event dispatch interruption.
     * </p>
     * <p>
     * Thread-safe for concurrent invocation from ApplicationEventService executor.
     * </p>
     *
     * @param eventObject Event payload to consume
     * @param staticParameter Variable-length static configuration parameters (0-4 String values supported)
     * @return Always returns true; exceptions logged but not propagated
     */
    public boolean accept(T eventObject, String ... staticParameter) {
        debug("[accept] eventObject: {} parameters: {}", eventObject, staticParameter);
        if (consumer != null) {
            consumer.accept(eventObject);
            return true;
        }
        if (consumerWithStaticData != null) {
            consumerWithStaticData.accept(eventObject, staticParameter);
            return true;
        }
        if (consumerMethod != null) {
            try {
                Object consumerObj = ApplicationContextProvider.getContext().getBean(consumerMethod.getDeclaringClass());
                if (staticParameter != null) {
                    invokeConsumerMethod(consumerMethod, consumerObj, eventObject, staticParameter);
                } else {
                    consumerMethod.invoke(consumerObj, eventObject);
                }
            } catch (IllegalAccessException e) {
                error(e, "Could not invoke consumer method {}:{} due to {}",
                        consumerMethod.getDeclaringClass().getName(), consumerMethod.getName(), e.getMessage());
            } catch (InvocationTargetException e) {
                error(e, "Could not invoke consumer method {}:{} due to {}",
                        consumerMethod.getDeclaringClass().getName(), consumerMethod.getName(), e.getTargetException().getStackTrace()[0].toString());
            }
        }
        return true;
    }

    /**
     * Invokes consumer method with event plus 1-4 static parameters.
     * <p>
     * This method supports up to 4 String parameters via overload pattern, dispatching to the
     * appropriate method signature based on the staticParameter array length.
     * </p>
     *
     * @param consumerMethod Method to invoke reflectively
     * @param consumerObj Spring bean instance owning the method
     * @param eventObject Event payload to pass as first parameter
     * @param staticParameter Static configuration parameters (1-4 String values)
     * @throws InvocationTargetException If consumer method throws exception
     * @throws IllegalAccessException If method not accessible
     */
    private void invokeConsumerMethod(Method consumerMethod, Object consumerObj, T eventObject, String[] staticParameter) throws InvocationTargetException, IllegalAccessException {
        debug("[invokeConsumerMethod] method: {} consumer: {} event: {} parameters: {}", consumerMethod, consumerObj, eventObject, staticParameter);
        if(staticParameter.length == 1){
            this.consumerMethod.invoke(consumerObj, eventObject, staticParameter[0]);
        }
        if(staticParameter.length == 2){
            this.consumerMethod.invoke(consumerObj, eventObject, staticParameter[0], staticParameter[1]);
        }
        if(staticParameter.length == 3){
            this.consumerMethod.invoke(consumerObj, eventObject, staticParameter[0], staticParameter[1], staticParameter[2]);
        }
        if(staticParameter.length == 4){
            this.consumerMethod.invoke(consumerObj, eventObject, staticParameter[0], staticParameter[1], staticParameter[2], staticParameter[3]);
        }
    }

    /**
     * Returns the reflective Method if this is a reflective consumer.
     *
     * @return Reflective Method if reflective consumer, null for functional consumers
     */
    public Method getConsumerMethod() {
        return consumerMethod;
    }
    /**
     * Returns the human-friendly consumer description.
     *
     * @return Human-friendly consumer description for UI presentation
     */
    public String getDescription() {
        return description;
    }

    /**
     * Validates that this consumer can handle specified method signature and event type.
     * <p>
     * This method is used by EventListenerService for consumer lookup, verifying that the
     * consumer's method matches the class, method name, compatible event type, and parameter count.
     * Event type compatibility is checked via assignability rather than exact match.
     * </p>
     *
     * @param className Fully-qualified class name to match against consumerMethod.getDeclaringClass()
     * @param methodName Method name to match
     * @param eventObjectClass Event payload Class to verify assignability
     * @param numberOfStaticMethodParameters Parameter count to match
     * @return true if consumerMethod matches all criteria (class, method, compatible event type, parameter count); false otherwise
     */
    public boolean verifyMethod(String className, String methodName, Class eventObjectClass, int numberOfStaticMethodParameters) {
        debug("[verifyMethod] class: {} method: {} event: {} numOfParams: {}", className, methodName, eventObjectClass, numberOfStaticMethodParameters);
        return this.consumerMethod != null
                && this.consumerMethod.getDeclaringClass().getName().equals(className)
                && this.consumerMethod.getName().equals(methodName)
                && this.eventClass.isAssignableFrom(eventObjectClass)
                && this.numberOfConsumerMethodParameters == numberOfStaticMethodParameters;
    }

    /**
     * Serializes consumer properties to Tuple4 for persistence or transmission.
     *
     * @return Tuple4 containing (category name, methodDescription, numberOfConsumerMethodParameters, description) for serialization
     */
    public Tuple4<String, String, Integer, String> propertiesToTuple() {
        return Tuples.of(this.category != null ? this.category.name() : "", this.methodDescription, this.numberOfConsumerMethodParameters, this.description != null ? this.description : "");
    }

    /**
     * Serializes consumer properties to Map for JSON/configuration export.
     *
     * @return Map with keys 'c' (category), 'm' (method), 'p' (parameters), 'd' (description), 'v' (verbose display)
     */
    public Map<String, String> propertiesToMap() {
        return Map.of(
                "c", this.category != null ? this.category.name() : "",
                "m", this.methodDescription,
                "p", String.valueOf(this.numberOfConsumerMethodParameters),
                "d", this.description != null ? this.description : "",
                "v", this.methodDescription + "(" + this.description + ")");
    }

    /**
     * Returns the EventConsumerCategory for domain-based filtering.
     *
     * @return EventConsumerCategory for domain-based filtering, or null if not categorized
     */
    public EventConsumerCategory getCategory() {
        return category;
    }
}

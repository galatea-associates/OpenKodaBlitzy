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

package com.openkoda.controller;

import com.openkoda.controller.common.PageAttributes;
import com.openkoda.core.flow.TransactionalExecutor;
import com.openkoda.core.helper.Messages;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.repository.Repositories;
import com.openkoda.service.Services;
import jakarta.inject.Inject;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Abstract base class aggregating core platform components (Services, Repositories, Controllers, Messages)
 * for convenient dependency injection.
 * <p>
 * Provides single-injection pattern for accessing platform beans. Aggregates Services, Repositories,
 * Controllers, and Messages singletons. Implements InitializingBean to register service and repository
 * beans in static resources map after Spring initialization. Subclassed by DefaultComponentProvider as
 * concrete {@code @Component} for Spring DI. Used throughout controllers to access platform services
 * without multiple {@code @Autowired} fields.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * public class MyController extends ComponentProvider {
 *     public void doSomething() {
 *         User user = services.user.findById(userId);
 *         Organization org = repositories.secure.organization.findOne(orgId);
 *     }
 * }
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see DefaultComponentProvider
 * @see Services
 * @see Repositories
 */
public class ComponentProvider implements PageAttributes, LoggingComponentWithRequestId, InitializingBean {

    /**
     * Autowired Services aggregator providing access to all platform service beans.
     *
     * @see Services
     */
    @Inject
    public Services services;

    /**
     * Autowired Repositories aggregator providing access to all repository beans.
     *
     * @see Repositories
     */
    @Inject
    public Repositories repositories;

    /**
     * Autowired Controllers aggregator for inter-controller communication.
     *
     * @see Controllers
     */
    @Inject
    public Controllers controllers;

    /**
     * Autowired Messages bean for internationalized message retrieval.
     */
    @Inject
    public Messages messages;

    protected Supplier<TransactionalExecutor> transactional = () -> services.transactionalExecutor;

    /**
     * Static map of platform resources (services, repositories) registered after bean initialization.
     * <p>
     * Populated once during startup, read-only afterward. Safe for concurrent access.
     * </p>
     */
    public final static Map<String, Object> resources = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Spring lifecycle callback that registers service and repository beans in static resources map.
     * <p>
     * Invoked after all properties set. Iterates Services fields, registers each service bean in
     * resources map. Repeats for Repositories. Enables reflection-based service and repository discovery.
     * </p>
     * <p>
     * Note: Executes once per application startup.
     * </p>
     *
     * @throws Exception if resource registration fails
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if(!initialized) {
            initialized = true;
            resources.put("services", services);
            resources.put("repositories", repositories);
        }
    }
}

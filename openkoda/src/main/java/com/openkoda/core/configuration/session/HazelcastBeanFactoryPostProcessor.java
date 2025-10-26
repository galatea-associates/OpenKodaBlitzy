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

package com.openkoda.core.configuration.session;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Spring BeanFactoryPostProcessor for registering custom Hazelcast-aware session scope.
 * <p>
 * Implements postProcessBeanFactory() to programmatically register OAuthClientContextHazelcastSessionScope
 * with scope name SESSION_HAZELCAST_AWARE. This processor enables the application to define session-scoped
 * beans that leverage Hazelcast for distributed session replication in clustered environments.
 * </p>
 * <p>
 * Note: Registration line is currently commented out, making processor inert. If enabled, must run before
 * beans referencing custom scope are instantiated. Spring invokes BeanFactoryPostProcessor implementations
 * early in the container lifecycle (after bean definitions loaded, before bean instantiation).
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see BeanFactoryPostProcessor
 * @see com.openkoda.core.security.oauth.OAuthClientContextHazelcastSessionScope
 */
public class HazelcastBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    public HazelcastBeanFactoryPostProcessor() {
    }

    /**
     * Intended to register custom scope but currently disabled via commented code.
     * <p>
     * Would call factory.registerScope() to add Hazelcast-aware session scope, enabling Spring beans
     * to use the custom scope annotation. When enabled, registers OAuthClientContextHazelcastSessionScope
     * instance with scope name SESSION_HAZELCAST_AWARE for distributed session management.
     * </p>
     *
     * @param factory the ConfigurableListableBeanFactory for scope registration
     * @throws BeansException if scope registration fails (when enabled)
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) throws BeansException {
//        factory.registerScope(OAuthClientContextHazelcastSessionScope.SESSION_HAZELCAST_AWARE, new OAuthClientContextHazelcastSessionScope());
    }
}



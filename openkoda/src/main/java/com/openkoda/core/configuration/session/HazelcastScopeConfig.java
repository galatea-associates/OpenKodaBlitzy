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

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class that registers HazelcastBeanFactoryPostProcessor bean.
 * <p>
 * Annotated with {@code @Configuration} to enable bean registration. Exposes
 * {@code beanFactoryPostProcessor()} factory method returning HazelcastBeanFactoryPostProcessor
 * instance. Spring invokes BeanFactoryPostProcessor in correct lifecycle phase (after definition
 * loading, before instantiation).
 * 
 * <p>
 * This configuration enables custom Hazelcast-based session scopes within the Spring application
 * context, allowing beans to be scoped to distributed Hazelcast sessions.
 * 
 *
 * @see HazelcastBeanFactoryPostProcessor
 * @since 1.7.1
 * @author OpenKoda Team
 * @version 1.7.1
 */
@Configuration
public class HazelcastScopeConfig {

    /**
     * Creates HazelcastBeanFactoryPostProcessor bean for custom scope registration.
     * <p>
     * Returns post-processor that will be invoked by Spring during bean factory initialization.
     * The processor registers Hazelcast-based session scopes in the Spring application context,
     * enabling distributed session management across clustered environments.
     * 
     *
     * @return BeanFactoryPostProcessor for Hazelcast session scope registration
     */
    @Bean
    public static BeanFactoryPostProcessor beanFactoryPostProcessor() {
        return new HazelcastBeanFactoryPostProcessor();
    }

}
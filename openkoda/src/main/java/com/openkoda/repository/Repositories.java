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

package com.openkoda.repository;

import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

/**
 * Top-level repository aggregator exposing secure and unsecure repository groups for simplified dependency injection.
 * <p>
 * This class is annotated with {@code @Component("AllRepositories")} for Spring bean registration and provides
 * a legacy dependency injection pattern where a single bean injection point gives access to all repository instances
 * instead of injecting individual repository beans. It exposes two public injected fields: {@code secure} for
 * privilege-enforced operations and {@code unsecure} for direct database access bypassing privilege checks.

 * <p>
 * This aggregator is used throughout controllers and services that need access to multiple repositories. Modern code
 * should prefer injecting specific repository interfaces directly instead of using this aggregator, as it provides
 * better type safety and clearer dependency declarations. However, removing this class would break many injection
 * sites in legacy code paths that rely on the convenience of dot-notation access pattern.

 * <p>
 * Example usage:
 * <pre>{@code
 * @Autowired Repositories repositories;
 * repositories.secure.organization.findOne(id);
 * }</pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SecureRepositories
 * @see UnsecureRepositories
 */
@Component("AllRepositories")
public class Repositories {
    // Original author: Arkadiusz Drysch (adrysch@stratoflow.com)

    /**
     * Aggregator of SecureRepository instances with privilege enforcement for all operations.
     * <p>
     * Use this accessor for user-facing operations that require access control validation. All repository
     * methods accessed through this field will enforce privilege checks based on the current security context.

     */
    @Inject public SecureRepositories secure;

    /**
     * Aggregator of unsecured repository instances bypassing privilege checks.
     * <p>
     * Use this accessor only for system-level operations and internal processing where access has already been
     * validated or privilege enforcement is not required. Direct database access without security checks.

     */
    @Inject public UnsecureRepositories unsecure;

}

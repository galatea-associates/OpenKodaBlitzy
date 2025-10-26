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

package com.openkoda.core.customisation;

import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * Singleton registry bean for {@link FrontendMapping} lookups by unique mapping names.
 * <p>
 * This class extends {@link HashMap}{@code <String, FrontendMapping>} to store mappings from
 * unique mapping names (as defined in {@code FrontendMappingDefinition.name}) to their
 * corresponding {@link FrontendMapping} instances. Each FrontendMapping bundles a form definition
 * with its associated repository for data access operations.
 * </p>
 * <p>
 * The {@link Component @Component} annotation makes this class an injectable Spring singleton,
 * ensuring a single shared registry instance across the application. Controllers and services
 * retrieve registered mappings via Spring dependency injection and map lookups.
 * </p>
 * <p>
 * <strong>Thread-Safety Considerations:</strong><br>
 * This class inherits mutability from {@link HashMap} and is <strong>not thread-safe</strong>
 * by default. All mutation operations (put, remove) must be synchronized externally.
 * {@link BasicCustomisationService} uses the {@code synchronized} keyword when modifying this
 * map to ensure thread-safe registration during application startup and runtime customization.
 * </p>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * @Autowired
 * private FrontendMappingMap mappingMap;
 *
 * FrontendMapping mapping = mappingMap.get("userForm");
 * }</pre>
 * </p>
 *
 * @see FrontendMapping
 * @see CustomisationService#registerFrontendMapping(String, FrontendMapping)
 * @see CustomisationService#unregisterFrontendMapping(String)
 * @see BasicCustomisationService
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Component
public class FrontendMappingMap extends HashMap<String, FrontendMapping> {
}
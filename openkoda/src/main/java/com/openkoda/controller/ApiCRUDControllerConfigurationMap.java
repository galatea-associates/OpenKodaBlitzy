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

import org.springframework.stereotype.Component;

/**
 * Concrete implementation of CRUD controller configuration registry for REST API endpoints.
 * <p>
 * Spring {@code @Component} extending {@link AbstractCRUDControllerConfigurationMap}.
 * Stores configurations for API-based generic CRUD controllers (CRUDApiController).
 * Configurations are registered at application startup via CRUDControllers.
 * Used by API controllers to retrieve entity-specific settings including repository,
 * form class, and required privileges for JSON REST endpoints.
 * 
 * <p>
 * Thread-safety: Extends HashMap with no additional synchronization. Assumes
 * single-threaded startup registration during {@code @PostConstruct} phase.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * See {@code AbstractCRUDControllerConfigurationMap}
 * See {@code CRUDApiController}
 */
@Component
public class ApiCRUDControllerConfigurationMap extends AbstractCRUDControllerConfigurationMap{
}

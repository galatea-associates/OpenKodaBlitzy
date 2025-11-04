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

/**
 * Common controller utilities for the integration module including page attributes, URL constants, and shared controller components.
 * <p>
 * This package provides centralized routing keys and model binding descriptors used by IntegrationControllerHtml
 * and Thymeleaf views. It contains interface-based constant definitions that establish consistent naming conventions
 * for integration-related endpoints and page model attributes.
 * 
 * <p>
 * Key classes include IntegrationPageAttributes for typed model descriptors used in integration form binding,
 * and IntegrationURLConstants for URL path constants that define integration routing patterns.
 * These artifacts ensure type-safe access to model attributes and URL paths across the integration controller layer.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.integration.controller
 * @see com.openkoda.integration.controller.IntegrationControllerHtml
 * @see com.openkoda.integration.controller.common.IntegrationPageAttributes
 * @see com.openkoda.integration.controller.common.IntegrationURLConstants
 */
package com.openkoda.integration.controller.common;
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

import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

/**
 * Minimal Spring Session web initializer that triggers automatic servlet filter and listener registration.
 * <p>
 * This class extends {@link AbstractHttpSessionApplicationInitializer} with an empty body. The presence of this
 * class on the classpath activates Spring Session automatic configuration hooks through the Servlet 3.0+
 * WebApplicationInitializer SPI. The parent class handles all initialization logic including registration of the
 * springSessionRepositoryFilter and the SessionEventHttpSessionListenerAdapter.
 * 
 * <p>
 * Removing this class disables Spring Session and breaks distributed session behavior in clustered deployments.
 * No custom logic is required as the parent class handles all initialization through the onStartup() method.
 * 
 * <p>
 * <strong>Important:</strong> Required for Spring Session WebApplicationInitializer SPI detection. Must be present
 * for distributed session support to function correctly.
 * 
 *
 * @since 1.7.1
 * @author OpenKoda Team
 * @version 1.7.1
 * @see AbstractHttpSessionApplicationInitializer
 */
public class Initializer extends AbstractHttpSessionApplicationInitializer {
}
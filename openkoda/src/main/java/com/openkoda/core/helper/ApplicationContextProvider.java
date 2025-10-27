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

package com.openkoda.core.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Helper class that stores the Spring {@link ApplicationContext} in a static field for global access outside
 * standard dependency injection.
 * <p>
 * This component enables bean lookup in non-managed classes or static contexts where constructor or field injection
 * is not available. The ApplicationContext is stored during Spring initialization via the {@link #context(ApplicationContext)}
 * setter method.
 * </p>
 * <p>
 * <b>Important Warnings:</b>
 * </p>
 * <ul>
 *   <li>{@link #getContext()} can return null before Spring initialization completes</li>
 *   <li>Using this pattern introduces lifecycle coupling and potential memory leak risks if misused</li>
 *   <li>Prefer standard dependency injection (constructor or field injection) when possible</li>
 * </ul>
 * <p>
 * <b>Thread Safety:</b> The static field is set once during Spring startup. Read access is safe after initialization.
 * Write operations are not synchronized as they occur only during Spring context setup.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * MyBean bean = ApplicationContextProvider.getContext().getBean(MyBean.class);
 * }</pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.springframework.context.ApplicationContextAware
 */
@Component("applicationContextProvider")
public class ApplicationContextProvider {

   private static ApplicationContext context;

   /**
    * Setter method for the ApplicationContext, invoked automatically by Spring during initialization.
    * <p>
    * This method stores the provided ApplicationContext in a static field for global access. Spring calls this method
    * via the {@code @Autowired} annotation when the bean is created.
    * </p>
    *
    * @param context the ApplicationContext to store for later retrieval
    */
   @Autowired
   public void context(ApplicationContext context) { this.context = context; }

   /**
    * Returns the stored ApplicationContext for bean lookup in non-managed classes.
    * <p>
    * This static method provides access to the Spring ApplicationContext from any code location, including classes
    * not managed by Spring. This is useful for accessing beans in static utility methods, legacy code, or third-party
    * library integrations where dependency injection is not available.
    * </p>
    * <p>
    * <b>Warning:</b> Returns {@code null} if called before Spring context initialization completes. Ensure the
    * application has fully started before invoking this method.
    * </p>
    *
    * @return the stored ApplicationContext, or {@code null} if not yet initialized by Spring
    */
   public static ApplicationContext getContext() {
      return context;
   }

}

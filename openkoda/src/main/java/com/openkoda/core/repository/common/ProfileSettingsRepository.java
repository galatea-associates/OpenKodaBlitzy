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

package com.openkoda.core.repository.common;

/**
 * Generic repository contract for decoupling UI settings forms from persistence.
 * <p>
 * This interface provides a standard contract for retrieving user-specific settings entities
 * that are displayed and edited on custom settings pages. It allows the settings customization
 * framework to remain agnostic of specific entity types while enabling type-safe form bindings.
 * The repository typically returns an entity with a 1-to-1 relationship to the User entity.
 * 
 * <p>
 * Implementations of this interface are registered with the customization framework via
 * {@link com.openkoda.core.customisation.BasicCustomisationService#registerSettingsForm}.
 * The framework invokes {@link #findOneForUserId(Long)} to populate form data and bind
 * user-specific settings to the settings page UI.
 * 
 * <p>
 * Implementation responsibilities:
 * 
 * <ul>
 *   <li>Define null-handling semantics (return null vs. throw exception for missing settings)</li>
 *   <li>Manage transaction boundaries for settings retrieval</li>
 *   <li>Ensure proper user ID validation and authorization checks if needed</li>
 * </ul>
 *
 * @param <T> the type of settings entity associated with a user
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @since 1.7.1
 * @see com.openkoda.core.customisation.BasicCustomisationService
 */
public interface ProfileSettingsRepository<T> {

    /**
     * Retrieves the settings entity associated with the specified user ID.
     * <p>
     * This method performs a lookup to find the user-specific settings entity that is displayed
     * and edited on the settings page. The returned entity is typically bound to a form and
     * populated with the user's current settings values.
     * 
     * <p>
     * Note: Null-handling semantics are implementation-dependent. Some implementations may return
     * {@code null} if no settings exist for the given user, while others may throw an exception
     * or create default settings on-demand.
     * 
     *
     * @param id the user ID to find settings for
     * @return the settings entity associated with the user, or {@code null} if no settings exist
     *         for the given user ID (depending on implementation)
     */
    T findOneForUserId(Long id);

}

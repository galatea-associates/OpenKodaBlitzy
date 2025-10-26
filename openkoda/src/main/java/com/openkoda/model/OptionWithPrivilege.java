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

package com.openkoda.model;

/**
 * Interface for dropdown options and enums that require privilege-based visibility control.
 * <p>
 * Extends OptionWithLabel to add privilege checking capability for UI component rendering. 
 * Options implementing this interface can be conditionally displayed based on the current 
 * user's privileges, enabling fine-grained access control in selection lists, dropdown menus, 
 * and form fields. The privilege check is performed by UI layer components before rendering 
 * the option.
 * </p>
 * <p>
 * Common implementations include Role enums, dynamic entity type selectors, and administrative 
 * configuration options where visibility depends on user permissions.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see OptionWithLabel
 * @see PrivilegeBase
 * @see Privilege
 */
public interface OptionWithPrivilege extends OptionWithLabel {

    /**
     * Returns the privilege required to view and select this option.
     * <p>
     * UI components check this privilege against the user's granted privileges before 
     * including the option in selection lists.
     * </p>
     *
     * @return the privilege that must be granted to the current user for this option to be 
     *         visible, may be null for publicly visible options
     */
    PrivilegeBase getPrivilege();

}

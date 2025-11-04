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
 * Marker interface for entities and domain objects that provide a human-readable label for UI display and selection lists.
 * <p>
 * Implemented by entities and value objects that need to be presented in dropdown menus, selection widgets, and user 
 * interface components. The label typically represents a display name, title, or user-friendly identifier. This interface 
 * enables generic UI component rendering without coupling to specific entity types.
 * 
 * <p>
 * Common implementations include entities like Organization, Role, User, and various configuration objects.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see OptionWithPrivilege for option types with privilege requirements
 */
public interface OptionWithLabel {

    /**
     * Returns the human-readable label for this option.
     * <p>
     * Used by UI components for dropdown display text and form field rendering.
     * 
     *
     * @return the display label, typically non-null but implementation-specific
     */
    String getLabel();
}

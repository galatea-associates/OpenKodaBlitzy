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

package com.openkoda.core.service.module;

/**
 * Type-safe enumeration defining UI insertion points for dynamic content in the OpenKoda platform.
 * <p>
 * This enum provides a canonical vocabulary for positioning modules and dynamic content within
 * page layouts. Each constant represents a specific region where content can be injected during
 * page rendering. The seven predefined areas follow vertical page layout flow from top to bottom:
 * navigation bar profile section, page body top/bottom, main content top/bottom, and sidebar
 * widget areas.
 * </p>
 * <p>
 * The {@link #STANDARD_AREAS} constant provides an ordered canonical array of all areas for
 * convenient iteration during module registration or content rendering.
 * </p>
 * <p>
 * Example usage in module registration:
 * <pre>{@code
 * Module module = new Module("customModule");
 * module.registerContent(ApplicationArea.SIDEBAR_TOP, widgetRenderer);
 * }</pre>
 * </p>
 * <p>
 * <b>Thread Safety:</b> Enum constants are inherently thread-safe and immutable.
 * </p>
 * <p>
 * <b>API Stability:</b> Changes to enum constant names or removal of constants are breaking
 * API changes that will affect existing modules and should be avoided.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @see Module
 * @since 1.7.1
 */
public enum ApplicationArea {

    /**
     * Injection point at the top of the main content area, above primary page content.
     * <p>
     * Typical use: Breadcrumbs, page-level alerts, or content-specific navigation.
     * </p>
     */
    CONTENT_TOP,
    
    /**
     * Injection point at the bottom of the main content area, below primary page content.
     * <p>
     * Typical use: Related content links, pagination controls, or content footers.
     * </p>
     */
    CONTENT_BOTTOM,
    
    /**
     * Injection point at the top of the page body, before the main content container.
     * <p>
     * Typical use: Site-wide banners, announcement bars, or global notifications.
     * </p>
     */
    BODY_TOP,
    
    /**
     * Injection point at the bottom of the page body, after the main content container.
     * <p>
     * Typical use: Footer widgets, analytics scripts, or page-level disclaimers.
     * </p>
     */
    BODY_BOTTOM,
    
    /**
     * Injection point at the top of the sidebar region for widget placement.
     * <p>
     * Typical use: Primary sidebar widgets, search boxes, or quick action panels.
     * </p>
     */
    SIDEBAR_TOP,
    
    /**
     * Injection point at the bottom of the sidebar region for widget placement.
     * <p>
     * Typical use: Secondary sidebar widgets, advertisements, or supplementary information.
     * </p>
     */
    SIDEBAR_BOTTOM,
    
    /**
     * Injection point in the navigation bar profile section for user menu extensions.
     * <p>
     * Typical use: User profile menu items, notification badges, or quick settings.
     * </p>
     */
    NAVBAR_PROFILE;

    /**
     * Ordered canonical array containing all seven ApplicationArea enum constants.
     * <p>
     * The ordering follows vertical page layout flow from top to bottom, which is useful
     * when iterating through areas during module registration or content rendering. The
     * sequence is: {@link #CONTENT_TOP}, {@link #CONTENT_BOTTOM}, {@link #BODY_TOP},
     * {@link #BODY_BOTTOM}, {@link #SIDEBAR_TOP}, {@link #SIDEBAR_BOTTOM}, {@link #NAVBAR_PROFILE}.
     * </p>
     * <p>
     * This array reference is immutable, and the enum constants it contains are effectively
     * immutable, making this constant safe for concurrent access without synchronization.
     * </p>
     * <p>
     * Example usage for registering content across all areas:
     * <pre>{@code
     * for (ApplicationArea area : ApplicationArea.STANDARD_AREAS) {
     *     module.registerContent(area, contentProvider);
     * }
     * }</pre>
     * </p>
     */
    public static final ApplicationArea[] STANDARD_AREAS = {CONTENT_TOP, CONTENT_BOTTOM, BODY_TOP, BODY_BOTTOM, SIDEBAR_TOP, SIDEBAR_BOTTOM, NAVBAR_PROFILE};

}

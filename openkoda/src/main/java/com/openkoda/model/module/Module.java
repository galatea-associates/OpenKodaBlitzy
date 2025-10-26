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

package com.openkoda.model.module;

import jakarta.validation.constraints.NotNull;

/**
 * Standardized contract for application extension modules in OpenKoda's plugin architecture.
 * <p>
 * This interface provides a consistent way to extend OpenKoda with custom functionality through
 * a modular plugin system. Each module has a unique identity (name and label), ordering control,
 * and view resolution capabilities for template-based UI integration.
 * </p>
 * <p>
 * Modular architecture benefits include plugin-style extensibility, feature isolation, and
 * independent deployment of application components. Modules can be registered dynamically
 * and integrated into the application without modifying core code.
 * </p>
 * <p>
 * Module Identity:
 * </p>
 * <ul>
 * <li>{@link #getName()} - Stable programmatic identifier for routing and configuration</li>
 * <li>{@link #getLabel()} - Human-readable display name for UI presentation</li>
 * <li>{@link #getOrdinal()} - Integer ordering hint for initialization and display sequence</li>
 * </ul>
 * <p>
 * View Resolution:
 * </p>
 * <ul>
 * <li>{@link #getModuleView(String)} - Composes module-scoped view template paths</li>
 * <li>{@link #getMainViewName()} - Returns default main view identifier</li>
 * </ul>
 * <p>
 * The static {@link #empty} sentinel instance provides a safe non-null fallback for registries
 * and null-tolerant code paths, returning empty strings and zero ordinal.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * Module myModule = () -> "My Custom Module";
 * String viewPath = myModule.getModuleView("dashboard");
 * }</pre>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.OpenkodaModule
 */
public interface Module {

    /**
     * Returns the human-readable display label for UI presentation.
     * <p>
     * The label is used in admin interfaces, module listings, and configuration screens where
     * end users select or view modules. This value should be localized and formatted for
     * display purposes.
     * </p>
     * <p>
     * The label appears in module selection dropdowns, configuration panels, and administrative
     * dashboards. Unlike {@link #getName()}, this value is intended for human consumption
     * and may contain spaces, special characters, or localized text.
     * </p>
     * <p>
     * Jakarta Validation {@code @NotNull} annotation enforces that implementations must
     * return a non-null value when a validation provider is present on the classpath.
     * </p>
     *
     * @return the human-readable module display label, never null
     * @see #getName()
     */
    @NotNull
    String getLabel();

    /**
     * Returns the stable programmatic identifier for module routing and configuration.
     * <p>
     * The name serves as a unique key for module registration, view resolution, and configuration
     * property lookup. This identifier must be stable across application restarts and should
     * follow naming conventions for URL paths and property keys.
     * </p>
     * <p>
     * The name is used in {@link #getModuleView(String)} to compose view template paths in the
     * format {@code "module/%s/%s"}. It also serves as a key in module registries and configuration
     * maps throughout the application.
     * </p>
     * <p>
     * This value must be unique across all registered modules in the application. Typical values
     * use lowercase with hyphens or underscores, such as "user-management" or "reporting_engine".
     * </p>
     * <p>
     * Jakarta Validation {@code @NotNull} annotation enforces that implementations must
     * return a non-null value when a validation provider is present on the classpath.
     * </p>
     *
     * @return the stable programmatic module identifier, never null
     * @see #getLabel()
     * @see #getModuleView(String)
     */
    @NotNull
    String getName();

    /**
     * Returns the integer ordering hint for module initialization sequence and display ordering.
     * <p>
     * The ordinal value controls the relative order in which modules are initialized at application
     * startup and displayed in user interfaces. Lower ordinal values indicate higher priority,
     * with modules having lower values being initialized first.
     * </p>
     * <p>
     * Module initialization order is important when modules have dependencies on each other or
     * need to register components in a specific sequence. Display ordering affects how modules
     * appear in navigation menus, selection dropdowns, and administrative listings.
     * </p>
     * <p>
     * The default ordinal value is typically 0. Modules that need to initialize early should
     * return negative values, while modules that can initialize later may return positive values.
     * </p>
     *
     * @return the integer ordering hint, default 0
     */
    int getOrdinal();

    /**
     * Composes module-scoped view template paths using convention-based path formatting.
     * <p>
     * This default implementation combines the module name and view name to produce template
     * paths in the format {@code "module/%s/%s"}, where the first placeholder is populated
     * with {@link #getName()} and the second with the provided view name.
     * </p>
     * <p>
     * The generated path is used by template engines (such as Thymeleaf) to resolve view
     * templates within the module's directory structure. For example, if {@code getName()}
     * returns "reporting" and viewName is "dashboard", the result is "module/reporting/dashboard".
     * </p>
     * <p>
     * Modules can override this method to customize path composition patterns, such as adding
     * versioning, internationalization, or alternative directory structures.
     * </p>
     * <p>
     * Example usage:
     * </p>
     * <pre>{@code
     * String path = module.getModuleView("settings");
     * }</pre>
     *
     * @param viewName the specific view name within the module, must not be null
     * @return the composed view template path in format "module/%s/%s"
     * @see #getName()
     * @see #getMainViewName()
     */
    default String getModuleView(String viewName) {
        return String.format("module/%s/%s", getName(), viewName);
    }

    /**
     * Returns the default main view name for the module landing page.
     * <p>
     * This default implementation returns "main" as the conventional identifier for a module's
     * primary or landing view. The main view name is typically used when navigating to the
     * module without specifying a particular subview.
     * </p>
     * <p>
     * The returned value is combined with {@link #getName()} in {@link #getModuleView(String)}
     * to resolve the full path to the module's main template. For example, if {@code getName()}
     * returns "admin", the main view path becomes "module/admin/main".
     * </p>
     * <p>
     * Modules can override this method to use alternative main view identifiers such as "index",
     * "home", or "dashboard" based on their specific conventions.
     * </p>
     * <p>
     * Jakarta Validation {@code @NotNull} annotation enforces that implementations must
     * return a non-null value when a validation provider is present on the classpath.
     * </p>
     *
     * @return the default main view name, defaults to "main", never null
     * @see #getModuleView(String)
     * @see #getName()
     */
    @NotNull
    default String getMainViewName() {
        return "main";
    }


    /**
     * Static sentinel Module instance providing safe non-null fallback for registries and null-tolerant code paths.
     * <p>
     * This shared instance implements all required Module methods with safe default values:
     * </p>
     * <ul>
     * <li>{@link #getLabel()} returns empty string ""</li>
     * <li>{@link #getName()} returns empty string ""</li>
     * <li>{@link #getOrdinal()} returns 0</li>
     * </ul>
     * <p>
     * The empty module serves as a null object pattern implementation, allowing code to avoid
     * null checks when a valid module reference is not available. This is particularly useful
     * in registries, optional configurations, and test scenarios where a placeholder module
     * is needed.
     * </p>
     * <p>
     * This instance is thread-safe and can be safely shared across multiple threads. All methods
     * return immutable values and have no side effects.
     * </p>
     * <p>
     * Example usage:
     * </p>
     * <pre>{@code
     * Module module = registry.get("unknown").orElse(Module.empty);
     * }</pre>
     *
     * @see #getLabel()
     * @see #getName()
     * @see #getOrdinal()
     */
    static Module empty = new Module() {
        @Override
        public String getLabel() {
            return "";
        }

        @Override
        public String getName() {
            return "";
        }

        @Override
        public int getOrdinal() {
            return 0;
        }
    };

}

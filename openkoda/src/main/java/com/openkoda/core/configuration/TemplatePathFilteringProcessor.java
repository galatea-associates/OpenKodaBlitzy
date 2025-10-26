package com.openkoda.core.configuration;

import static com.openkoda.controller.common.URLConstants.FRONTENDRESOURCEREGEX;
import static com.openkoda.service.export.FolderPathConstants.FRONTEND_RESOURCE_;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.openkoda.controller.common.URLConstants;
import com.openkoda.model.component.FrontendResource;

/**
 * Spring component that parses and filters frontend template paths to extract metadata.
 * <p>
 * Processes Thymeleaf template paths to identify access level restrictions (organization/global),
 * email template flags, and normalized resource names. Used by template resolution infrastructure
 * to determine proper template location and security context.
 * </p>
 * <p>
 * Analyzes template paths matching pattern 'frontendresource/{accessLevel}/{resourceName}' and
 * email template discriminator '~' suffix. Enables tenant-aware template resolution with access
 * level enforcement and email template detection.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FrontendResource
 * @see FrontendResourceOrClassLoaderTemplateResolver
 * @see URLConstants
 */
@Component
public class TemplatePathFilteringProcessor {

    private static final Pattern accessLevelPath = Pattern.compile(FRONTEND_RESOURCE_
            + "(" + Arrays.stream(FrontendResource.AccessLevel.values()).map(al -> al.toString().toLowerCase()).collect(Collectors.joining("|")) + ")/"
            + FRONTENDRESOURCEREGEX + "$");
    
    /**
     * Data transfer object holding parsed template path components and metadata.
     * <p>
     * Contains extracted information from template path parsing: access level restriction,
     * email flag, filtered template path, resource name, and frontend resource entry name.
     * Mutable POJO populated by processTemplatePath() method.
     * </p>
     *
     * @author OpenKoda Team
     * @since 1.7.1
     */
    public static class FilteredTemplatePath {
        private boolean email = false;
        private FrontendResource.AccessLevel accessLevel;

        private String filteredTemplate;
        private String filteredResourceName;
        private String frontendResourceEntryName;

        /**
         * Checks if this template is an email template.
         *
         * @return true if email template (path ends with '~'), false otherwise
         */
        public boolean isEmail() {
            return email;
        }

        /**
         * Sets the email template flag.
         *
         * @param isEmail true for email templates, false otherwise
         */
        public void setEmail(boolean isEmail) {
            this.email = isEmail;
        }

        /**
         * Gets the access level restriction for this template.
         *
         * @return the access level (ORGANIZATION, GLOBAL, etc.)
         */
        public FrontendResource.AccessLevel getAccessLevel() {
            return accessLevel;
        }

        /**
         * Sets the access level restriction.
         *
         * @param accessLevel the access level to set
         */
        public void setAccessLevel(FrontendResource.AccessLevel accessLevel) {
            this.accessLevel = accessLevel;
        }

        /**
         * Gets the template path with email discriminator removed.
         *
         * @return the filtered template path
         */
        public String getFilteredTemplate() {
            return filteredTemplate;
        }

        /**
         * Sets the filtered template path.
         *
         * @param filteredTemplate the filtered path to set
         */
        public void setFilteredTemplate(String filteredTemplate) {
            this.filteredTemplate = filteredTemplate;
        }

        /**
         * Gets the resource name with email discriminator removed.
         *
         * @return the filtered resource name
         */
        public String getFilteredResourceName() {
            return filteredResourceName;
        }

        /**
         * Sets the filtered resource name.
         *
         * @param filteredResourceName the filtered name to set
         */
        public void setFilteredResourceName(String filteredResourceName) {
            this.filteredResourceName = filteredResourceName;
        }

        /**
         * Gets the frontend resource entry name (last path segment).
         *
         * @return the entry name extracted from template path
         */
        public String getFrontendResourceEntryName() {
            return frontendResourceEntryName;
        }

        /**
         * Sets the frontend resource entry name.
         *
         * @param frontendResourceEntryName the entry name to set
         */
        public void setFrontendResourceEntryName(String frontendResourceEntryName) {
            this.frontendResourceEntryName = frontendResourceEntryName;
        }
    }

    /**
     * Parses template path to extract access level, email flag, and normalized names.
     * <p>
     * Analyzes the provided template path against access level pattern and email discriminator.
     * Extracts access level from path (e.g., 'frontendresource/organization/mytemplate'),
     * detects email templates via '~' suffix, and produces normalized template and resource names.
     * </p>
     * <p>
     * Algorithm: First checks template against accessLevelPath regex to extract access level from path.
     * Then checks for email discriminator '~' suffix and strips it. Falls back to provided
     * tenantedResourceAccessLevel if no access level in path.
     * </p>
     *
     * @param template the Thymeleaf template path to parse (e.g., 'frontendresource/organization/mytemplate.html~')
     * @param resourceName the original resource name before filtering
     * @param tenantedResourceAccessLevel the default access level to use if not specified in path
     * @return FilteredTemplatePath containing parsed components: access level, email flag, filtered paths and entry name
     * @see FilteredTemplatePath
     * @see FrontendResource.AccessLevel
     * @see URLConstants
     */
    public FilteredTemplatePath processTemplatePath(String template, String resourceName,
            FrontendResource.AccessLevel tenantedResourceAccessLevel) {

        FilteredTemplatePath filteredPath = new FilteredTemplatePath();
        filteredPath.setAccessLevel(tenantedResourceAccessLevel);
        filteredPath.setFilteredTemplate(template);
        filteredPath.setFilteredResourceName(resourceName);

        // resolve template access level
        Matcher m = accessLevelPath.matcher(template);
        if (m.matches()) {
            filteredPath.setAccessLevel(FrontendResource.AccessLevel.valueOf(m.group(1).toUpperCase()));
        }

        int emailPath = -1;
        if (template.endsWith(URLConstants.EMAILRESOURCE_DISCRIMINATOR)) {
            emailPath = template.indexOf("email");
            filteredPath.setEmail(true);
            filteredPath.setFilteredTemplate(template.substring(0, template.length() - 1));
            filteredPath.setFilteredResourceName(resourceName.replace(URLConstants.EMAILRESOURCE_DISCRIMINATOR, ""));
            filteredPath
                    .setFrontendResourceEntryName(StringUtils.substring(template, emailPath, template.length() - 1));
        } else {
            filteredPath.setFrontendResourceEntryName(StringUtils.substringAfterLast(template, "/"));
        }

        return filteredPath;
    }
}

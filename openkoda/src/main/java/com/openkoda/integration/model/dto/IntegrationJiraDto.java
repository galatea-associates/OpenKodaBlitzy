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

package com.openkoda.integration.model.dto;

/**
 * Data transfer object for Jira integration configuration form binding.
 * Mutable JavaBean for Jira Cloud OAuth tokens and project settings.
 * <p>
 * This DTO is used to transfer Jira integration configuration data between
 * the web layer and service layer. It contains the essential project and
 * organization identifiers required for Jira Cloud API integration.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class IntegrationJiraDto {
    /**
     * Jira project name.
     */
    public String projectName;
    
    /**
     * Jira organization name.
     */
    public String organizationName;

    /**
     * Gets the Jira project name.
     *
     * @return the Jira project name
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Sets the Jira project name.
     *
     * @param projectName the Jira project name to set
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Gets the Jira organization name.
     *
     * @return the Jira organization name
     */
    public String getOrganizationName() {
        return organizationName;
    }

    /**
     * Sets the Jira organization name.
     *
     * @param organizationName the Jira organization name to set
     */
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }
}

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
 * Data transfer object for GitHub integration configuration form binding.
 * <p>
 * Mutable JavaBean for GitHub repository and authentication settings. This DTO is used
 * in Spring MVC controllers for form binding and in services for GitHub API integration
 * configuration. Contains repository identification fields required for GitHub REST API calls.
 * 
 * <p>
 * This class follows the JavaBean pattern with public fields and conventional getters/setters.
 * Instances are not thread-safe due to mutable fields; callers must enforce concurrency control.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class IntegrationGitHubDto {
    
    /**
     * GitHub repository name.
     * <p>
     * The name of the repository without the owner prefix. For example, for the repository
     * at https://github.com/owner/repo-name, this field should contain "repo-name".
     * 
     */
    public String gitHubRepoName;
    
    /**
     * GitHub username or organization name.
     * <p>
     * The owner of the repository, which can be either a user account or an organization.
     * For example, for the repository at https://github.com/owner/repo-name, this field
     * should contain "owner".
     * 
     */
    public String gitHubRepoOwner;

    /**
     * Gets the GitHub repository name.
     *
     * @return the repository name without owner prefix, or null if not set
     */
    public String getGitHubRepoName() {
        return gitHubRepoName;
    }

    /**
     * Sets the GitHub repository name.
     *
     * @param gitHubRepoName the repository name to set, can be null
     */
    public void setGitHubRepoName(String gitHubRepoName) {
        this.gitHubRepoName = gitHubRepoName;
    }

    /**
     * Gets the GitHub repository owner.
     *
     * @return the username or organization name that owns the repository, or null if not set
     */
    public String getGitHubRepoOwner() {
        return gitHubRepoOwner;
    }

    /**
     * Sets the GitHub repository owner.
     *
     * @param gitHubRepoOwner the username or organization name to set, can be null
     */
    public void setGitHubRepoOwner(String gitHubRepoOwner) {
        this.gitHubRepoOwner = gitHubRepoOwner;
    }
}

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
 * Data transfer object for Basecamp integration configuration form binding.
 * <p>
 * Mutable JavaBean used in controllers and services for Basecamp API endpoint configuration.
 * This DTO carries Basecamp API URLs between web layer, service layer, and persistence.
 * Instances are not thread-safe; callers must enforce concurrency control.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class IntegrationBasecampDto {
    
    /**
     * Basecamp API URL for to-do list operations.
     * <p>
     * Format example: {@code https://3.basecampapi.com/{account}/buckets/{project}/todolists/{list}/todos.json}
     * 
     */
    public String toDoListUrl;

    /**
     * Returns the Basecamp API URL for to-do list operations.
     *
     * @return the Basecamp API URL, may be null if not configured
     */
    public String getToDoListUrl() {
        return toDoListUrl;
    }

    /**
     * Sets the Basecamp API URL for to-do list operations.
     *
     * @param toDoListUrl the Basecamp API URL in format https://3.basecampapi.com/{account}/buckets/{project}/todolists/{list}/todos.json
     */
    public void setToDoListUrl(String toDoListUrl) {
        this.toDoListUrl = toDoListUrl;
    }
}

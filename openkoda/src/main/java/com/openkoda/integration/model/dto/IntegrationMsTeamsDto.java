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
 * Data transfer object for Microsoft Teams integration configuration form binding.
 * <p>
 * This mutable JavaBean carries MS Teams webhook endpoint configuration between
 * controllers, services, and form binding layers. It uses the standard JavaBean pattern
 * with public fields and conventional getter/setter methods for compatibility with
 * Spring MVC property binding and Jackson serialization.
 * 
 * <p>
 * The webhook URL field contains sensitive endpoint information that should be protected.
 * Instances are not thread-safe due to mutable fields.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class IntegrationMsTeamsDto {
    
    /**
     * Microsoft Teams incoming webhook URL for posting messages.
     * This is a sensitive endpoint that should be protected. Avoid logging
     * this value in plaintext and store it in secure configuration stores.
     */
    public String webhookUrl;

    /**
     * Gets the Microsoft Teams webhook URL.
     *
     * @return the Microsoft Teams incoming webhook URL, may be null if not configured
     */
    public String getWebhookUrl() {
        return webhookUrl;
    }

    /**
     * Sets the Microsoft Teams webhook URL.
     *
     * @param webhookUrl the Microsoft Teams incoming webhook URL to configure
     */
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}

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
 * Data transfer object for Slack integration configuration form binding.
 * <p>
 * Mutable JavaBean for Slack webhook endpoint configuration. This DTO is used
 * to capture and transfer Slack webhook settings between the web layer and
 * service layer during integration setup and updates.
 * 
 * <p>
 * The webhook URL is a sensitive endpoint that should be protected and stored
 * securely to prevent unauthorized access to Slack channels.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class IntegrationSlackDto {
    
    /**
     * Slack incoming webhook URL for posting messages.
     * <p>
     * This is a sensitive endpoint that should be protected. The webhook URL
     * allows the application to send messages to configured Slack channels.
     * 
     */
    public String webhookUrl;

    /**
     * Gets the Slack incoming webhook URL.
     * <p>
     * Returns the configured webhook endpoint for sending messages to Slack.
     * This URL should be treated as sensitive configuration data.
     * 
     *
     * @return the Slack webhook URL, may be null if not configured
     */
    public String getWebhookUrl() {
        return webhookUrl;
    }

    /**
     * Sets the Slack incoming webhook URL.
     * <p>
     * Configures the webhook endpoint that will be used to send messages to
     * Slack channels. The URL should be obtained from Slack's incoming webhook
     * integration setup and kept secure.
     * 
     *
     * @param webhookUrl the Slack webhook URL to configure, may be null
     */
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}

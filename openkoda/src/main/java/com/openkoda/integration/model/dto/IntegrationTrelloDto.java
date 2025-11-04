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
 * Data transfer object for Trello integration configuration form binding.
 * Mutable JavaBean for Trello API credentials and board settings.
 * <p>
 * This DTO encapsulates Trello integration parameters including API authentication
 * credentials and target board information. Used for binding form input to Trello
 * integration configuration.

 * <p>
 * Example usage:
 * <pre>
 * IntegrationTrelloDto dto = new IntegrationTrelloDto();
 * dto.setTrelloApiKey("your-api-key");
 * dto.setTrelloBoardName("Project Board");
 * </pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
public class IntegrationTrelloDto {

    /**
     * Trello API key. This is a sensitive credential that should be protected.
     */
    public String trelloApiKey;
    
    /**
     * Trello API token. This is a sensitive credential that should be protected.
     */
    public String trelloApiToken;
    
    /**
     * Trello board name.
     */
    public String trelloBoardName;
    
    /**
     * Trello list name within board.
     */
    public String trelloListName;

    /**
     * Gets the Trello API key.
     *
     * @return the Trello API key, or null if not set
     */
    public String getTrelloApiKey() {
        return trelloApiKey;
    }

    /**
     * Sets the Trello API key.
     *
     * @param trelloApiKey the Trello API key to set
     */
    public void setTrelloApiKey(String trelloApiKey) {
        this.trelloApiKey = trelloApiKey;
    }

    /**
     * Gets the Trello API token.
     *
     * @return the Trello API token, or null if not set
     */
    public String getTrelloApiToken() {
        return trelloApiToken;
    }

    /**
     * Sets the Trello API token.
     *
     * @param trelloApiToken the Trello API token to set
     */
    public void setTrelloApiToken(String trelloApiToken) {
        this.trelloApiToken = trelloApiToken;
    }

    /**
     * Gets the Trello board name.
     *
     * @return the Trello board name, or null if not set
     */
    public String getTrelloBoardName() {
        return trelloBoardName;
    }

    /**
     * Sets the Trello board name.
     *
     * @param trelloBoardName the Trello board name to set
     */
    public void setTrelloBoardName(String trelloBoardName) {
        this.trelloBoardName = trelloBoardName;
    }

    /**
     * Gets the Trello list name within the board.
     *
     * @return the Trello list name, or null if not set
     */
    public String getTrelloListName() {
        return trelloListName;
    }

    /**
     * Sets the Trello list name within the board.
     *
     * @param trelloListName the Trello list name to set
     */
    public void setTrelloListName(String trelloListName) {
        this.trelloListName = trelloListName;
    }
}

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

package com.openkoda.integration.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.openkoda.dto.NotificationDto;
import com.openkoda.integration.controller.IntegrationComponentProvider;
import com.openkoda.integration.model.configuration.IntegrationModuleOrganizationConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.social.support.URIBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Trello REST API consumer for creating cards on Trello boards. Implements notification-to-Trello-card conversion.
 * <p>
 * This service integrates OpenKoda notifications with Trello by automatically creating cards in designated
 * boards and lists. It handles board and list resolution or creation before card creation, ensuring that
 * notifications are properly tracked in external Trello projects.
 * </p>
 * <p>
 * Authentication uses Trello API key and token (trelloApiKey, trelloApiToken) from per-organization
 * configuration. Trello uses query parameter authentication instead of header-based authentication,
 * with all endpoints under the /1/ API path.
 * </p>
 * <p>
 * The integration workflow follows these steps:
 * <ol>
 *   <li>Verify notification scope (must be organization-scoped)</li>
 *   <li>Load per-organization configuration with API credentials</li>
 *   <li>Validate API credentials using StringUtils</li>
 *   <li>Resolve board ID by name or create new board if missing</li>
 *   <li>Resolve list ID within board or create new list if missing</li>
 *   <li>Create card via POST /1/cards with query params: key, token, idList, name, desc</li>
 * </ol>
 * </p>
 * <p>
 * <b>Trello API peculiarities:</b> Unlike most REST APIs, Trello uses query parameters for authentication
 * (key and token) instead of Authorization headers. All API endpoints are versioned under /1/ path.
 * No OAuth refresh is needed as Trello uses long-lived API tokens.
 * </p>
 * <p>
 * Example Trello configuration:
 * <pre>
 * trelloApiKey=32-character-hexadecimal-key
 * trelloApiToken=64-character-token
 * trelloBoardName=Project Notifications
 * trelloListName=Incoming Alerts
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see IntegrationComponentProvider
 * @see NotificationDto
 * @see IntegrationModuleOrganizationConfiguration
 */
@Service
public class TrelloIntegrationConsumers extends IntegrationComponentProvider {

    /**
     * Trello API endpoint for retrieving boards accessible to the authenticated user.
     * Default: https://api.trello.com/1/members/me/boards
     * <p>
     * Query parameters: fields=name, key={apiKey}, token={apiToken}
     * </p>
     */
    @Value("${api.trello.get.boards:https://api.trello.com/1/members/me/boards}")
    private String TRELLO_GET_BOARDS_API;

    /**
     * Trello API endpoint for retrieving lists within a board.
     * Default: https://api.trello.com/1/boards/
     * <p>
     * Must be appended with board ID. Query parameters: lists=all, fields=lists, key={apiKey}, token={apiToken}
     * </p>
     */
    @Value("${api.trello.get.lists:https://api.trello.com/1/boards/}")
    private String TRELLO_GET_LISTS_API;

    /**
     * Trello API endpoint for creating new boards.
     * Default: https://api.trello.com/1/boards/
     * <p>
     * Query parameters: name={boardName}, defaultLists=false, key={apiKey}, token={apiToken}
     * </p>
     */
    @Value("${api.trello.create.board:https://api.trello.com/1/boards/}")
    private String TRELLO_CREATE_BOARD_API;

    /**
     * Trello API endpoint for creating lists within a board.
     * Default: https://api.trello.com/1/lists
     * <p>
     * Query parameters: name={listName}, idBoard={boardId}, key={apiKey}, token={apiToken}
     * </p>
     */
    @Value("${api.trello.create.list:https://api.trello.com/1/lists}")
    private String TRELLO_CREATE_LIST_API;

    /**
     * Trello API endpoint for creating cards within a list.
     * Default: https://api.trello.com/1/cards
     * <p>
     * Query parameters: idList={listId}, pos=0, name={cardTitle}, desc={cardDescription}, key={apiKey}, token={apiToken}
     * </p>
     */
    @Value("${api.trello.create.card:https://api.trello.com/1/cards}")
    private String TRELLO_CREATE_CARD_API;

    /**
     * RestTemplate instance for executing HTTP requests to Trello API endpoints.
     * Used for all GET and POST operations to Trello's REST API.
     */
    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Creates a Trello card from an organization notification with automatic board and list resolution.
     * <p>
     * This method orchestrates the complete workflow of converting an OpenKoda notification into a Trello card.
     * It validates notification scope, loads organization-specific Trello configuration, ensures that the
     * target board and list exist (creating them if necessary), and finally creates the card with the
     * notification message as the card description.
     * </p>
     * <p>
     * <b>Workflow steps:</b>
     * <ol>
     *   <li>Verify notification scope - must be organization-scoped (not global)</li>
     *   <li>Check propagate flag - skip if notification should not be propagated</li>
     *   <li>Load per-organization configuration with API credentials and board/list names</li>
     *   <li>Validate API credentials (key and token must be non-blank)</li>
     *   <li>Resolve board ID by name, or create new board if not found</li>
     *   <li>Resolve list ID within board by name, or create new list if not found</li>
     *   <li>Create card in the resolved list with notification message as description</li>
     * </ol>
     * </p>
     * <p>
     * <b>Validation requirements:</b>
     * API key must be 32 characters hexadecimal, token must be 64 characters alphanumeric.
     * Board name and list name must be configured in IntegrationModuleOrganizationConfiguration.
     * </p>
     * <p>
     * Example configuration:
     * <pre>
     * trelloApiKey=a1b2c3d4e5f6...
     * trelloApiToken=0123456789abcdef...
     * trelloBoardName=Project Notifications
     * trelloListName=New Alerts
     * </pre>
     * </p>
     *
     * @param notification the NotificationDto containing message to convert to Trello card.
     *                     Must be organization-scoped with propagate flag set to true.
     * @throws Exception if API errors occur (network failures, authentication errors, invalid board/list),
     *                   or if RestClientException is thrown during HTTP operations
     * @see NotificationDto
     * @see IntegrationModuleOrganizationConfiguration
     * @see #getBoardId(IntegrationModuleOrganizationConfiguration)
     * @see #createBoard(IntegrationModuleOrganizationConfiguration)
     * @see #getListId(IntegrationModuleOrganizationConfiguration, String)
     * @see #createList(IntegrationModuleOrganizationConfiguration, String)
     * @see #createNotificationCard(IntegrationModuleOrganizationConfiguration, String, NotificationDto)
     */
    public void createTrelloCardFromOrgNotification(NotificationDto notification) throws Exception {
        debug("[createTrelloCardFromOrgNotification]");
        if (!services.notification.isOrganization(notification)) {
            info("[createTrelloCardFromOrgNotification] Notification is not organizational.");
            return;
        }
        if(!notification.getPropagate()){
            return;
        }
        IntegrationModuleOrganizationConfiguration integrationConfiguration
                = integrationService.getInnerOrganizationConfig(notification.getOrganizationId());
        String apiKey = integrationConfiguration.getTrelloApiKey();
        String apiToken = integrationConfiguration.getTrelloApiToken();
        if (StringUtils.isBlank(apiKey) || StringUtils.isBlank(apiToken)) {
            warn("[createTrelloCardFromOrgNotification] Trello key or token unavailable");
            return;
        }
        if (StringUtils.isBlank(integrationConfiguration.getTrelloBoardName()) || StringUtils.isBlank(integrationConfiguration.getTrelloListName())) {
            warn("[createTrelloCardFromOrgNotification] Trello configuration is invalid: lack of board/list name.");
            return;
        }
        String boardId = getBoardId(integrationConfiguration);
        String listId;
        if (StringUtils.isBlank(boardId)) {
            boardId = createBoard(integrationConfiguration);
            listId = createList(integrationConfiguration, boardId);
        } else {
            listId = getListId(integrationConfiguration, boardId);
            if (StringUtils.isBlank(listId)) {
                listId = createList(integrationConfiguration, boardId);
            }
        }
        createNotificationCard(integrationConfiguration, listId, notification);
    }

    /**
     * Retrieves the board ID for a board matching the configured board name.
     * <p>
     * Performs a GET request to TRELLO_GET_BOARDS_API to retrieve all boards accessible to the authenticated
     * user, then searches for a board matching the name specified in the organization configuration.
     * </p>
     * <p>
     * <b>API endpoint:</b> GET https://api.trello.com/1/members/me/boards
     * </p>
     * <p>
     * <b>Query parameters:</b>
     * <ul>
     *   <li>fields=name - Only retrieve board name field</li>
     *   <li>key={apiKey} - Trello API key from configuration</li>
     *   <li>token={apiToken} - Trello API token from configuration</li>
     * </ul>
     * </p>
     * <p>
     * <b>Response format:</b> Array of board objects with id and name fields.
     * Filters by exact name match using trelloBoardName from configuration.
     * </p>
     *
     * @param config the IntegrationModuleOrganizationConfiguration containing Trello API credentials
     *               (trelloApiKey, trelloApiToken) and target board name (trelloBoardName)
     * @return the Trello board ID string if a matching board is found, or null if no board matches
     * @throws Exception if API errors occur during board retrieval or if authentication fails
     * @see IntegrationModuleOrganizationConfiguration#getTrelloApiKey()
     * @see IntegrationModuleOrganizationConfiguration#getTrelloApiToken()
     * @see IntegrationModuleOrganizationConfiguration#getTrelloBoardName()
     */
    private String getBoardId(IntegrationModuleOrganizationConfiguration config) throws Exception {
        debug("[getBoardId]");
        URIBuilder builder = URIBuilder.fromUri(TRELLO_GET_BOARDS_API)
                .queryParam("fields", "name")
                .queryParam("key", config.getTrelloApiKey())
                .queryParam("token", config.getTrelloApiToken());
        ResponseEntity<List> response = restTemplate.getForEntity(builder.build(), List.class);
        integrationService.handleResponseError(response, "[getBoardId] Error while checking data integrity. Code: {}. Error: {}");
        List<Map<String, String>> boards = (List<Map<String, String>>) response.getBody();
        return boards.stream().filter(board -> board.get("name").equals(config.getTrelloBoardName()))
                .findAny().map(board -> board.get("id"))
                .orElse(null);

    }

    /**
     * Retrieves the list ID for a list matching the configured list name within a specific board.
     * <p>
     * Performs a GET request to retrieve all lists within the specified board, then searches for a list
     * matching the name specified in the organization configuration. Only considers lists that are not closed.
     * </p>
     * <p>
     * <b>API endpoint:</b> GET https://api.trello.com/1/boards/{boardId}
     * </p>
     * <p>
     * <b>Query parameters:</b>
     * <ul>
     *   <li>lists=all - Retrieve all lists including closed lists</li>
     *   <li>fields=lists - Only retrieve lists field from board object</li>
     *   <li>key={apiKey} - Trello API key from configuration</li>
     *   <li>token={apiToken} - Trello API token from configuration</li>
     * </ul>
     * </p>
     * <p>
     * <b>Filtering logic:</b> Searches for exact name match using trelloListName from configuration
     * and filters out closed lists (closed=true). Returns the first matching open list.
     * </p>
     *
     * @param config the IntegrationModuleOrganizationConfiguration containing Trello API credentials
     *               and target list name (trelloListName)
     * @param boardId the Trello board ID string to search for lists within
     * @return the Trello list ID string if a matching open list is found, or null if no list matches
     *         or if all matching lists are closed
     * @throws Exception if API errors occur during list retrieval or if authentication fails
     * @see IntegrationModuleOrganizationConfiguration#getTrelloListName()
     */
    private String getListId(IntegrationModuleOrganizationConfiguration config, String boardId) throws Exception {
        debug("[getListId]");
        URIBuilder builder = URIBuilder.fromUri(TRELLO_GET_LISTS_API + boardId)
                .queryParam("lists", "all")
                .queryParam("fields", "lists")
                .queryParam("key", config.getTrelloApiKey())
                .queryParam("token", config.getTrelloApiToken());
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(builder.build(), JsonNode.class);
        integrationService.handleResponseError(response, "[getBoardId] Error while checking data integrity. Code: {}. Error: {}");
        JsonNode lists = response.getBody();
        for (JsonNode list : lists.get("lists")) {
            if (list.get("name").asText().equals(config.getTrelloListName()) && !list.get("closed").asBoolean()) {
                return list.get("id").asText();
            }
        }
        return null;
    }

    /**
     * Creates a new Trello board with the name specified in the organization configuration.
     * <p>
     * Performs a POST request to TRELLO_CREATE_BOARD_API to create a new board. The board is created
     * without default lists (defaultLists=false) to allow custom list configuration.
     * </p>
     * <p>
     * <b>API endpoint:</b> POST https://api.trello.com/1/boards/
     * </p>
     * <p>
     * <b>Query parameters:</b>
     * <ul>
     *   <li>name={boardName} - Name for the new board from trelloBoardName configuration</li>
     *   <li>defaultLists=false - Do not create default lists (To Do, Doing, Done)</li>
     *   <li>key={apiKey} - Trello API key from configuration</li>
     *   <li>token={apiToken} - Trello API token from configuration</li>
     * </ul>
     * </p>
     * <p>
     * <b>Response:</b> JSON object containing the newly created board's details including id field.
     * </p>
     *
     * @param config the IntegrationModuleOrganizationConfiguration containing Trello API credentials
     *               and board name (trelloBoardName)
     * @return the Trello board ID string of the newly created board
     * @throws Exception if board creation fails due to API errors, authentication failures,
     *                   or if the board name is invalid or already exists
     * @see IntegrationModuleOrganizationConfiguration#getTrelloBoardName()
     */
    private String createBoard(IntegrationModuleOrganizationConfiguration config) throws Exception {
        debug("[createBoard]");
        URIBuilder builder = URIBuilder.fromUri(TRELLO_CREATE_BOARD_API)
                .queryParam("name", config.getTrelloBoardName())
                .queryParam("defaultLists", "false")
                .queryParam("key", config.getTrelloApiKey())
                .queryParam("token", config.getTrelloApiToken());
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(builder.build(), null, JsonNode.class);
        integrationService.handleResponseError(response, "[createBoard] Error while creating new Board. Code: {}. Error: {}");
        return response.getBody().get("id").asText();
    }

    /**
     * Creates a new list within a specified Trello board.
     * <p>
     * Performs a POST request to TRELLO_CREATE_LIST_API to create a new list with the name specified
     * in the organization configuration. The list is created within the board identified by boardId.
     * </p>
     * <p>
     * <b>API endpoint:</b> POST https://api.trello.com/1/lists
     * </p>
     * <p>
     * <b>Query parameters:</b>
     * <ul>
     *   <li>name={listName} - Name for the new list from trelloListName configuration</li>
     *   <li>idBoard={boardId} - ID of the board where the list will be created</li>
     *   <li>key={apiKey} - Trello API key from configuration</li>
     *   <li>token={apiToken} - Trello API token from configuration</li>
     * </ul>
     * </p>
     * <p>
     * <b>Response:</b> JSON object containing the newly created list's details including id field.
     * </p>
     *
     * @param config the IntegrationModuleOrganizationConfiguration containing Trello API credentials
     *               and list name (trelloListName)
     * @param boardId the Trello board ID string where the list will be created
     * @return the Trello list ID string of the newly created list
     * @throws Exception if list creation fails due to API errors, authentication failures,
     *                   invalid board ID, or if the list name is invalid
     * @see IntegrationModuleOrganizationConfiguration#getTrelloListName()
     */
    private String createList(IntegrationModuleOrganizationConfiguration config, String boardId) throws Exception {
        debug("[createList]");
        URIBuilder builder = URIBuilder.fromUri(TRELLO_CREATE_LIST_API)
                .queryParam("name", config.getTrelloListName())
                .queryParam("idBoard", boardId)
                .queryParam("key", config.getTrelloApiKey())
                .queryParam("token", config.getTrelloApiToken());
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(builder.build(), null, JsonNode.class);
        integrationService.handleResponseError(response, "[createBoard] Error while creating new Board. Code: {}. Error: {}");
        return response.getBody().get("id").asText();
    }

    /**
     * Creates a Trello card within a specified list containing the notification message.
     * <p>
     * Performs a POST request to TRELLO_CREATE_CARD_API to create a new card with a fixed title
     * "New notification!" and the notification message as the card description. The card is positioned
     * at the top of the list (pos=0).
     * </p>
     * <p>
     * <b>API endpoint:</b> POST https://api.trello.com/1/cards
     * </p>
     * <p>
     * <b>Query parameters:</b>
     * <ul>
     *   <li>idList={listId} - ID of the list where the card will be created</li>
     *   <li>pos=0 - Position of the card (0=top of list)</li>
     *   <li>name="New notification!" - Fixed card title</li>
     *   <li>desc={notification.getMessage()} - Card description from notification message</li>
     *   <li>key={apiKey} - Trello API key from configuration</li>
     *   <li>token={apiToken} - Trello API token from configuration</li>
     * </ul>
     * </p>
     * <p>
     * <b>Card positioning:</b> pos=0 ensures that new notification cards always appear at the top
     * of the list, making recent notifications immediately visible.
     * </p>
     *
     * @param config the IntegrationModuleOrganizationConfiguration containing Trello API credentials
     * @param listId the Trello list ID string where the card will be created
     * @param notification the NotificationDto containing the message to use as card description
     * @throws Exception if card creation fails due to API errors, authentication failures,
     *                   invalid list ID, or if the notification message exceeds Trello's size limits
     * @see NotificationDto#getMessage()
     */
    private void createNotificationCard(IntegrationModuleOrganizationConfiguration config, String listId, NotificationDto notification) throws Exception {
        debug("[createNotificationCard]");
        URIBuilder builder = URIBuilder.fromUri(TRELLO_CREATE_CARD_API)
                .queryParam("idList", listId)
                .queryParam("pos", "0")
                .queryParam("name", "New notification!")
                .queryParam("desc", notification.getMessage())
                .queryParam("key", config.getTrelloApiKey())
                .queryParam("token", config.getTrelloApiToken());
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(builder.build(), null, JsonNode.class);
        integrationService.handleResponseError(response, "[createBoard] Error while creating new Board. Code: {}. Error: {}");

    }

}

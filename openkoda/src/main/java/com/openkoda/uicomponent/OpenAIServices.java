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

package com.openkoda.uicomponent;

import com.openkoda.uicomponent.annotation.Autocomplete;

/**
 * Service contract for OpenAI ChatGPT integration including prompt-based conversations, model selection, and data schema generation.
 * <p>
 * Provides methods for sending messages to ChatGPT with customizable prompts, models, and temperature.
 * Supports conversation context via conversationId for multi-turn interactions.
 * Offers data schema prompt generation for entity metadata exposure to LLM.
 * All methods except conversationId variant accept varargs repositoryNames for entity context.
 * Methods annotated with {@code @Autocomplete} provide metadata for UI tooling.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.service.openai.ChatGPTService
 * @see com.openkoda.service.openai.ChatGPTPromptService
 * @see com.openkoda.uicomponent.live.LiveOpenAIServices
 */
public interface OpenAIServices {

    /**
     * Sends message to ChatGPT with prompt template file, optional entity schemas, configurable model and temperature.
     * <p>
     * Loads prompt template from promptFileName, retrieves entity schemas for repositoryNames via SearchableRepositories,
     * composes final prompt with chatGPTPromptService.getDataSchemas, sends to chatGPTService with model/temperature parameters.
     * Prompt template can include placeholders for entity schemas.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * sendMessageToGPTWithPrompt("analyze-user", "Summarize user data", "gpt-4", "0.7", "user", "role");
     * }</pre>
     * 
     *
     * @param promptFileName Prompt template filename (loaded from prompt repository or file system)
     * @param message User message content to send to ChatGPT
     * @param model OpenAI model identifier (e.g., "gpt-4", "gpt-3.5-turbo") or null for default
     * @param temperature Sampling temperature string (0.0-2.0, higher=more random) or null for default (1.0)
     * @param repositoryNames Varargs array of entity repository names for schema context (e.g., "user", "organization")
     * @return ChatGPT response text from completion API
     * @throws RuntimeException If OpenAI API call fails or prompt file not found
     */
    @Autocomplete
    String sendMessageToGPTWithPrompt(String promptFileName, String message, String model, String temperature,
            String... repositoryNames);

    /**
     * Sends message to ChatGPT with entity schemas but without prompt template.
     * <p>
     * Similar to sendMessageToGPTWithPrompt but omits prompt template loading. Retrieves entity schemas
     * for repositoryNames, composes context, sends message with schemas to chatGPTService.
     * Suitable for ad-hoc queries without predefined prompts.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * sendMessageToGPT("List all user fields", "gpt-3.5-turbo", "0.5", "user");
     * }</pre>
     * 
     *
     * @param message User message content to send to ChatGPT
     * @param model OpenAI model identifier or null for default
     * @param temperature Sampling temperature string or null for default
     * @param repositoryNames Varargs array of entity repository names for schema context
     * @return ChatGPT response text from completion API
     * @throws RuntimeException If OpenAI API call fails
     */
    String sendMessageToGPT(String message, String model, String temperature, String... repositoryNames);

    /**
     * Sends message to ChatGPT within existing conversation context for multi-turn interactions.
     * <p>
     * Retrieves conversation history from chatGPTService using conversationId, appends message to conversation,
     * sends complete message array to OpenAI API. Maintains context across multiple turns. Temperature/model use
     * conversation defaults.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * sendMessageToGPT("What about their permissions?", "conv-12345"); // continues previous conversation
     * }</pre>
     * 
     * <p>
     * Thread-safety: Conversation storage managed by ChatGPTService, may use synchronized access.
     * 
     *
     * @param message User message content to append to conversation
     * @param conversationId Conversation identifier for context retrieval (previous messages maintained by ChatGPTService)
     * @return ChatGPT response text considering full conversation history
     * @throws RuntimeException If OpenAI API call fails or conversationId invalid
     */
    @Autocomplete
    String sendMessageToGPT(String message, String conversationId);

    /**
     * Generates comprehensive prompt text containing all dynamic entity schemas for LLM context.
     * <p>
     * Calls SearchableRepositories.getDynamicSearchableRepositoriesEntityKeys() to get all entity types,
     * retrieves schemas via chatGPTPromptService.getDataSchemas(...), formats as prompt text.
     * Useful for providing LLM with complete data model context for queries.
     * 
     * <p>
     * Example usage:
     * <pre>{@code
     * String schemas = getCompleteDataSchemaPrompt();
     * // Returns: "Entity schemas:\nUser: {id, email, ...}\nOrganization: {...}"
     * }</pre>
     * 
     * <p>
     * Performance: May be expensive for large schemas - consider caching result.
     * 
     *
     * @return String containing formatted entity schemas for all SearchableRepositories
     */
    @Autocomplete
    String getCompleteDataSchemaPrompt();

}

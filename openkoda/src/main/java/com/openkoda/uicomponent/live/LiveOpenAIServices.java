package com.openkoda.uicomponent.live;

import com.openkoda.repository.SearchableRepositories;
import com.openkoda.service.openai.ChatGPTPromptService;
import com.openkoda.service.openai.ChatGPTService;
import com.openkoda.uicomponent.OpenAIServices;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;


/**
 * Implementation of {@link OpenAIServices} providing ChatGPT integration for AI-powered operations in UI components.
 * <p>
 * This service acts as a bridge between UI components and OpenAI's GPT models, enabling natural language processing
 * and AI-powered features. It delegates message transport and conversation management to {@link ChatGPTService}
 * and prompt composition to {@link ChatGPTPromptService}.
 * </p>
 * <p>
 * Key capabilities include:
 * <ul>
 *   <li>Sending messages to GPT models with configurable parameters (model, temperature)</li>
 *   <li>Managing conversation contexts for multi-turn interactions</li>
 *   <li>Loading and using prompt templates for structured interactions</li>
 *   <li>Retrieving entity schema information for data-aware prompt generation</li>
 * </ul>
 * </p>
 * <p>
 * Entity schema retrieval is performed via {@link SearchableRepositories} to provide GPT with context about
 * available data structures, enabling more accurate and context-aware responses.
 * </p>
 * <p>
 * This class is stateless and thread-safe, as it delegates all operations to thread-safe service dependencies.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * String response = service.sendMessageToGPT("Summarize user data", "gpt-4", "0.7", "user");
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see OpenAIServices
 * @see ChatGPTService
 * @see ChatGPTPromptService
 * @see SearchableRepositories
 */
@Component
public class LiveOpenAIServices implements OpenAIServices {
    
    /**
     * Service for OpenAI API communication and conversation management.
     * <p>
     * Handles message transport to OpenAI's GPT models, manages conversation contexts,
     * and processes responses. Supports multiple GPT models with configurable parameters.
     * </p>
     */
    @Inject
    ChatGPTService chatGPTService;
    
    /**
     * Service for prompt template composition and data schema generation.
     * <p>
     * Provides capabilities for loading prompt templates and generating structured prompts
     * that include entity schema information for context-aware GPT interactions.
     * </p>
     */
    @Inject
    ChatGPTPromptService promptService;
    
    /**
     * Sends a message to GPT with specified model and temperature parameters, optionally including entity context.
     * <p>
     * This method initiates a new conversation (conversationId is null) and sends the message with configurable
     * model selection and creativity settings. Entity schemas from specified repositories are included as context
     * to enable data-aware responses.
     * </p>
     * <p>
     * The temperature parameter controls response creativity: lower values (0.0-0.3) produce more deterministic
     * responses, while higher values (0.7-1.0) increase randomness and creativity.
     * </p>
     *
     * @param message the user message to send to GPT
     * @param model the GPT model name to use (e.g., "gpt-3.5-turbo", "gpt-4")
     * @param temperature the creativity setting as a string representation of a float between 0.0 and 1.0
     * @param repositoryNames optional entity repository names to include as context for the GPT model
     * @return the GPT response as a string
     */
    @Override
    public String sendMessageToGPT(String message, String model, String temperature, String... repositoryNames) {
        return chatGPTService.sendMessageToGPT(null, message, model, temperature, null, repositoryNames);
    }

    /**
     * Sends a message to GPT within an existing conversation context.
     * <p>
     * This method continues an existing conversation by sending a message with the specified conversation identifier.
     * The conversation context is maintained by the {@link ChatGPTService}, allowing for multi-turn interactions
     * where the GPT model remembers previous messages and maintains contextual awareness.
     * </p>
     * <p>
     * Model and temperature parameters are not specified in this variant, as they are determined by the
     * conversation's initial configuration.
     * </p>
     *
     * @param message the user message to send to GPT
     * @param conversationId the unique conversation identifier for context continuation
     * @return the GPT response as a string, informed by the conversation history
     */
    @Override
    public String sendMessageToGPT(String message, String conversationId) {
        return chatGPTService.sendMessageToGPT(message, conversationId, null);
    }

    /**
     * Sends a message to GPT using a prompt template file with specified model and temperature parameters.
     * <p>
     * This method loads a prompt template from the specified file and combines it with the user message before
     * sending to GPT. Prompt templates provide structured instructions that guide the GPT model's behavior
     * and response format. Entity schemas from specified repositories are included as context.
     * </p>
     * <p>
     * The prompt file is loaded by {@link ChatGPTService} and merged with the message content. This enables
     * consistent, repeatable interactions with predefined instruction sets.
     * </p>
     *
     * @param promptFileName the name of the prompt template file to load
     * @param message the user message to send to GPT
     * @param model the GPT model name to use (e.g., "gpt-3.5-turbo", "gpt-4")
     * @param temperature the creativity setting as a string representation of a float between 0.0 and 1.0
     * @param repositoryNames optional entity repository names to include as context for the GPT model
     * @return the GPT response as a string, formatted according to the prompt template instructions
     */
    @Override
    public String sendMessageToGPTWithPrompt(String promptFileName, String message, String model, String temperature,
                                             String... repositoryNames) {
        return chatGPTService.sendMessageToGPT(promptFileName, message, model, temperature, null, repositoryNames);
    }

    /**
     * Retrieves a complete data schema prompt containing all available entity structures.
     * <p>
     * This method generates a comprehensive prompt string that describes all searchable entity schemas in the system.
     * Entity keys are retrieved via {@link SearchableRepositories#getDynamicSearchableRepositoriesEntityKeys()},
     * and the schemas are composed by {@link ChatGPTPromptService#getDataSchemas(java.util.Set)}.
     * </p>
     * <p>
     * The returned prompt provides GPT with detailed information about available data structures, field types,
     * relationships, and constraints. This enables the model to generate accurate queries, provide data-aware
     * suggestions, and understand the application's domain model.
     * </p>
     * <p>
     * This is typically used when initializing conversations that require deep understanding of the data model,
     * such as query generation, data analysis, or entity manipulation operations.
     * </p>
     *
     * @return a complete data schema prompt as a string containing all entity structure information
     */
    @Override
    public String getCompleteDataSchemaPrompt() {
        return promptService.getDataSchemas(SearchableRepositories.getDynamicSearchableRepositoriesEntityKeys());
    }
}
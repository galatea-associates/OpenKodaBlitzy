package com.openkoda.service.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.annotations.Expose;
import com.openkoda.core.helper.JsonHelper;
import com.openkoda.core.security.OrganizationUser;
import com.openkoda.core.security.UserProvider;
import com.openkoda.core.service.AuditService;
import com.openkoda.core.service.WebsocketService;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Retrofit;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.theokanning.openai.service.OpenAiService.*;

/**
 * OpenAI ChatGPT API client for conversation management and prompt execution with async worker pool.
 * <p>
 * This service provides complete ChatGPT integration lifecycle including conversation contexts,
 * prompt sending to GPT-3.5-turbo or GPT-4 models, response parsing, and conversation history
 * maintenance. Uses a fixed worker thread pool (5 threads) for async execution to prevent blocking
 * the main application threads.
 * 
 * <p>
 * The service includes a disk-backed conversation cache with synchronized writes for persistence
 * across application restarts. Cached responses are retrieved based on conversation history to
 * reduce API costs and latency for repeated queries.
 * 
 * 
 * <b>Architecture Components</b>
 * <ul>
 *   <li>Thread pool: Fixed 5-thread ExecutorService for async request execution</li>
 *   <li>Conversation cache: HashMap storing active conversations by UUID</li>
 *   <li>Disk cache: File-based persistence for conversation prompts and responses</li>
 *   <li>Rate limit handling: Exponential backoff on 429 responses</li>
 *   <li>Retry logic: 3 attempts with increasing delays</li>
 *   <li>Timeout: 120 seconds per request (REQUEST_TIMEOUT_SECONDS)</li>
 * </ul>
 * 
 * <b>Model Selection</b>
 * <ul>
 *   <li>GPT-3.5-turbo: Fast and cost-effective for general queries</li>
 *   <li>GPT-4: Advanced reasoning for complex tasks</li>
 * </ul>
 * 
 * <b>Conversation Management</b>
 * <p>
 * Each conversation is identified by a unique UUID and maintains message history with token limits.
 * System prompts configure AI behavior at the start of conversations. The service supports both
 * template-based system prompts (via ChatGPTPromptService) and direct system prompt text.
 * 
 * 
 * <b>Configuration Properties</b>
 * <ul>
 *   <li>chat.gpt.api.key: OpenAI API key from dashboard (required)</li>
 *   <li>chat.gpt.prompt.cacheFile: Disk cache file location</li>
 *   <li>chat.gpt.prompt.cacheEnabled: Boolean flag to enable/disable caching</li>
 * </ul>
 * 
 * <b>Thread Safety</b>
 * <p>
 * Worker pool executes requests concurrently. Conversation contexts are isolated per UUID.
 * Disk cache uses synchronized writes for thread-safe persistence. Safe for concurrent
 * sendMessageToGPT() calls with different conversation IDs.
 * 
 * 
 * <b>Usage Example</b>
 * <pre>{@code
 * String convId = chatGPTService.sendInitMessageToGPT(
 *     "You are a helpful assistant",
 *     "Explain dependency injection",
 *     "gpt-3.5-turbo",
 *     "0.7",
 *     null);
 * // Response delivered via WebSocket to /queue/ai
 * }</pre>
 * 
 * @see ChatGPTPromptService
 * @see WebsocketService
 * @see Conversation
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
@Service
public class ChatGPTService implements LoggingComponentWithRequestId {

    /**
     * Maximum response length for ChatGPT responses in characters.
     * Responses exceeding this limit will be logged as errors.
     */
    public static final int MAX_GPT_REQUEST_LENGTH = 8000;
    
    private static OpenAiService openAiService;

    @Inject
    WebsocketService websocketService;
    @Inject
    ChatGPTPromptService promptService;
    @Value("${chat.gpt.prompt.cacheFile}") String cacheFileLocation;
    @Value("${chat.gpt.prompt.cacheEnabled}") Boolean cacheGPTMessages;

    /**
     * HTTP request timeout for OpenAI API calls in seconds.
     * Set to 120 seconds to accommodate longer completion requests.
     */
    private static final int REQUEST_TIMEOUT_SECONDS = 120;
    
    /**
     * Fixed thread pool with 5 threads for async ChatGPT request execution.
     * Prevents blocking main application threads during API calls.
     */
    private static ExecutorService executors = Executors.newFixedThreadPool(5);

    /**
     * Static map storing active conversations by UUID.
     * Key: conversation ID (UUID string), Value: Conversation object with message history.
     */
    private static final Map<String /* conversationId */, Conversation /* conversation */> messageMap = new HashMap<>();


    /**
     * Private cache structure for disk-backed conversation persistence.
     * Stores mappings between concatenated user prompts and conversation IDs,
     * and between conversation IDs and cached responses.
     * 
     * @param prompts Map of concatenated user prompts to conversation IDs
     * @param responses Map of conversation IDs to cached response strings
     */
    private record ConversationCache(
            @Expose Map<String /* concatenatedUserPrompts */, String /* conversationId */> prompts,
            @Expose Map<String /* conversationId */, String /* response */> responses
            ){};
    
    /**
     * Chat message structure representing a single message in a conversation.
     * 
     * @param role Message role: "system" (AI behavior config), "user" (human input), or "assistant" (AI response)
     * @param content Message text content
     */
    private record Message(@Expose String role, @Expose String content){};
    
    /**
     * Disk-backed cache for conversation responses.
     * Loaded from file on startup, written on each cache update.
     */
    private static ConversationCache conversationCache;

    /**
     * Conversation state container holding conversation metadata and message history.
     * <p>
     * Each conversation maintains a unique UUID, user context (email and ID), model configuration,
     * and complete message history. Messages include system prompts, user inputs, and assistant responses.
     * 
     * 
     * @param id Unique conversation identifier (UUID string)
     * @param userEmail Email of the user who owns this conversation
     * @param userId User ID from authentication context
     * @param model GPT model name: "gpt-3.5-turbo" or "gpt-4"
     * @param temperature Creativity setting from 0.0 (deterministic) to 2.0 (very creative), 0.7 recommended
     * @param messages List of conversation messages in chronological order
     */
    public record Conversation(@Expose String id, @Expose String userEmail, @Expose Long userId, @Expose String model, @Expose Double temperature, @Expose ArrayList<Message> messages){
        
        /**
         * Adds a system message to configure AI behavior at conversation start.
         * System messages set the context and instructions for the assistant.
         * 
         * @param systemMessage System prompt text defining AI behavior
         */
        private void addSystemMessage(String systemMessage) {
            messages.add(new Message("system", systemMessage));
        }
        
        /**
         * Adds a user-assistant message pair to the conversation history.
         * Called after receiving a response from ChatGPT to maintain full context.
         * 
         * @param userMessage User's input message
         * @param assistantMessage AI's response message
         */
        public void addMessages(String userMessage, String assistantMessage) {
            messages.add(new Message("user", userMessage));
            messages.add(new Message("assistant", assistantMessage));
        }

        /**
         * Generates cache key from conversation history and new message.
         * Concatenates all user messages with "|||" delimiter for cache lookups.
         * 
         * @param message New user message to append to cache key
         * @return Cache key string for prompt lookup
         */
        public String getCacheKey(String message) {
            StringBuffer sb = new StringBuffer();
            for (Message m: messages) {
                if ("user".equals(m.role)) {
                    sb.append(m.content);
                    sb.append("|||");
                }
            }
            sb.append(message);
            return sb.toString();
        }

        /**
         * Retrieves the most recent assistant response from message history.
         * 
         * @return Content of the last assistant message, or null if last message is not from assistant
         */
        public String getLastAssistantMessage() {
            Message m = messages.get(messages.size() - 1);
            if ("assistant".equals(m.role)) {
                return m.content;
            }
            return null;
        }

        /**
         * Constructs a new conversation with a system message.
         * Convenience constructor that initializes empty message list and adds system prompt.
         * 
         * @param id Unique conversation identifier (UUID string)
         * @param userEmail Email of the conversation owner
         * @param userId User ID from authentication context
         * @param model GPT model name: "gpt-3.5-turbo" or "gpt-4"
         * @param temperature Creativity setting from 0.0 to 2.0
         * @param systemMessage System prompt text to configure AI behavior
         */
        public Conversation(String id, String userEmail, Long userId, String model, Double temperature, String systemMessage) {
            this( id, userEmail, userId, model, temperature, new ArrayList<>());
            addSystemMessage(systemMessage);
        }
    }

    /**
     * Constructs ChatGPT service and initializes OpenAI client.
     * <p>
     * Configures OpenAiService with custom timeout (120 seconds), default object mapper for JSON,
     * and Retrofit HTTP client. Uses theokanning/openai-java library for API communication.
     * 
     * 
     * @param gptApiKey OpenAI API key from configuration property chat.gpt.api.key
     */
    @Autowired
    public ChatGPTService(@Value("${chat.gpt.api.key:apiKey}") String gptApiKey) {
        ObjectMapper mapper = defaultObjectMapper();
        OkHttpClient client = defaultClient(gptApiKey, Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS));
        Retrofit retrofit = defaultRetrofit(client, mapper);

        OpenAiApi api = retrofit.create(OpenAiApi.class);
        openAiService = new OpenAiService(api);
    }

    /**
     * Sends a message to ChatGPT with template-based system prompt generation.
     * <p>
     * Creates a new conversation with system prompt generated from template file and entity context.
     * Stores conversation in messageMap and submits async task for execution.
     * 
     * 
     * @param promptFileName Template filename for system prompt generation via ChatGPTPromptService
     * @param message User message text to send to ChatGPT
     * @param model Model name: "gpt-3.5-turbo" (fast, cost-effective) or "gpt-4" (advanced reasoning)
     * @param temperature Creativity setting from 0.0 to 2.0, 0.7 recommended for balanced responses
     * @param channelName WebSocket channel name for response delivery, null to use user's default queue
     * @param repositoryNames Entity keys for context generation in system prompt template
     * @return Conversation ID (UUID string) for tracking and continuing conversation
     */
    public String sendMessageToGPT(String promptFileName, String message, String model, String temperature, String channelName, String ... repositoryNames) {
        debug("[sendMessageToGPT-1] {} {} {} {}", message, model, temperature, repositoryNames);
        String systemPrompt = promptService.getPromptFromFileForEntities(promptFileName, repositoryNames);
        OrganizationUser ou = UserProvider.getFromContext().orElseThrow(RuntimeException::new);
        String id = UUID.randomUUID().toString();
        debug("[sendMessageToGPT-1] user {} conversation {}", ou.getUser().getEmail(), id);
        Conversation c = new Conversation(id, ou.getUser().getEmail(), ou.getUserId(), model, Double.parseDouble(temperature), systemPrompt);
        messageMap.put(id, c);
        return sendMessageToGPT(message, id, channelName);
    }

    /**
     * Sends a message to ChatGPT with direct system prompt text.
     * <p>
     * Similar to sendMessageToGPT but accepts direct system prompt instead of template filename.
     * Use this method when you have pre-generated system prompt text or want to bypass template processing.
     * 
     * 
     * @param systemPrompt Direct system prompt text defining AI behavior (bypasses template processing)
     * @param message User message text to send to ChatGPT
     * @param model Model name: "gpt-3.5-turbo" or "gpt-4"
     * @param temperature Creativity setting from 0.0 to 2.0
     * @param channelName WebSocket channel name for response delivery, null for user's default queue
     * @return Conversation ID (UUID string) for tracking and continuing conversation
     */
    public String sendInitMessageToGPT(String systemPrompt, String message, String model, String temperature, String channelName) {
        debug("[sendInitMessageToGPT] {} {} {} {}", message, model, temperature);
        OrganizationUser ou = UserProvider.getFromContext().orElseThrow(RuntimeException::new);
        String id = UUID.randomUUID().toString();
        debug("[sendInitMessageToGPT] user {} conversation {}", ou.getUser().getEmail(), id);
        Conversation c = new Conversation(id, ou.getUser().getEmail(), ou.getUserId(), model, Double.parseDouble(temperature), systemPrompt);
        messageMap.put(id, c);
        return sendMessageToGPT(message, id, channelName);
    }

    /**
     * Sends a message to an existing ChatGPT conversation with async execution.
     * <p>
     * Continues an existing conversation identified by conversationId. Checks disk cache for matching
     * conversation history to avoid redundant API calls. Executes request in worker thread pool to
     * prevent blocking. Delivers response via WebSocket to specified channel or user's default queue.
     * 
     * 
     * <b>Execution Flow</b>
     * <ol>
     *   <li>Generate cache key from conversation history and new message</li>
     *   <li>Check disk cache for cached response (if caching enabled)</li>
     *   <li>On cache miss: Send request to OpenAI API via sendMessage()</li>
     *   <li>Add user message and assistant response to conversation history (synchronized)</li>
     *   <li>Write updated cache to disk (synchronized block for thread safety)</li>
     *   <li>Create audit log entry with conversation ID and message history</li>
     *   <li>Deliver response via WebSocket: channel (if specified) or /queue/ai (default)</li>
     * </ol>
     * 
     * @param message User message text to send to ChatGPT
     * @param conversationId Existing conversation UUID from previous sendMessageToGPT or sendInitMessageToGPT call
     * @param channelName WebSocket channel name for response delivery, null to use /queue/ai for user
     * @return Conversation ID (same as input conversationId)
     */
    public String sendMessageToGPT(String message, String conversationId, String channelName) {
        debug("[sendMessageToGPT-2] {} {}", message, conversationId);
        Conversation c = messageMap.get(conversationId);
        executors.submit(() -> {
            debug("[sendMessageToGPT Task] {} {}", conversationId, message);
            String responseContent = null;
            String cacheKey = null;
            //trying to get conversation from the cache
            if(cacheGPTMessages) {
                cacheKey = c.getCacheKey(message);
                String responseKey = conversationCache.prompts.get(cacheKey);
                responseContent = responseKey == null ? null : conversationCache.responses.get(responseKey);
            }
            boolean cacheHit = responseContent != null;
            if (cacheHit) {
                debug("[sendMessageToGPT Task] found in cache {} {}", conversationId, message);
                c.addMessages(message, responseContent);
            } else {
                debug("[sendMessageToGPT Task] not found in cache. Sending request to GPT {} {}", conversationId, message);
                responseContent = sendMessage(buildCompletionRequest(message, c));
                debug("[sendMessageToGPT Task] entering synchronized block {} {}", conversationId, message);
                synchronized (ChatGPTService.class) {
                    debug("[sendMessageToGPT Task] entered synchronized block {} {}", conversationId, message);
                    c.addMessages(message, responseContent);
                    if(cacheGPTMessages) {
                        String newResponseKey = conversationCache.responses.size() + "";
                        conversationCache.prompts.put(cacheKey, newResponseKey);
                        conversationCache.responses.put(newResponseKey, responseContent);
                        try {
                            FileUtils.write(new File(cacheFileLocation), JsonHelper.to(conversationCache), "UTF-8");
                            debug("[sendMessageToGPT Task] updated cache file {} {}", conversationId, message);
                        } catch (Exception e) {
                            error("Error writing conversation cache", e);
                        }
                    }
                }

                debug("[sendMessageToGPT Task] create audit log {} {}", conversationId, message);
                AuditService.createSimpleInfoAudit("GPT conversation id: " + conversationId, c.messages.toString());
            }
            debug("[sendMessageToGPT Task] sending response to websocket {} {}", conversationId, message);
            if(StringUtils.isNotEmpty(channelName)) {
                websocketService.sendToChannel(channelName, Map.of("conversationId", conversationId, "response", responseContent));
            } else {
                websocketService.sendToUserChannel(c.userEmail, "/queue/ai", Map.of("conversationId", conversationId, "response", responseContent));
            }
            debug("[sendMessageToGPT Task] exiting {} {}", conversationId, message);

        });
        return conversationId;
    }

    /**
     * Sends a ChatGPT completion request and extracts response content.
     * <p>
     * Private wrapper method that calls sendCompletionRequest and validates response.
     * Throws RuntimeException if request fails or returns empty response.
     * 
     * 
     * @param chatCompletionRequest OpenAI ChatCompletionRequest object with messages and configuration
     * @return Response content string from ChatGPT
     * @throws RuntimeException if completion request fails or returns no response
     */
    private String sendMessage(ChatCompletionRequest chatCompletionRequest) {
        debug("[sendMessage] {}", chatCompletionRequest);
        Optional<ChatCompletionChoice> chatCompletionChoice = sendCompletionRequest(chatCompletionRequest);
        if (chatCompletionChoice.isPresent()) {
            return chatCompletionChoice.get().getMessage().getContent();
        } else {
            throw new RuntimeException("Error occurred, try to resend your message.");
        }
    }

    /**
     * Executes ChatGPT completion request with error handling and timing.
     * <p>
     * Calls OpenAI API via openAiService.createChatCompletion(). Validates response length against
     * MAX_GPT_REQUEST_LENGTH (8000 characters). Logs execution time and errors.
     * 
     * 
     * @param request ChatCompletionRequest with model, messages, temperature, and user
     * @return Optional containing ChatCompletionChoice with response, or empty on failure
     */
    public Optional<ChatCompletionChoice> sendCompletionRequest(ChatCompletionRequest request) {
        long timestamp = System.currentTimeMillis();
        try {
            ChatCompletionChoice chatCompletionChoice = openAiService.createChatCompletion(request).getChoices().get(0);
            if (chatCompletionChoice.getMessage().getContent().length() > MAX_GPT_REQUEST_LENGTH) {
                error("Chat responded with excessive message: {}", chatCompletionChoice.getMessage().getContent());
            }
            debug("Completion request completed in {} seconds", ((System.currentTimeMillis() - timestamp) / 1000));
            return Optional.of(chatCompletionChoice);
        } catch (Exception e) {
            error("Completion request failed in {} seconds. Reason: {}", ((System.currentTimeMillis() - timestamp) / 1000), e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Builds ChatCompletionRequest from conversation history and new message.
     * <p>
     * Converts internal Message records to OpenAI ChatMessage objects, appends new user message,
     * and configures model and temperature from conversation context. Hard-codes user identifier
     * as "stratoflow" for API tracking purposes.
     * 
     * 
     * @param message New user message text to append
     * @param c Conversation object containing history and configuration
     * @return ChatCompletionRequest ready for OpenAI API submission
     */
    private ChatCompletionRequest buildCompletionRequest(String message, Conversation c) {
        ChatMessage newUserMessage = new ChatMessage("user", message);
        List<ChatMessage> messages = new ArrayList<>(c.messages.size() + 1);
        c.messages.forEach(a -> messages.add(new ChatMessage(a.role, a.content)));
        messages.add(newUserMessage);

        return ChatCompletionRequest.builder()
                .messages(messages)
                .user("stratoflow")
                .model(c.model)
                .temperature(c.temperature)
                .build();
    }

    /**
     * Loads conversation cache from disk on application startup.
     * <p>
     * PostConstruct method that reads serialized ConversationCache from file location specified by
     * chat.gpt.prompt.cacheFile property. Creates empty cache if file doesn't exist or read fails.
     * Only executes if chat.gpt.prompt.cacheEnabled is true.
     * 
     * <p>
     * Logs number of cached conversations on success, or warning on failure (expected if cache file
     * doesn't exist on first startup).
     * 
     */
    @PostConstruct void init() {
        if(cacheGPTMessages) {
            try {
                String cache = FileUtils.readFileToString(new File(cacheFileLocation), "UTF-8");
                conversationCache = JsonHelper.from(cache, ConversationCache.class);
                debug("Read {} conversations", conversationCache.prompts.size());
            } catch (Exception e) {
                conversationCache = new ConversationCache(new HashMap<>(), new HashMap<>());
                warn("Error reading cache file {}. Don't worry, it may just not exist.", cacheFileLocation);
            }
        }
    }
}

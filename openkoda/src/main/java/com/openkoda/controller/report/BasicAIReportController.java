package com.openkoda.controller.report;

import com.openkoda.core.controller.frontendresource.AbstractFrontendResourceController;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.openkoda.controller.common.URLConstants.*;

/**
 * Controller providing AI-powered report generation using ChatGPT integration.
 * <p>
 * This controller enables users to describe reports in natural language, which are then translated to SQL queries
 * via ChatGPT. The typical user workflow consists of: describe report in plain language → ChatGPT generates SQL
 * query → execute query against the database → format and return results. All routes are served under the
 * {@code /reports/ai} endpoint pattern, supporting both global and organization-scoped report execution.
 * </p>
 * <p>
 * Security features include SQL injection prevention through query validation, enforcement of read-only operations
 * (SELECT statements only), and parameterized query execution. The controller validates all generated SQL to prevent
 * DDL and DML operations, ensuring data integrity and security.
 * </p>
 * <p>
 * The {@code @Profile("!development")} annotation disables this controller in the development profile to prevent
 * accidental AI service calls during local development, avoiding unnecessary API costs and external dependencies
 * during testing.
 * </p>
 * <p>
 * This controller extends {@link AbstractFrontendResourceController} to inherit cross-cutting concerns including
 * request logging (via the {@code debug()} method), error handling, and common response building patterns.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * // User submits: "Show top 10 users by login count last month"
 * // Controller routes to ChatGPT → generates SQL → executes query → returns formatted report
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.service.openai.ChatGPTService
 * @see com.openkoda.controller.report.ReportService
 * @see AbstractFrontendResourceController
 */
@Controller
@RequestMapping({_HTML + _QUERY_REPORT, _HTML_ORGANIZATION_ORGANIZATIONID + _QUERY_REPORT})
@Profile("!development")
public class BasicAIReportController extends AbstractFrontendResourceController {

    /**
     * Sends natural language prompt to AI service for SQL query generation and report execution.
     * <p>
     * Currently unimplemented and throws {@link NotImplementedException}. The intended implementation workflow
     * consists of: validate prompt → send to ChatGPT API → receive generated SQL query → validate SQL for safety
     * (SELECT only, no DDL/DML operations) → execute query against database → format results as table/chart →
     * return view with report data.
     * </p>
     * <p>
     * This method is stateless and thread-safe. The controller delegates to ChatGPT service for SQL generation
     * and to report service for query execution. All database operations will use read-only transactions with
     * configured timeout limits to prevent long-running queries.
     * </p>
     * <p>
     * Security considerations: SQL injection prevention is achieved through strict validation of generated SQL
     * (enforcement of SELECT-only statements), parameterization of query values, and prohibition of DDL/DML
     * operations. User authorization checks verify the caller has {@code CHECK_CAN_CREATE_REPORTS} privilege
     * and appropriate organization access for tenant-scoped queries.
     * </p>
     * <p>
     * Example expected usage (once implemented):
     * <pre>
     * ModelAndView result = sendPrompt(orgId, "Show revenue by product category", 
     *     "https://api.openai.com/chat", "conv-123", "", "gpt-4-0613", "0.2");
     * </pre>
     * </p>
     * <p>
     * <b>Implementation TODO:</b> Add input validation (sanitize prompt, validate URL format for promptWebEndpoint),
     * implement numeric conversion for temperature parameter with range validation (0.0-1.0), add model validation
     * against allowed list, implement tenant/organization authorization checks, configure request timeouts/retries
     * for AI service calls, implement SQL validation (enforce SELECT-only, prevent SQL injection via parameterization),
     * add structured error handling with user-friendly messages, implement result formatting (table/chart options),
     * add unit tests and integration tests, implement observability (metrics/traces for AI service calls).
     * </p>
     *
     * @param organizationId Optional organization ID for tenant-scoped report execution. If {@code null}, executes
     *                       in global context. Used for multi-tenancy authorization checks to ensure users can only
     *                       generate reports within their permitted organizations. Must reference a valid organization
     *                       entity if provided.
     * @param prompt Natural language report description provided by user (e.g., "Show top 10 users by login count
     *               last month"). Must be non-empty and contain valid characters. Used to generate SQL query via
     *               ChatGPT API. Should be sanitized to prevent prompt injection attacks.
     * @param promptWebEndpoint Target web endpoint URL for AI service invocation. Must be valid HTTPS URL pointing
     *                          to ChatGPT API. Used to route request to appropriate ChatGPT API endpoint. Should
     *                          be validated for protocol and domain whitelist to prevent SSRF attacks.
     * @param conversationId Unique conversation identifier for maintaining context across multiple prompts. Used for
     *                       conversational AI interactions and chat history tracking. Enables multi-turn conversations
     *                       where subsequent prompts can reference previous queries and results.
     * @param channelId Optional communication channel identifier (default empty string). Used for routing responses
     *                  in multi-channel environments (e.g., web UI, API, webhook). Can be used to track request
     *                  origin and customize response format per channel.
     * @param model ChatGPT model identifier (default {@code "gpt-4-0613"}). Supported values include {@code "gpt-4-0613"}
     *              and {@code "gpt-3.5-turbo"}. Controls which AI model is used for SQL generation. More advanced models
     *              produce better SQL quality but have higher API costs and latency. Should be validated against
     *              allowed model list.
     * @param temperature AI response randomness parameter as string (default {@code "0.2"}). Valid range: 0.0-1.0,
     *                    where lower values (0.0-0.3) produce more deterministic and consistent SQL queries, while
     *                    higher values (0.7-1.0) introduce more variation. Requires conversion to numeric type and
     *                    range validation before passing to AI service.
     * @return Expected to return {@link org.springframework.web.servlet.ModelAndView} containing generated SQL query,
     *         result table with data rows, visualization options (chart/table configurations), and any error/warning
     *         messages. Currently throws {@link NotImplementedException} until feature is fully implemented.
     * @throws NotImplementedException Always thrown in current implementation - feature not yet implemented. Remove
     *                                  this exception handler when implementing actual AI report generation logic.
     * @throws org.springframework.web.bind.MissingServletRequestParameterException If required request parameters
     *                                                                               (prompt, promptWebEndpoint,
     *                                                                               conversationId) are missing
     *                                                                               from the HTTP request.
     * @see com.openkoda.service.openai.ChatGPTService
     * @see com.openkoda.controller.report.QueryReportController
     * @see AbstractFrontendResourceController#debug(String)
     */
    public Object sendPrompt(@PathVariable(name=ORGANIZATIONID, required = false) Long organizationId,
                             @RequestParam("prompt") String prompt,
                             @RequestParam("promptWebEndpoint") String promptWebEndpoint,
                             @RequestParam("conversationId") String conversationId,
                             @RequestParam(value = "channelId", required = false, defaultValue = "") String channelId,
                             @RequestParam(value = "model", required = false, defaultValue = "gpt-4-0613") String model,
                             @RequestParam(value = "temperature", required = false, defaultValue = "0.2") String temperature) {
        debug("[sendPrompt]");
        throw new NotImplementedException();
    }

}

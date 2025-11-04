# Integration Module

## Overview

The Integration module provides connectors and adapters for third-party services including Trello, GitHub, Jira, Basecamp, and OAuth providers. It implements OAuth 2.0 flows, REST API consumers using Spring RestTemplate, and per-organization integration configuration management.

**Position in Architecture**: External integration layer - connects OpenKoda to third-party APIs.

## Module Structure

```
com.openkoda.integration/
├── consumer/              (REST API consumers for third-party services)
│   ├── BaseConsumer       (Base REST consumer with error handling)
│   ├── TrelloConsumer     (Trello API integration)
│   ├── GitHubConsumer     (GitHub API integration)
│   ├── JiraConsumer       (Jira API integration)
│   └── BasecampConsumer   (Basecamp API integration)
├── controller/            (OAuth callback controllers)
├── form/                  (Integration configuration forms)
├── model/                 (Integration configuration entities)
└── service/               (Integration service contracts)
```

## Configuration

### OAuth Configuration

Configure OAuth client credentials in `application.properties`:

```properties
# Trello OAuth
trello.api.key=your-api-key
trello.oauth.secret=your-secret

# GitHub OAuth
github.client.id=your-client-id
github.client.secret=your-secret

# Jira OAuth
jira.consumer.key=your-consumer-key
jira.private.key=path/to/private-key.pem
```

### RestTemplate Configuration

REST consumers use Spring RestTemplate with custom interceptors:

```java
@Bean
public RestTemplate integrationRestTemplate() {
    RestTemplate template = new RestTemplate();
    template.getInterceptors().add(loggingInterceptor);
    return template;
}
```

### Per-Organization Configuration

Integration settings are stored per organization in configuration entities.

## Key Components

### BaseConsumer (REST API Consumer Base)

**Purpose**: Base class for REST API consumers with common error handling and authentication.

```java
public abstract class BaseConsumer extends LoggingComponentWithRequestId {
    protected RestTemplate restTemplate;
    
    protected <T> ResponseEntity<T> get(String url, Class<T> responseType) {
        // Common GET logic with error handling
    }
}
```

### TrelloConsumer

**Purpose**: Trello API integration for board and card management.

```java
@Service
public class TrelloConsumer extends BaseConsumer {
    public List<Board> getBoards(String token);
    public Card createCard(String boardId, CardRequest request);
}
```

### GitHubConsumer

**Purpose**: GitHub API integration for repository and issue management.

```java
@Service
public class GitHubConsumer extends BaseConsumer {
    public List<Repository> getRepositories(String accessToken);
    public Issue createIssue(String repo, IssueRequest request);
}
```

### JiraConsumer

**Purpose**: Jira API integration for issue tracking and project management.

```java
@Service
public class JiraConsumer extends BaseConsumer {
    public List<Project> getProjects(String instanceUrl, String token);
    public Issue createIssue(IssueRequest request);
}
```

### OAuth Callback Controllers

**Purpose**: Handle OAuth 2.0 authorization code flow callbacks.

```java
@Controller
public class TrelloOAuthController {
    @GetMapping("/callback/trello")
    public String handleCallback(@RequestParam String code) {
        // Exchange code for access token
        // Store token in organization configuration
        return "redirect:/integrations";
    }
}
```

## API Usage Examples

### Example 1: Trello Integration Setup

```java
// Initiate OAuth flow
String authUrl = trelloService.getAuthorizationUrl(organizationId);
return "redirect:" + authUrl;
```

### Example 2: Creating GitHub Issue

```java
@Autowired
private GitHubConsumer githubConsumer;

IssueRequest request = new IssueRequest("Bug Report", "Description");
Issue issue = githubConsumer.createIssue("owner/repo", request);
```

### Example 3: Fetching Jira Projects

```java
@Autowired
private JiraConsumer jiraConsumer;

List<Project> projects = jiraConsumer.getProjects(
    jiraInstanceUrl, 
    accessToken
);
```

## OAuth Flow Implementation

### Authorization Flow

```
1. User clicks "Connect Trello"
   ↓
2. Redirect to Trello authorization URL
   ↓
3. User grants permission
   ↓
4. Trello redirects to /callback/trello?code=...
   ↓
5. Exchange code for access token
   ↓
6. Store token in IntegrationConfiguration entity
   ↓
7. Integration ready for use
```

### Token Storage

Integration tokens are stored per organization:

```java
@Entity
public class IntegrationConfiguration extends OrganizationRelatedEntity {
    private String provider;  // "trello", "github", etc.
    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;
}
```

### Token Refresh

Consumers automatically refresh expired tokens:

```java
protected String getValidToken(Long organizationId) {
    IntegrationConfig config = configRepository.findByOrgAndProvider(orgId, "github");
    if (config.isExpired()) {
        config = refreshToken(config);
    }
    return config.getAccessToken();
}
```

## Dependencies

### Internal Dependencies

- **Core module**: Uses logging, error handling, security
- **Model module**: Integration configuration entities
- **Service module**: Integration service orchestration

### External Dependencies

- Spring Web (RestTemplate, @Controller)
- OAuth2 Client libraries
- Third-party API SDKs (optional, for type-safe clients)
- Jackson (JSON serialization)

## Relationships with Other Modules

```
Controller Layer
    ↓ initiates
Integration Module
    ↓ calls
External APIs (Trello, GitHub, Jira, etc.)
```

**Integration → Core**: Uses LoggingComponentWithRequestId, exception handling

**Integration → Model**: Stores configuration in IntegrationConfiguration entities

**Integration → Service**: Service layer orchestrates integration workflows

**Controller → Integration**: Controllers trigger OAuth flows and API calls

## Setup and Testing

### OAuth Provider Setup

1. Register application with each provider:
   - Trello: https://trello.com/app-key
   - GitHub: https://github.com/settings/developers
   - Jira: https://developer.atlassian.com/console

2. Configure callback URLs:
   ```
   https://your-domain.com/callback/trello
   https://your-domain.com/callback/github
   https://your-domain.com/callback/jira
   ```

3. Add credentials to `application.properties`

### Running Integration Tests

```bash
cd openkoda
mvn test -Dtest=com.openkoda.integration.**
```

### Integration Test Pattern

```java
@SpringBootTest
public class TrelloConsumerTest extends AbstractTest {
    @Autowired
    private TrelloConsumer consumer;
    
    @Test
    public void testGetBoards() {
        // Mock RestTemplate responses
        List<Board> boards = consumer.getBoards(testToken);
        assertNotNull(boards);
    }
}
```

## Additional Resources

- [BaseConsumer.java](consumer/BaseConsumer.java) - REST consumer base class
- [TrelloConsumer.java](consumer/TrelloConsumer.java) - Trello integration
- [OAuth Controllers](controller/) - Authorization callback handlers
- [Integration Configuration](model/IntegrationConfiguration.java) - Token storage

## Error Handling

### API Error Responses

Consumers handle common API error scenarios:

```java
try {
    ResponseEntity<Board> response = restTemplate.getForEntity(url, Board.class);
    return response.getBody();
} catch (HttpClientErrorException.Unauthorized e) {
    throw new ApiException("Invalid or expired token");
} catch (HttpClientErrorException.NotFound e) {
    throw new ApiException("Resource not found");
} catch (RestClientException e) {
    error("API call failed", "url", url, "error", e.getMessage());
    throw new ApiException("Integration API unavailable");
}
```

### Token Expiration

Automatically refresh expired tokens before API calls:

```java
if (token.isExpired()) {
    token = oauthService.refreshToken(token.getRefreshToken());
    configRepository.save(token);
}
```

## Security Considerations

### Token Storage

- Access tokens are encrypted in the database
- Refresh tokens are stored securely
- Tokens are organization-scoped

### API Rate Limiting

Consumers implement rate limiting to respect API quotas:

```java
@RateLimiter(name = "github", fallbackMethod = "rateLimitFallback")
public List<Repository> getRepositories(String token) {
    // API call
}
```

### HTTPS Enforcement

All API calls use HTTPS for secure communication.

## Common Integration Patterns

### Webhook Registration

```java
public void registerWebhook(String callbackUrl, String accessToken) {
    WebhookRequest request = new WebhookRequest(callbackUrl);
    restTemplate.postForObject(webhookUrl, request, Webhook.class);
}
```

### Batch Operations

```java
public List<Issue> createIssuesBatch(List<IssueRequest> requests) {
    return requests.stream()
        .map(this::createIssue)
        .collect(Collectors.toList());
}
```

---

**Source**: openkoda/src/main/java/com/openkoda/integration/  
**Package Documentation**: See package-info.java files in subpackages  
**API Reference**: See [Javadoc](../../../../target/site/apidocs/com/openkoda/integration/package-summary.html)

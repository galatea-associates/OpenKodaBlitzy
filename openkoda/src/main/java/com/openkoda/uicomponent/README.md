# UIComponent Module

## Overview

The UIComponent module provides UI-facing service interfaces and GraalVM JavaScript integration for executing dynamic frontend logic. It includes service contracts for data operations, integrations, media handling, messaging, OpenAI interactions, and system operations, along with WebSocket support for live updates.

**Position in Architecture**: UI service layer - bridges backend services and frontend JavaScript code.

## Module Structure

```
com.openkoda.uicomponent/
├── annotation/            (Service interface annotations)
├── live/                  (WebSocket live update services)
│   └── LiveService        (Real-time data push to clients)
└── [root services]        (Service interfaces and GraalVM integration)
    ├── DataServices       (CRUD operations exposed to JS)
    ├── IntegrationServices (Third-party API access)
    ├── MediaServices      (File upload and media handling)
    ├── MessagesServices   (I18n message lookup)
    ├── OpenAIServices     (ChatGPT integration)
    ├── SystemServices     (System utilities)
    ├── JsFlowRunner       (GraalVM JS execution)
    └── FileSystemImpl     (Polyglot filesystem access)
```

## Configuration

### GraalVM Context Configuration

Configure GraalVM polyglot context for JavaScript execution:

```java
@Bean
public Context graalVmContext() {
    return Context.newBuilder("js")
        .allowAllAccess(false)  // Security: restrict access
        .allowIO(IOAccess.newBuilder()
            .fileSystem(fileSystemImpl)
            .build())
        .build();
}
```

### WebSocket Configuration

Enable WebSocket support for live updates:

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveService, "/ws/live");
    }
}
```

## Key Components

### Service Interfaces

**Purpose**: Define contracts for UI operations exposed to JavaScript code.

#### DataServices

CRUD operations for dynamic entities and standard entities:

```java
public interface DataServices {
    Object findById(String entityType, Long id);
    List<Object> findAll(String entityType);
    Object save(String entityType, Object entity);
    void delete(String entityType, Long id);
}
```

#### IntegrationServices

Access to third-party integrations:

```java
public interface IntegrationServices {
    List<Object> getTrelloBoards(Long organizationId);
    Object createGitHubIssue(String repo, String title, String body);
}
```

#### OpenAIServices

ChatGPT integration for AI-powered features:

```java
public interface OpenAIServices {
    String sendMessage(Long conversationId, String message);
    String generateCompletion(String prompt);
}
```

### GraalVM JavaScript Integration

#### JsFlowRunner

**Purpose**: Execute JavaScript Flow pipelines in GraalVM Context.

```java
@Service
public class JsFlowRunner extends LoggingComponentWithRequestId {
    public Object executeFlow(String jsCode, Map<String, Object> context) {
        try (Context graalContext = Context.newBuilder("js").build()) {
            Value bindings = graalContext.getBindings("js");
            context.forEach(bindings::putMember);
            
            Value result = graalContext.eval("js", jsCode);
            return result.as(Object.class);
        }
    }
}
```

#### FileSystemImpl

**Purpose**: Polyglot filesystem exposing ServerJs code to JavaScript context.

```java
@Service
public class FileSystemImpl implements FileSystem {
    @Override
    public Path parsePath(String path) {
        // Restrict access to ServerJs directory
        return serverJsBasePath.resolve(path);
    }
    
    @Override
    public void checkAccess(Path path, Set<AccessMode> modes) {
        // Security checks
    }
}
```

### LiveService (WebSocket)

**Purpose**: Real-time data push to connected clients.

```java
@Service
public class LiveService extends LoggingComponentWithRequestId 
    implements WebSocketHandler {
    
    public void broadcast(String channel, Object message) {
        sessions.stream()
            .filter(s -> s.isSubscribed(channel))
            .forEach(s -> s.sendMessage(serialize(message)));
    }
}
```

## API Usage Examples

### Example 1: Executing JavaScript Flow

```java
String jsCode = """
    const user = dataServices.findById('User', userId);
    return user.email;
""";

Object result = jsFlowRunner.executeFlow(jsCode, Map.of(
    "userId", 123L,
    "dataServices", dataServicesImpl
));
```

### Example 2: Live Update Broadcasting

```java
@Autowired
private LiveService liveService;

liveService.broadcast("notifications", new Notification("New message"));
```

### Example 3: ChatGPT Integration

```java
@Autowired
private OpenAIServices openAIServices;

String response = openAIServices.sendMessage(conversationId, "Hello AI!");
```

## GraalVM JavaScript Context

### Available Bindings

JavaScript code has access to service interfaces:

```javascript
// dataServices - CRUD operations
const user = dataServices.findById('User', 123);
dataServices.save('User', user);

// integrationServices - Third-party APIs
const boards = integrationServices.getTrelloBoards(orgId);

// openAIServices - ChatGPT
const response = openAIServices.generateCompletion(prompt);

// messagesServices - i18n
const message = messagesServices.get('welcome.message');

// systemServices - Utilities
const timestamp = systemServices.currentTimestamp();
```

### Security Restrictions

GraalVM context is sandboxed:

- No direct filesystem access (only via FileSystemImpl)
- No network access
- No system property access
- Limited Java class access

### Value Conversion

GraalVM automatically converts between Java and JavaScript types:

```java
// Java → JavaScript
context.putMember("javaList", List.of(1, 2, 3));
// JavaScript sees: [1, 2, 3]

// JavaScript → Java
Value jsResult = context.eval("js", "[1, 2, 3]");
List<Integer> javaList = jsResult.as(new TypeLiteral<List<Integer>>(){});
```

## Dependencies

### Internal Dependencies

- **Core module**: Uses Flow, logging, security
- **Service module**: Implements service interfaces
- **Model module**: Data services operate on entities

### External Dependencies

- GraalVM SDK 22.3.1 (JavaScript engine)
- Spring WebSocket (live updates)
- Jackson (JSON serialization for WebSocket messages)

## Relationships with Other Modules

```
Frontend (JavaScript)
    ↓ calls via GraalVM
UIComponent Module
    ↓ delegates to
Service Module
```

**UIComponent → Service**: Delegates operations to service layer

**UIComponent → Core**: Uses Flow for JavaScript execution

**Frontend → UIComponent**: JavaScript code calls service interfaces

**UIComponent → WebSocket**: Live updates push data to browser

## Setup and Testing

### GraalVM Installation

Ensure GraalVM JDK is used:

```bash
java -version
# Should show: GraalVM CE 22.3.1
```

### WebSocket Testing

Test WebSocket connection:

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/live');
ws.onmessage = (event) => {
    console.log('Received:', JSON.parse(event.data));
};
```

### Running UIComponent Tests

```bash
cd openkoda
mvn test -Dtest=com.openkoda.uicomponent.**
```

### Integration Test Pattern

```java
@SpringBootTest
public class JsFlowRunnerTest extends AbstractTest {
    @Autowired
    private JsFlowRunner runner;
    
    @Test
    public void testExecuteSimpleFlow() {
        Object result = runner.executeFlow(
            "return 2 + 2;", 
            Collections.emptyMap()
        );
        assertEquals(4, result);
    }
}
```

## Additional Resources

- [JsFlowRunner.java](JsFlowRunner.java) - GraalVM JavaScript execution
- [DataServices.java](DataServices.java) - CRUD service interface
- [LiveService.java](live/LiveService.java) - WebSocket live updates
- [FileSystemImpl.java](FileSystemImpl.java) - Polyglot filesystem

## JavaScript Flow Examples

### Example 1: Data Manipulation

```javascript
// Find all users in organization
const users = dataServices.findAll('User');

// Filter and transform
const activeUsers = users.filter(u => u.active);
const emails = activeUsers.map(u => u.email);

return emails;
```

### Example 2: Integration Workflow

```javascript
// Fetch from GitHub
const repos = integrationServices.getGitHubRepositories(orgId);

// Create Trello cards for each repo
repos.forEach(repo => {
    integrationServices.createTrelloCard({
        name: repo.name,
        description: repo.description
    });
});

return { processed: repos.length };
```

### Example 3: AI-Assisted Processing

```javascript
const document = dataServices.findById('Document', docId);

// Use ChatGPT to summarize
const summary = openAIServices.generateCompletion(
    `Summarize: ${document.content}`
);

document.summary = summary;
dataServices.save('Document', document);

return summary;
```

## Performance Considerations

### Context Reuse

Create context once and reuse for multiple executions:

```java
private Context sharedContext;

@PostConstruct
public void init() {
    sharedContext = Context.newBuilder("js")
        .allowAllAccess(false)
        .build();
}
```

### Asynchronous Execution

Run long JavaScript operations asynchronously:

```java
@Async
public CompletableFuture<Object> executeFlowAsync(String jsCode) {
    return CompletableFuture.supplyAsync(() -> 
        jsFlowRunner.executeFlow(jsCode, context)
    );
}
```

## WebSocket Message Protocol

### Client → Server

```json
{
    "action": "subscribe",
    "channel": "notifications",
    "organizationId": 123
}
```

### Server → Client

```json
{
    "channel": "notifications",
    "type": "NEW_MESSAGE",
    "data": {
        "id": 456,
        "content": "Hello",
        "timestamp": "2024-11-04T14:00:00Z"
    }
}
```

---

**Source**: openkoda/src/main/java/com/openkoda/uicomponent/  
**Package Documentation**: See package-info.java files in subpackages  
**API Reference**: See [Javadoc](../../../../target/site/apidocs/com/openkoda/uicomponent/package-summary.html)

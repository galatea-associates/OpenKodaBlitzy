# Service Module

## Overview

The Service module implements the business logic layer of OpenKoda, orchestrating operations between the controller and repository layers. It provides core services for organization management, user administration, dynamic entity generation, external integrations (OpenAI, OAuth), schema migrations, and data export/import.

**Position in Architecture**: Business logic layer - mediates between controllers and repositories.

## Module Structure

```
com.openkoda.service/
├── autocomplete/          (Autocomplete reflection helpers)
├── captcha/               (reCAPTCHA verification)
├── csv/                   (CSV file assembly)
├── dynamicentity/         (Runtime dynamic-entity subsystem with Byte Buddy)
├── export/                (Export/import orchestration for components)
├── map/                   (Geospatial parsing - WKT POINT via JTS)
├── notification/          (Notification lifecycle management)
├── openai/                (ChatGPT integration and conversation management)
├── organization/          (Organization provisioning and tenant management)
├── role/                  (Role modification reconciliation)
├── upgrade/               (Database version migration orchestration)
├── user/                  (User management, API keys, privileges, tokens)
└── [root services]        (Services.java aggregator + core services)
```

## Configuration

### Service Bean Registration

Services are auto-discovered via component scanning:

```java
@ComponentScan("com.openkoda.service")
```

### Transaction Management

Service layer methods use declarative transactions:

```java
@Service
@Transactional
public class MyService {
    // Methods automatically wrapped in transactions
}
```

### External API Configuration

Configure external service credentials in `application.properties`:

```properties
openai.api.key=sk-...
recaptcha.secret.key=...
```

## Key Services

### Services.java (Service Aggregator)

**Purpose**: Convenience aggregator exposing 50+ service beans for legacy code patterns.

```java
@Autowired
private Services services;

User user = services.user.findByEmail(email);
```

### DynamicEntityRegistrationService (Runtime Entity Generation)

**Purpose**: Byte Buddy-based runtime JPA entity generation from form definitions.

**Workflow**: Form → DynamicEntityDescriptor → Byte Buddy class generation → JPA registration

```java
DynamicEntityDescriptor descriptor = factory.createDescriptor(formEntity);
dynamicEntityService.createOrUpdateTable(descriptor, organizationId);
```

### OrganizationService (Tenant Management)

**Purpose**: Organization provisioning, schema management, and tenant isolation.

```java
Organization org = organizationService.createOrganization("TenantName");
```

### ChatGPTService (OpenAI Integration)

**Purpose**: OpenAI API integration with conversation management and disk-backed caching.

```java
String response = chatGPTService.sendMessage(conversationId, userMessage);
```

### DbVersionService (Schema Migration)

**Purpose**: SQL-based schema migration with version tracking and transactional execution.

```java
dbVersionService.upgradeDatabase(sqlBlocks);
```

### UserService (User Management)

**Purpose**: User CRUD operations, API key management, privilege assignment.

```java
User user = userService.createUser(email, password, organizationId);
```

## API Usage Examples

### Example 1: Creating an Organization

```java
Organization org = organizationService.createOrganization("CompanyName");
organizationService.addUserToOrganization(userId, org.getId());
```

### Example 2: Dynamic Entity Generation

```java
DynamicEntityDescriptor descriptor = descriptorFactory.create(formDefinition);
Class<?> entityClass = dynamicEntityService.registerEntity(descriptor);
```

### Example 3: Database Migration

```java
List<String> sqlBlocks = dbVersionService.parseSqlFile("migration.sql");
dbVersionService.executeMigration(sqlBlocks);
```

## Dependencies

### Internal Dependencies

- **Core module**: Uses Flow pipelines, caching, security, logging
- **Model module**: Operates on entity definitions (User, Organization, Role)
- **Repository module**: Delegates data access to repository layer

### External Dependencies

- Byte Buddy 1.x (runtime class generation for dynamic entities)
- OpenAI Java SDK (ChatGPT integration)
- JTS Topology Suite (geospatial WKT parsing)
- Apache Commons CSV (CSV export/import)
- Spring Data JPA (repository integration)

## Relationships with Other Modules

```
Controller Layer
    ↓ calls
Service Layer (this module)
    ↓ calls
Repository Layer
```

**Service → Core**: Services use Flow, LoggingComponentWithRequestId, RequestSessionCacheService

**Service → Model**: Services operate on entities (User, Organization, DynamicEntity)

**Service → Repository**: Services delegate persistence to repositories

**Controller → Service**: Controllers call service methods for business operations

## Setup and Testing

### Module-Specific Setup

1. Configure external API keys in `application.properties`:
   ```properties
   openai.api.key=your-key-here
   ```

2. Enable service layer in Spring Boot application:
   ```java
   @SpringBootApplication
   @ComponentScan(basePackages = {"com.openkoda.service"})
   public class Application { }
   ```

### Running Service Tests

```bash
cd openkoda
mvn test -Dtest=com.openkoda.service.**
```

### Integration Test Pattern

```java
@SpringBootTest
public class OrganizationServiceTest extends AbstractTest {
    @Autowired
    private OrganizationService service;
    
    @Test
    public void testCreateOrganization() {
        Organization org = service.createOrganization("TestOrg");
        assertNotNull(org.getId());
    }
}
```

## Additional Resources

- [Services.java](Services.java) - Service aggregator with 50+ bean references
- [Dynamic Entity System](dynamicentity/DynamicEntityRegistrationService.java) - Byte Buddy integration
- [ChatGPT Integration](openai/ChatGPTService.java) - OpenAI API patterns
- [Schema Migration](upgrade/DbVersionService.java) - Database version management

## Common Patterns

### Service Method Pattern

```java
@Service
@Transactional
public class MyService extends LoggingComponentWithRequestId {
    
    @Autowired
    private MyRepository repository;
    
    public Optional<MyEntity> findById(Long id) {
        debug("Finding entity", "id", id);
        return repository.findOne(id);
    }
}
```

### Exception Handling Pattern

Service methods throw domain exceptions that are caught by controller exception handlers:

```java
public Organization getOrganization(Long id) {
    return organizationRepository.findOne(id)
        .orElseThrow(() -> new ApiException("Organization not found"));
}
```

---

**Source**: openkoda/src/main/java/com/openkoda/service/  
**Package Documentation**: See package-info.java files in subpackages  
**API Reference**: See [Javadoc](../../../../target/site/apidocs/com/openkoda/service/package-summary.html)

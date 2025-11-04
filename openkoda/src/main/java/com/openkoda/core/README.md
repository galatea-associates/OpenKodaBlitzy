# Core Module

## Overview

The Core module provides the foundational framework for the entire OpenKoda application. It implements cross-cutting concerns including request tracing, auditing, caching, security, multi-tenancy, and the Flow pipeline architecture. This module contains abstract and generic framework-like classes that are used throughout the application.

**Position in Architecture**: Foundation layer - all other modules depend on Core.

## Module Structure

The Core module is organized into the following subpackages:

```
com.openkoda.core/
├── audit/              (Hibernate-based auditing with interceptors)
├── cache/              (Request-scoped and session-scoped caching)
├── configuration/      (Spring @Configuration classes and platform wiring)
├── controller/         (Controller utilities and health probes)
├── customisation/      (Runtime extension and bootstrap APIs)
├── exception/          (HTTP-aware exceptions and error resolution)
├── flow/               (Flow pipeline runtime primitives)
├── form/               (Form/FrontendMapping DSL and Spring MVC binders)
├── helper/             (Low-level utilities: UrlHelper, JsonHelper, etc.)
├── job/                (Scheduled background jobs)
├── lifecycle/          (Application lifecycle management)
├── multitenancy/       (Tenant isolation and organization-scoped operations)
├── repository/         (Repository base contracts and utilities)
├── security/           (Security primitives and authentication)
├── service/            (Core services)
└── tracker/            (Request/job-correlated tracing and debug capture)
```

## Configuration

### Spring Bean Wiring

Core components are automatically discovered through component scanning:

```java
@ComponentScan("com.openkoda.core")
```

### Auditing Setup

Enable JPA auditing with `@EnableJpaAuditing` to activate the audit interceptor:

```java
@Configuration
@EnableJpaAuditing
public class AuditConfiguration {
    // Audit interceptor automatically applied to all JPA entities
}
```

### Cache Configuration

Request-scoped caching is enabled by default. Use `RequestSessionCacheService` for memoization:

```java
cacheService.tryGet("key", () -> expensiveOperation());
```

## Core Components

### Request Tracing (tracker/)

**LoggingComponentWithRequestId**: Provides correlation IDs for request tracking across the system.

```java
debug("Processing request", "userId", userId);
```

### Auditing (audit/)

**AuditInterceptor**: Hibernate interceptor capturing entity changes in session scope.

**PropertyChangeInterceptor**: Tracks field-level modifications for audit trails.

### Flow Pipeline (flow/)

**Flow**: Functional composition DSL for controller logic with transactional execution.

```java
Flow.init()
    .thenSet("user", a -> userRepository.findById(userId))
    .thenSet("roles", a -> securityService.getUserRoles(a.result("user")))
    .execute();
```

### Form DSL (form/)

**FrontendMappingDefinition**: Reflection-based form mapping with validation lifecycle.

```java
form.populateFrom(entity);
if (form.validate()) { 
    form.populateTo(entity); 
}
```

### Multi-Tenancy (multitenancy/)

**OrganizationService**: Tenant provisioning and organization-scoped data isolation.

### Security (security/)

**SecurityService**: Authentication and privilege evaluation framework.

## API Usage Examples

### Example 1: Using Flow Pipeline for Request Handling

```java
return Flow.init(userRepository)
    .thenSet("currentUser", a -> userRepository.findById(userId))
    .thenSet("permissions", a -> getPermissions(a.result("currentUser")))
    .execute();
```

### Example 2: Request-Scoped Caching

```java
Object result = requestSessionCacheService.tryGet(
    "expensiveData", 
    () -> computeExpensiveData()
);
```

### Example 3: Request Correlation Logging

```java
@Component
public class MyService extends LoggingComponentWithRequestId {
    public void process() {
        debug("Starting process", "requestId", getRequestId());
    }
}
```

## Dependencies

### Internal Dependencies

- **model package**: Core uses model entities for security and multi-tenancy
- **repository package**: Core provides base repository contracts

### External Dependencies

- Spring Framework 6.0.x (dependency injection, MVC)
- Hibernate 6.1.x (auditing interceptors, JPA integration)
- Spring Security 6.0.x (authentication, authorization)
- GraalVM JS 22.3.1 (JavaScript flow execution)

## Relationships with Other Modules

```
Core Module (foundation)
    ↓ used by
├── Controller Module (web layer)
├── Service Module (business logic)
├── Repository Module (data access)
└── All other modules
```

**Core → Model**: Core references model entities for security (User, Role, Privilege) and multi-tenancy (Organization)

**Core → Repository**: Core provides base repository interfaces (SecureRepository pattern)

**Service → Core**: Services use Flow pipelines, caching, and security utilities

**Controller → Core**: Controllers use Flow for request handling and PageModelMap for responses

## Setup and Testing

### Module-Specific Setup

No additional setup required beyond standard Spring Boot configuration. Core components are auto-configured.

### Running Core Tests

```bash
cd openkoda
mvn test -Dtest=com.openkoda.core.**
```

### Integration Test Pattern

Core provides `AbstractTest` base class for integration tests with database and Spring context:

```java
@SpringBootTest
public class MyServiceTest extends AbstractTest {
    @Autowired
    private MyService service;
    
    @Test
    public void testFeature() {
        // Test implementation
    }
}
```

## Additional Resources

- [Flow Pipeline Documentation](flow/Flow.java) - Functional composition patterns
- [Security Architecture](security/SecurityService.java) - Authentication and authorization
- [Audit System](audit/AuditInterceptor.java) - Entity change tracking
- [Multi-Tenancy Guide](multitenancy/) - Organization-scoped operations

## Design Patterns Used

- **Pipeline Pattern**: Flow-based request processing
- **Interceptor Pattern**: Hibernate audit interceptors
- **Cache-Aside Pattern**: Request-scoped caching
- **Template Method**: LoggingComponentWithRequestId base class
- **Strategy Pattern**: Customisation service extension points

---

**Source**: openkoda/src/main/java/com/openkoda/core/  
**Package Documentation**: See package-info.java files in each subpackage  
**API Reference**: See [Javadoc](../../../../target/site/apidocs/com/openkoda/core/package-summary.html)

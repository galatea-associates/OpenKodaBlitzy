# Controller Module

## Overview

The controller module implements the web layer (presentation tier) of the OpenKoda platform using Spring MVC. Controllers handle HTTP requests, orchestrate business logic via Flow pipelines, and return HTML views or JSON responses. The module provides generic CRUD adapters, REST APIs, admin interfaces, and public endpoints.

**Key Responsibilities**:
- HTTP request/response handling
- Request parameter binding and validation
- Flow pipeline orchestration for business logic composition
- View assembly via PageModelMap
- Security enforcement with @PreAuthorize
- Error handling and exception mapping

**Position in Architecture**: Entry point for all web traffic. Sits above service and repository layers, delegates business logic to services, uses Flow for composable request handling.

## Module Structure

### Subpackages

- **admin/**: Admin interface controllers (logs, audit, integrations, system health)
- **api/**: REST API controllers with JSON responses (v1, v2, auth)
- **common/**: Common constants (PageAttributes, SessionData, URLConstants)
- **file/**: File upload/download controllers
- **frontendresource/**: Frontend resource serving (assets, public, restricted)
- **notification/**: Notification management
- **organization/**: Multi-tenant organization management
- **report/**: Report generation (AI-powered, query-based)
- **role/**: Role and permission management
- **user/**: User management and password recovery

### Key Classes

- **CRUDControllerHtml**: Generic HTML CRUD for any entity (list/create/edit/delete)
- **CRUDApiController**: Generic REST API CRUD with JSON responses
- **PageBuilderController**: Dynamic dashboard rendering from FrontendResource configs
- **PublicController**: Public endpoints (registration, email verification)
- **ErrorControllerImpl**: Global error handling (404, 500, 403)
- **ComponentsController**: Component export/import (YAML archives)
- **SitemapController**: XML sitemap for SEO

## Configuration

### Spring MVC Setup

Controllers use standard Spring MVC annotations:

```java
@Controller
@RequestMapping("/admin")
public class MyController extends ComponentProvider {
    // Inherits services, repositories, controllers, messages
}
```

### Security Configuration

Endpoints secured with Spring Security @PreAuthorize:

```java
@PreAuthorize("hasAuthority('" + Privilege.PrivilegeNames.canReadOrgData + "')")
@GetMapping("/data")
public Object viewData() { ... }
```

### Generic CRUD Registration

Register entities for automatic CRUD endpoints in CRUDControllers:

```java
htmlCRUDControllerConfigurationMap.registerCRUDControllerBuilder(
    "users",
    Privilege.canReadOrgData,
    Privilege.canManageOrgData,
    builder -> builder.text("name").email("email").build(),
    repositories.secure.user
);
```

## Core Components

### CRUDControllerHtml

**Purpose**: Generic HTML CRUD controller eliminating boilerplate

**Endpoints**:
- `GET /{obj}/all` - List entities with pagination
- `GET /{obj}/{id}` - Entity detail view
- `GET /{obj}/new` - Create form
- `POST /{obj}/save` - Submit form
- `GET /{obj}/{id}/edit` - Edit form
- `POST /{obj}/{id}/delete` - Delete entity

**Usage Example**:
```java
// Automatically available for registered entities:
// GET /users/all
// GET /users/123
// GET /users/new
```

### Flow Pipeline Pattern

Controllers use Flow for composable request handling:

```java
@GetMapping("/users")
public Object listUsers(@PathVariable Long orgId, Pageable pageable) {
    return Flow.init(services)
        .thenSet("users", a -> services.user.findAll(orgId, pageable))
        .thenSet("count", a -> services.user.count(orgId))
        .execute();
}
```

### PageBuilderController

**Purpose**: Dynamic dashboard composition from widget definitions

**Key Method**:
```java
public String renderDashboard(Long dashboardId) {
    // Deserializes JSON dashboard config
    // Dispatches to widget controllers
    // Assembles HTML fragments
    return assembledDashboard;
}
```

## API Usage Examples

### Example 1: Creating Custom Controller

```java
@Controller
@RequestMapping("/products")
public class ProductController extends ComponentProvider {
    
    @GetMapping("/all")
    public Object listProducts(Pageable pageable) {
        return Flow.init(services, repositories)
            .thenSet("products", a -> repositories.secure.product.findAll(pageable))
            .execute()
            .mav("product/list");
    }
}
```

### Example 2: REST API Endpoint

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductApiController extends ComponentProvider {
    
    @GetMapping
    public ResponseEntity<List<Product>> getProducts() {
        List<Product> products = services.product.findAll();
        return ResponseEntity.ok(products);
    }
}
```

### Example 3: Flow with Error Handling

```java
@PostMapping("/save")
public Object saveProduct(@Valid ProductForm form, BindingResult result) {
    return Flow.init(services)
        .then(a -> {
            if (result.hasErrors()) {
                return new Result(false).mav("product/form");
            }
            Product product = form.toEntity();
            services.product.save(product);
            return new Result(true).mav("redirect:/products/all");
        })
        .execute();
}
```

## Dependencies and Relationships

### Internal Dependencies

- **core.flow**: Flow, PageModelMap, Result for request orchestration
- **service.***: Business logic services (user, organization, etc.)
- **repository.***: Data access repositories (secure and unsecure)
- **model.***: JPA entities (User, Organization, Role, etc.)
- **form.***: Form binding and validation classes
- **core.security**: Security utilities and privilege helpers

### Module Relationship Diagram

```
HTTP Request
    ↓
Controller (this module)
    ↓ delegates via Flow
Service Layer
    ↓
Repository Layer
    ↓
Database (PostgreSQL)
```

### Cross-Module Usage

- **Services** call: `services.user.findById()`, `services.organization.create()`
- **Repositories** call: `repositories.secure.user.findOne()`, `repositories.unsecure.organization.findAll()`
- **Flow** usage: `Flow.init().thenSet().execute()`

## Setup and Testing

### Running Controllers Locally

1. Start PostgreSQL: `docker-compose up postgres`
2. Run application: `mvn spring-boot:run`
3. Access: `http://localhost:8080`

### Controller Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testListProducts() throws Exception {
        mockMvc.perform(get("/products/all"))
            .andExpect(status().isOk())
            .andExpect(view().name("product/list"));
    }
}
```

### Integration Testing with Selenium

```java
@Test
public void testProductCRUD() {
    driver.get("http://localhost:8080/products/new");
    driver.findElement(By.id("name")).sendKeys("Test Product");
    driver.findElement(By.id("submit")).click();
    assertTrue(driver.getPageSource().contains("Test Product"));
}
```

## Additional Resources

- [Javadoc: CRUDControllerHtml](../../target/site/apidocs/com/openkoda/controller/CRUDControllerHtml.html)
- [Javadoc: Flow](../../target/site/apidocs/com/openkoda/core/flow/Flow.html)
- [Spring MVC Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html)
- [Spring Security](https://docs.spring.io/spring-security/reference/index.html)

## Common Patterns

### Pattern 1: List View with Pagination

```java
@GetMapping("/all")
public Object list(Pageable pageable) {
    return Flow.init(services)
        .thenSet("entities", a -> repository.findAll(pageable))
        .mav("entity/list");
}
```

### Pattern 2: Form Submission with Validation

```java
@PostMapping("/save")
public Object save(@Valid EntityForm form, BindingResult result) {
    if (result.hasErrors()) return mav("entity/form");
    services.entity.save(form.toEntity());
    return mav("redirect:/entities/all");
}
```

### Pattern 3: JSON API Response

```java
@GetMapping("/api/data")
public ResponseEntity<?> getData() {
    return ResponseEntity.ok(services.data.getAll());
}
```

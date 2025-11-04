# Repository Module

## Overview

The Repository module implements the data access layer using Spring Data JPA. It provides repository interfaces for all domain entities, implements the SecureRepository pattern for privilege-based access control, and offers utilities for native queries, specifications, and dynamic repository discovery.

**Position in Architecture**: Data access layer - bridges service layer and database.

## Module Structure

```
com.openkoda.repository/
├── admin/                 (Admin-related repositories)
├── ai/                    (AI conversation and thread repositories)
├── event/                 (Event and notification repositories)
├── file/                  (File storage repositories)
├── notifications/         (Notification repositories)
├── organization/          (Organization and tenant repositories)
├── specifications/        (JPA Specification builders)
├── task/                  (Task and workflow repositories)
├── user/                  (User, role, privilege repositories)
└── [root]                 (Repositories aggregator, SecureRepository pattern)
```

## Configuration

### Repository Scanning

Enable Spring Data JPA repositories with extended package scanning:

```java
@EnableJpaRepositories(basePackages = {
    "com.openkoda.repository",
    "generated-entity"  // For dynamic entity repositories
})
```

### Transaction Management

Repository operations are automatically transactional when called from `@Transactional` service methods.

### Query Configuration

Configure query timeout and fetch size in `application.properties`:

```properties
spring.jpa.properties.javax.persistence.query.timeout=30000
```

## Key Components

### Repositories.java (Repository Aggregator)

**Purpose**: Convenience aggregator exposing all repository beans for legacy code patterns.

```java
@Component
public class Repositories {
    public final OrganizationRepository organization;
    public final UserRepository user;
    public final RoleRepository role;
    // 40+ more repository beans
}
```

Usage:

```java
@Autowired
private Repositories repositories;

Organization org = repositories.organization.findOne(orgId).orElse(null);
```

### SecureRepository Pattern

**Purpose**: Privilege-enforcing wrapper around standard Spring Data repositories.

```java
public interface SecureRepository<T> {
    Optional<T> findOne(Long id);
    List<T> findAll();
    T save(T entity);
    void delete(Long id);
    // All methods enforce privilege checks
}
```

### SecureRepositories.java

**Purpose**: Aggregator exposing privilege-checked repository instances.

```java
@Autowired
private SecureRepositories secureRepositories;

// Privilege check applied automatically
Optional<Organization> org = secureRepositories.organization.findOne(orgId);
```

### SearchableRepositoryMetadata

**Purpose**: Dynamic repository discovery and metadata extraction.

```java
List<SearchableRepositoryMetadata> repos = metadataService.findAll();
```

## Repository Interfaces

### Standard Repository Pattern

```java
public interface UserRepository extends 
    CommonRepository<User>,
    SearchableFunctionalRepositoryWithLongId<User> {
    
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.organizationId = :orgId")
    List<User> findByOrganization(@Param("orgId") Long organizationId);
}
```

### Secure Repository Wrapper

```java
public interface SecureUserRepository extends SecureRepository<User> {
    // Inherits privilege-checked CRUD operations
}
```

## API Usage Examples

### Example 1: Basic Repository Usage

```java
@Autowired
private UserRepository userRepository;

Optional<User> user = userRepository.findByEmail("user@example.com");
```

### Example 2: Secure Repository Usage

```java
@Autowired
private SecureRepositories secureRepositories;

// Privilege check enforced
Optional<Organization> org = secureRepositories.organization.findOne(orgId);
```

### Example 3: Using Specifications

```java
Specification<User> spec = UserSpecifications.searchSpecification(searchTerm);
List<User> users = userRepository.findAll(spec);
```

### Example 4: Native Queries

```java
String sql = "SELECT * FROM users WHERE email LIKE :pattern";
List<User> users = userRepository.findBySql(sql, Map.of("pattern", "%@example.com"));
```

## Dependencies

### Internal Dependencies

- **Model module**: Repositories operate on model entities
- **Core module**: Uses security context for privilege checks

### External Dependencies

- Spring Data JPA 3.0.x (repository abstraction)
- Hibernate 6.1.x (JPA provider)
- PostgreSQL JDBC driver (database connectivity)
- Querydsl (optional, for type-safe queries)

## Relationships with Other Modules

```
Service Layer
    ↓ calls
Repository Layer (this module)
    ↓ queries
Database (PostgreSQL)
```

**Repository → Model**: Repositories define queries for entity types

**Repository → Core**: SecureRepository uses core security services

**Service → Repository**: Services call repository methods for data access

## Setup and Testing

### Database Setup

Repositories require PostgreSQL database connection:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/openkoda
spring.datasource.username=postgres
spring.datasource.password=password
```

### Running Repository Tests

```bash
cd openkoda
mvn test -Dtest=com.openkoda.repository.**
```

### Integration Test Pattern

```java
@SpringBootTest
@Transactional
public class UserRepositoryTest extends AbstractTest {
    @Autowired
    private UserRepository repository;
    
    @Test
    public void testFindByEmail() {
        User user = createTestUser();
        Optional<User> found = repository.findByEmail(user.getEmail());
        assertTrue(found.isPresent());
    }
}
```

## Additional Resources

- [Repositories.java](Repositories.java) - Repository aggregator pattern
- [SecureRepository.java](SecureRepository.java) - Privilege enforcement interface
- [UserRepository.java](user/UserRepository.java) - Example repository with custom queries
- [Specifications Package](specifications/) - JPA Specification builders

## Query Patterns

### Method Query Derivation

Spring Data JPA generates queries from method names:

```java
List<User> findByEmailAndOrganizationId(String email, Long orgId);
Optional<User> findFirstByEmailOrderByCreatedOnDesc(String email);
Long countByOrganizationId(Long orgId);
boolean existsByEmail(String email);
```

### Named Queries

Define queries with `@Query` annotation:

```java
@Query("SELECT u FROM User u WHERE u.email LIKE :pattern")
List<User> searchByEmailPattern(@Param("pattern") String pattern);
```

### Native SQL Queries

Use native SQL when needed:

```java
@Query(value = "SELECT * FROM users WHERE organization_id = :orgId", nativeQuery = true)
List<User> findByOrgNative(@Param("orgId") Long organizationId);
```

### Specifications

Build complex queries programmatically:

```java
public static Specification<User> hasEmail(String email) {
    return (root, query, cb) -> cb.equal(root.get("email"), email);
}

List<User> users = repository.findAll(hasEmail("test@example.com"));
```

## Performance Considerations

### Fetch Strategies

Configure fetch joins to avoid N+1 queries:

```java
@Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.id = :id")
Optional<User> findByIdWithRoles(@Param("id") Long id);
```

### Batch Fetching

Enable batch fetching in entity mappings:

```java
@OneToMany(fetch = FetchType.LAZY)
@BatchSize(size = 10)
private Set<UserRole> roles;
```

### Pagination

Use `Pageable` for large result sets:

```java
Page<User> findByOrganizationId(Long orgId, Pageable pageable);
```

---

**Source**: openkoda/src/main/java/com/openkoda/repository/  
**Package Documentation**: See package-info.java files in subpackages  
**API Reference**: See [Javadoc](../../../../target/site/apidocs/com/openkoda/repository/package-summary.html)

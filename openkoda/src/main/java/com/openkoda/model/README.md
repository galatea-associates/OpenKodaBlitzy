# Model Module

## Overview

The Model module defines the JPA domain model for OpenKoda, including entities for multi-tenancy (Organization), role-based access control (Role, Privilege, User), dynamic runtime entities (DynamicEntity), configuration, and core business entities. All entities use JPA annotations with Hibernate as the ORM provider.

**Position in Architecture**: Domain model layer - foundation for repository and service layers.

## Module Structure

```
com.openkoda.model/
├── authentication/        (Token, UserRole, LoginAndPassword)
├── common/                (OpenkodaModule, PrivilegeBase, AbstractEntity)
├── component/             (FrontendResource, Scheduler, ServerJs, Form, Event)
├── file/                  (File entity with organization scoping)
├── module/                (Module registration and lifecycle)
├── notification/          (Notification, NotificationConfig entities)
├── report/                (Report definition entities)
├── task/                  (Task and workflow entities)
└── [root entities]        (Organization, User, Role, Privilege, DynamicEntity)
```

## Configuration

### JPA Entity Scanning

Entities are auto-discovered via `@EntityScan`:

```java
@EntityScan(basePackages = {
    "com.openkoda.model",
    "generated-entity"  // For dynamic entities
})
```

### Auditing Configuration

Enable JPA auditing for automatic timestamp management:

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig { }
```

Entities extending `TimestampedEntity` automatically get `createdOn` and `updatedOn` timestamps.

### Sequence Generators

Global ID sequence with allocation size 10 for performance:

```java
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_generator")
@SequenceGenerator(name = "global_generator", sequenceName = "GLOBAL_ID_GENERATOR", allocationSize = 10)
private Long id;
```

## Core Entities

### Organization (Multi-Tenancy)

**Purpose**: Tenant entity representing isolated organizational units.

**Key Features**:
- Properties Map stored as JSONB for flexible configuration
- Computed fields via `@Formula` for privilege tokens
- Branding fields (name, logoId)

```java
@Entity
@Table(name = "organizations")
public class Organization extends TimestampedEntity {
    private String name;
    private Map<String, String> properties;  // JSONB storage
    
    @Formula("...SQL...")  // Computed privilege token
    private String requiredReadPrivilege;
}
```

### User (Authentication)

**Purpose**: User entity with authentication credentials and organization memberships.

**Key Relationships**:
- Many-to-Many with Organization via UserRole
- Role assignments per organization
- API keys and tokens for programmatic access

```java
@Entity
public class User extends TimestampedEntity {
    private String email;
    private String passwordHash;
    
    @OneToMany(mappedBy = "userId")
    private Set<UserRole> roles;
}
```

### Role (RBAC)

**Purpose**: Role-based access control with privilege sets.

**Inheritance Strategy**: Single table inheritance for GlobalRole, OrganizationRole, GlobalOrganizationRole

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Role extends TimestampedEntity {
    private String name;
    private String privileges;  // Comma-separated privilege names
    
    @Transient
    private Set<PrivilegeBase> privilegesSet;
}
```

### Privilege (Authorization)

**Purpose**: Enumeration of canonical system privileges with categorization.

```java
public enum Privilege implements PrivilegeBase {
    canReadBackend(1, PrivilegeGroup.ORGANIZATION, "Read Backend"),
    canManageBackend(2, PrivilegeGroup.ORGANIZATION, "Manage Backend");
    
    private final long id;
    private final PrivilegeGroup group;
    private final String label;
}
```

### DynamicEntity (Runtime Entities)

**Purpose**: Definition of runtime-generated JPA entities via Byte Buddy.

```java
@Entity
public class DynamicEntity extends TimestampedEntity {
    @Column(unique = true)
    private String tableName;
    
    private String definition;  // JSON entity definition
}
```

## Entity Features

### Auditing

Entities extending `TimestampedEntity` or `AuditableEntityBase` get automatic auditing:

```java
@EntityListeners(AuditingEntityListener.class)
public abstract class TimestampedEntity {
    @CreatedDate
    private Instant createdOn;
    
    @LastModifiedDate
    private Instant updatedOn;
}
```

### Tenant Awareness

Organization-scoped entities include `organizationId` for multi-tenancy:

```java
@Entity
public class MyEntity extends OrganizationRelatedEntity {
    @Column(name = "organization_id")
    private Long organizationId;
}
```

### Computed Fields

Use `@Formula` for database-computed values:

```java
@Formula("CASE WHEN removable THEN 'CAN_MANAGE_' || UPPER(name) ELSE NULL END")
private String requiredPrivilege;
```

## API Usage Examples

### Example 1: Creating an Organization

```java
Organization org = new Organization();
org.setName("Acme Corp");
organizationRepository.save(org);
```

### Example 2: User with Roles

```java
User user = new User();
user.setEmail("user@example.com");
UserRole role = new UserRole(user.getId(), roleId, orgId);
```

### Example 3: Querying with Tenant Scope

```java
List<MyEntity> entities = repository.findByOrganizationId(currentOrgId);
```

## Dependencies

### Internal Dependencies

- **Core module**: Uses core utilities and base classes
- **Repository module**: Entities are persisted via repositories

### External Dependencies

- Spring Data JPA 3.0.x (JPA provider integration)
- Hibernate 6.1.x (ORM implementation)
- PostgreSQL JDBC (database driver)
- Jackson (JSON serialization for properties fields)

## Relationships with Other Modules

```
Model Module (domain entities)
    ↓ used by
├── Repository Module (data access)
├── Service Module (business logic)
└── Controller Module (web layer)
```

**Model → Core**: Extends base entity classes (TimestampedEntity, OrganizationRelatedEntity)

**Repository → Model**: Repositories define queries for model entities

**Service → Model**: Services operate on entity instances

**Controller → Model**: Controllers transform entities to DTOs for API responses

## Setup and Testing

### Database Schema

Entities require PostgreSQL database with schema initialized:

```bash
psql -U postgres -f schema.sql
```

### Running Model Tests

```bash
cd openkoda
mvn test -Dtest=com.openkoda.model.**
```

### Integration Test Pattern

```java
@SpringBootTest
@Transactional
public class OrganizationTest extends AbstractTest {
    @Autowired
    private OrganizationRepository repository;
    
    @Test
    public void testCreateOrganization() {
        Organization org = new Organization();
        org.setName("Test");
        org = repository.save(org);
        assertNotNull(org.getId());
    }
}
```

## Additional Resources

- [Organization Entity](Organization.java) - Tenant model with JSONB properties
- [Role/Privilege System](Role.java) - RBAC implementation
- [User Entity](User.java) - Authentication and authorization
- [Dynamic Entities](DynamicEntity.java) - Runtime entity generation

## Entity Lifecycle

```
1. Entity Creation → new Entity()
2. Validation → @PrePersist callbacks
3. Persistence → repository.save()
4. Audit → @LastModifiedDate updated
5. Retrieval → repository.findById()
6. Update → entity.setField() → repository.save()
7. Deletion → repository.delete()
```

## Common Patterns

### Entity Builder Pattern

```java
Organization org = Organization.builder()
    .name("Company")
    .build();
```

### Cascading Operations

```java
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
private Set<ChildEntity> children;
```

---

**Source**: openkoda/src/main/java/com/openkoda/model/  
**Package Documentation**: See package-info.java files in subpackages  
**API Reference**: See [Javadoc](../../../../target/site/apidocs/com/openkoda/model/package-summary.html)

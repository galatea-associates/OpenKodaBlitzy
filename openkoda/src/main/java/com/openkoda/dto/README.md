# DTO Module

## Overview

The DTO (Data Transfer Object) module provides mutable JavaBean classes for transferring data across application layers. DTOs decouple API contracts from internal entity representations, enable JSON serialization for REST APIs, and provide simplified data structures for UI rendering.

**Position in Architecture**: Data transfer layer - used across controller, service, and integration modules.

## Module Structure

```
com.openkoda.dto/
├── file/                  (File and document DTOs)
│   ├── FileDto            (File metadata transfer)
│   └── DocumentDto        (Document content transfer)
├── payment/               (Payment and billing DTOs)
│   └── PaymentDto         (Payment transaction data)
├── system/                (System and configuration DTOs)
│   ├── SystemHealthDto    (Health check data)
│   └── ConfigDto          (Configuration settings)
├── user/                  (User and authentication DTOs)
│   ├── UserDto            (User profile data)
│   ├── LoginDto           (Login credentials)
│   └── RegistrationDto    (User registration data)
└── [root DTOs]            (Common DTOs)
    ├── NotificationDto    (Notification messages)
    └── AuditDto           (Audit trail records)
```

## Configuration

### Jackson Serialization

DTOs use Jackson annotations for JSON serialization:

```properties
# application.properties
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.time-zone=UTC
spring.jackson.default-property-inclusion=non_null
```

### Bean Validation

Enable validation for DTOs:

```java
@Configuration
public class ValidationConfig {
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }
}
```

## Key DTOs

### NotificationDto

**Purpose**: Transfer notification data between service and controller layers.

```java
public class NotificationDto {
    private Long id;
    private String message;
    private String type;  // INFO, WARNING, ERROR
    private Instant createdOn;
    private boolean read;
    
    // Getters and setters
}
```

### UserDto

**Purpose**: User profile data for API responses.

```java
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    
    @JsonIgnore
    private String passwordHash;  // Never serialize passwords
    
    // Getters and setters
}
```

### FileDto

**Purpose**: File metadata transfer for upload/download operations.

```java
public class FileDto {
    private Long id;
    private String filename;
    private String contentType;
    private Long size;
    private String downloadUrl;
    
    // Getters and setters
}
```

## DTO Patterns

### Entity → DTO Mapping

Convert entities to DTOs for API responses:

```java
public class UserDto {
    public static UserDto fromEntity(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRoles(user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet()));
        return dto;
    }
}
```

### DTO → Entity Mapping

Convert DTOs to entities for persistence:

```java
public void updateEntity(User user, UserDto dto) {
    user.setEmail(dto.getEmail());
    user.setFirstName(dto.getFirstName());
    user.setLastName(dto.getLastName());
    // Password updated separately with validation
}
```

### Nested DTOs

DTOs can contain other DTOs for complex structures:

```java
public class OrganizationDto {
    private Long id;
    private String name;
    private List<UserDto> users;
    private List<RoleDto> roles;
    
    // Getters and setters
}
```

## API Usage Examples

### Example 1: REST Controller Response

```java
@GetMapping("/api/users/{id}")
public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    UserDto dto = UserDto.fromEntity(user);
    return ResponseEntity.ok(dto);
}
```

### Example 2: Request Body Validation

```java
@PostMapping("/api/users")
public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto dto) {
    User user = userService.create(dto);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(UserDto.fromEntity(user));
}
```

### Example 3: Page Response

```java
@GetMapping("/api/users")
public Page<UserDto> getUsers(Pageable pageable) {
    Page<User> users = userRepository.findAll(pageable);
    return users.map(UserDto::fromEntity);
}
```

## Jackson Annotations

### Common Annotations

```java
public class UserDto {
    @JsonProperty("user_id")  // Custom JSON property name
    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Instant createdOn;
    
    @JsonIgnore  // Exclude from serialization
    private String internalField;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String optionalField;
}
```

### Custom Serializer

```java
public class MoneyDto {
    @JsonSerialize(using = MoneySerializer.class)
    @JsonDeserialize(using = MoneyDeserializer.class)
    private BigDecimal amount;
}
```

## Bean Validation

### Validation Annotations

```java
public class RegistrationDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    private String password;
    
    @Pattern(regexp = "^[a-zA-Z ]+$", message = "Name must contain only letters")
    private String firstName;
    
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
}
```

### Custom Validation

```java
@Constraint(validatedBy = UniqueEmailValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueEmail {
    String message() default "Email already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

## Dependencies

### Internal Dependencies

- **Model module**: DTOs often mirror entity structures
- **Service module**: Services convert between entities and DTOs

### External Dependencies

- Jackson Databind 2.14.x (JSON serialization)
- Jakarta Bean Validation 3.0.x (validation annotations)
- Spring Web (used in controllers)

## Relationships with Other Modules

```
Controller Layer
    ↓ uses DTOs for request/response
DTO Module
    ↓ mapped to/from
Model Module (Entities)
```

**Controller → DTO**: Controllers use DTOs for API contracts

**DTO → Model**: DTOs are mapped to/from entity classes

**Service → DTO**: Services perform entity-DTO conversions

**Integration → DTO**: External API responses deserialized to DTOs

## Setup and Testing

### DTO Testing Pattern

```java
@Test
public void testDtoSerialization() throws JsonProcessingException {
    UserDto dto = new UserDto();
    dto.setId(123L);
    dto.setEmail("test@example.com");
    
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(dto);
    
    assertTrue(json.contains("\"email\":\"test@example.com\""));
}
```

### Validation Testing

```java
@Test
public void testDtoValidation() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    
    RegistrationDto dto = new RegistrationDto();
    dto.setEmail("invalid-email");  // Invalid format
    
    Set<ConstraintViolation<RegistrationDto>> violations = 
        validator.validate(dto);
    
    assertFalse(violations.isEmpty());
}
```

## Additional Resources

- [NotificationDto.java](NotificationDto.java) - Notification transfer object
- [UserDto.java](user/UserDto.java) - User profile DTO
- [FileDto.java](file/FileDto.java) - File metadata DTO

## DTO Best Practices

### 1. Immutability (Optional)

For read-only DTOs, consider immutability:

```java
public class UserDto {
    private final Long id;
    private final String email;
    
    public UserDto(Long id, String email) {
        this.id = id;
        this.email = email;
    }
    
    // Only getters, no setters
}
```

### 2. Builder Pattern

For complex DTOs with many fields:

```java
public class OrganizationDto {
    // Fields...
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long id;
        private String name;
        
        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public OrganizationDto build() {
            OrganizationDto dto = new OrganizationDto();
            dto.setId(id);
            dto.setName(name);
            return dto;
        }
    }
}
```

### 3. Separate Read/Write DTOs

Different DTOs for input and output:

```java
// For API responses
public class UserResponseDto {
    private Long id;
    private String email;
    // No password field
}

// For API requests
public class UserRequestDto {
    private String email;
    private String password;
    // No id field (assigned by server)
}
```

### 4. Version-Specific DTOs

For API versioning:

```java
public class UserDtoV1 {
    private String name;  // Single name field
}

public class UserDtoV2 {
    private String firstName;  // Split into two fields
    private String lastName;
}
```

## JSON Examples

### UserDto JSON

```json
{
    "id": 123,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["USER", "ADMIN"],
    "createdOn": "2024-11-04T14:00:00Z"
}
```

### NotificationDto JSON

```json
{
    "id": 456,
    "message": "Your request was processed",
    "type": "INFO",
    "read": false,
    "createdOn": "2024-11-04T14:30:00Z"
}
```

### PageDto JSON

```json
{
    "content": [
        { "id": 1, "name": "Item 1" },
        { "id": 2, "name": "Item 2" }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 20
    },
    "totalElements": 42,
    "totalPages": 3
}
```

## Common Pitfalls

### Avoid Circular References

```java
// BAD: Circular reference
public class UserDto {
    private OrganizationDto organization;
}
public class OrganizationDto {
    private List<UserDto> users;  // Circular!
}

// GOOD: Break the cycle
public class UserDto {
    private Long organizationId;  // Reference by ID only
}
```

### Handle Null Values

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private String optionalField;  // Null values omitted from JSON
}
```

---

**Source**: openkoda/src/main/java/com/openkoda/dto/  
**Package Documentation**: See package-info.java files in subpackages  
**API Reference**: See [Javadoc](../../../../target/site/apidocs/com/openkoda/dto/package-summary.html)

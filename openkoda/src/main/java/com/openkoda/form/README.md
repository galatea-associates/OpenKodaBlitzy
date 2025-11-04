# Form Module

## Overview

The Form module provides request-side form adapters that implement the FrontendMappingDefinition interface for binding HTTP form data to domain entities. Forms handle the complete lifecycle: populating from entities (populateFrom), validating input (validate), and updating entities (populateTo).

**Position in Architecture**: Form handling layer - mediates between HTTP requests and domain entities.

## Module Structure

```
com.openkoda.form/
├── rule/                  (Logical operators and rule editor components)
│   ├── Rule               (Rule definition base)
│   ├── RuleOperator       (AND, OR, NOT operators)
│   └── RuleEditor         (Rule construction UI support)
└── [root forms]           (Form implementations)
    ├── OrganizationForm   (Organization CRUD form)
    ├── UserForm           (User management form)
    ├── RoleForm           (Role configuration form)
    └── PageForm           (Dynamic page form)
```

## Configuration

### Form Validation Configuration

Enable Jakarta Bean Validation:

```java
@Configuration
public class ValidationConfig {
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }
}
```

### Form Binding Configuration

Configure Spring MVC data binding:

```properties
# application.properties
spring.mvc.format.date=yyyy-MM-dd
spring.mvc.format.date-time=yyyy-MM-dd'T'HH:mm:ss
```

## Key Components

### FrontendMappingDefinition Interface

**Purpose**: Defines the contract for form lifecycle management.

```java
public interface FrontendMappingDefinition {
    // Populate form from entity
    FrontendMappingDefinition populateFrom(Object entity);
    
    // Validate form data
    boolean validate();
    
    // Update entity from form
    void populateTo(Object entity);
    
    // Get validation errors
    Map<String, String> getErrors();
}
```

### Abstract Form Base Class

Most forms extend a base class providing common functionality:

```java
public abstract class AbstractForm implements FrontendMappingDefinition {
    protected Map<String, String> errors = new HashMap<>();
    
    @Override
    public Map<String, String> getErrors() {
        return errors;
    }
    
    protected void addError(String field, String message) {
        errors.put(field, message);
    }
}
```

### OrganizationForm Example

**Purpose**: Form for creating and updating organizations.

```java
public class OrganizationForm extends AbstractOrganizationRelatedForm {
    private String name;
    private String description;
    private Map<String, String> properties;
    
    @Override
    public OrganizationForm populateFrom(Organization entity) {
        this.name = entity.getName();
        this.description = entity.getDescription();
        this.properties = entity.getProperties();
        return this;
    }
    
    @Override
    public boolean validate() {
        if (StringUtils.isBlank(name)) {
            addError("name", "Name is required");
            return false;
        }
        return true;
    }
    
    @Override
    public void populateTo(Organization entity) {
        entity.setName(name);
        entity.setDescription(description);
        entity.setProperties(properties);
    }
}
```

## Form Lifecycle

### 1. Display Form (GET)

```java
@GetMapping("/organization/{id}/edit")
public String editForm(@PathVariable Long id, Model model) {
    Organization org = organizationRepository.findById(id).orElseThrow();
    
    OrganizationForm form = new OrganizationForm();
    form.populateFrom(org);  // Entity → Form
    
    model.addAttribute("form", form);
    return "organization/edit";
}
```

### 2. Submit Form (POST)

```java
@PostMapping("/organization/{id}/edit")
public String submitForm(
    @PathVariable Long id,
    @ModelAttribute OrganizationForm form,
    Model model
) {
    if (!form.validate()) {  // Validate
        model.addAttribute("errors", form.getErrors());
        return "organization/edit";
    }
    
    Organization org = organizationRepository.findById(id).orElseThrow();
    form.populateTo(org);  // Form → Entity
    organizationRepository.save(org);
    
    return "redirect:/organizations";
}
```

### 3. Create New Entity

```java
@PostMapping("/organization/new")
public String createOrganization(
    @ModelAttribute OrganizationForm form,
    Model model
) {
    if (!form.validate()) {
        model.addAttribute("errors", form.getErrors());
        return "organization/new";
    }
    
    Organization org = new Organization();
    form.populateTo(org);  // Form → New Entity
    organizationRepository.save(org);
    
    return "redirect:/organizations";
}
```

## API Usage Examples

### Example 1: Simple Form Handling

```java
OrganizationForm form = new OrganizationForm();
form.setName("New Org");

if (form.validate()) {
    Organization org = new Organization();
    form.populateTo(org);
    repository.save(org);
}
```

### Example 2: Form with Validation Errors

```java
UserForm form = new UserForm();
form.setEmail("invalid-email");

if (!form.validate()) {
    Map<String, String> errors = form.getErrors();
    // Display errors to user
    return "user/form";
}
```

### Example 3: Updating Existing Entity

```java
User user = userRepository.findById(userId).orElseThrow();
UserForm form = new UserForm().populateFrom(user);

// User modifies form data
form.setFirstName("Updated Name");

if (form.validate()) {
    form.populateTo(user);
    userRepository.save(user);
}
```

## Field Definitions

### FieldDefinition Builder

Forms use FieldDefinition for dynamic field configuration:

```java
FieldDefinition nameField = FieldDefinition.builder()
    .name("name")
    .label("Organization Name")
    .type(FieldType.TEXT)
    .required(true)
    .maxLength(100)
    .build();
```

### Dynamic Form Generation

```java
public class DynamicForm extends AbstractForm {
    private Map<String, Object> fields = new HashMap<>();
    private List<FieldDefinition> fieldDefinitions;
    
    public DynamicForm(List<FieldDefinition> definitions) {
        this.fieldDefinitions = definitions;
    }
    
    @Override
    public boolean validate() {
        for (FieldDefinition def : fieldDefinitions) {
            Object value = fields.get(def.getName());
            if (def.isRequired() && value == null) {
                addError(def.getName(), def.getLabel() + " is required");
                return false;
            }
        }
        return true;
    }
}
```

## Validation Strategies

### Bean Validation Integration

```java
public class UserForm extends AbstractForm {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @Override
    public boolean validate() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        
        Set<ConstraintViolation<UserForm>> violations = validator.validate(this);
        
        if (!violations.isEmpty()) {
            violations.forEach(v -> 
                addError(v.getPropertyPath().toString(), v.getMessage())
            );
            return false;
        }
        return true;
    }
}
```

### Custom Validation Logic

```java
@Override
public boolean validate() {
    boolean valid = true;
    
    if (StringUtils.isBlank(email)) {
        addError("email", "Email is required");
        valid = false;
    }
    
    if (password != null && password.length() < 8) {
        addError("password", "Password must be at least 8 characters");
        valid = false;
    }
    
    // Business rule validation
    if (emailExists(email)) {
        addError("email", "Email already in use");
        valid = false;
    }
    
    return valid;
}
```

## Dependencies

### Internal Dependencies

- **Core module**: Uses UrlHelper, MultitenancyService
- **Model module**: Forms populate/update entity classes

### External Dependencies

- Spring Web (form binding, @ModelAttribute)
- Jakarta Bean Validation (validation annotations)
- Apache Commons Lang (StringUtils)

## Relationships with Other Modules

```
HTTP Request
    ↓ binds to
Form (this module)
    ↓ validates & populates
Entity (Model module)
    ↓ persisted via
Repository
```

**Controller → Form**: Controllers bind HTTP requests to forms

**Form → Model**: Forms populate entities from user input

**Form → Core**: Forms use core utilities for validation

## Setup and Testing

### Form Testing Pattern

```java
@Test
public void testFormValidation() {
    OrganizationForm form = new OrganizationForm();
    form.setName("");  // Invalid: empty name
    
    assertFalse(form.validate());
    assertTrue(form.getErrors().containsKey("name"));
}
```

### Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
public class OrganizationFormTest extends AbstractTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testSubmitForm() throws Exception {
        mockMvc.perform(post("/organization/new")
            .param("name", "Test Org")
            .param("description", "Description"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organizations"));
    }
}
```

## Additional Resources

- [FrontendMappingDefinition.java](../core/form/FrontendMappingDefinition.java) - Form lifecycle interface
- [FieldDefinition.java](../core/form/FieldDefinition.java) - Field configuration
- [OrganizationForm.java](OrganizationForm.java) - Example form implementation
- [Rule Package](rule/) - Logical operators and rule editor

## Rule Editor Components

### Rule Definition

**Purpose**: Define validation rules and business logic.

```java
public class Rule {
    private String field;
    private RuleOperator operator;
    private Object value;
    
    public boolean evaluate(Object entity) {
        Object fieldValue = extractFieldValue(entity, field);
        return operator.apply(fieldValue, value);
    }
}
```

### Rule Operators

```java
public enum RuleOperator {
    EQUALS((a, b) -> Objects.equals(a, b)),
    NOT_EQUALS((a, b) -> !Objects.equals(a, b)),
    GREATER_THAN((a, b) -> compare(a, b) > 0),
    LESS_THAN((a, b) -> compare(a, b) < 0),
    CONTAINS((a, b) -> a.toString().contains(b.toString()));
    
    private final BiFunction<Object, Object, Boolean> evaluator;
    
    public boolean apply(Object a, Object b) {
        return evaluator.apply(a, b);
    }
}
```

### Composite Rules

```java
public class CompositeRule extends Rule {
    private List<Rule> rules;
    private LogicalOperator operator;  // AND, OR
    
    @Override
    public boolean evaluate(Object entity) {
        if (operator == LogicalOperator.AND) {
            return rules.stream().allMatch(r -> r.evaluate(entity));
        } else {
            return rules.stream().anyMatch(r -> r.evaluate(entity));
        }
    }
}
```

## Form Best Practices

### 1. Separate Concerns

Keep forms focused on data binding and validation:

```java
// GOOD: Form handles only binding and validation
public class UserForm extends AbstractForm {
    private String email;
    
    @Override
    public boolean validate() {
        // Validation logic only
    }
}

// Service handles business logic
@Service
public class UserService {
    public User createUser(UserForm form) {
        // Business logic here
    }
}
```

### 2. Immutable After Validation

Once validated, forms should not be modified:

```java
public class ImmutableForm {
    private final String name;
    private boolean validated = false;
    
    public void setName(String name) {
        if (validated) {
            throw new IllegalStateException("Cannot modify validated form");
        }
        this.name = name;
    }
}
```

### 3. Clear Error Messages

Provide user-friendly validation messages:

```java
if (password.length() < 8) {
    addError("password", "Password must be at least 8 characters long");
}
if (!password.matches(".*[A-Z].*")) {
    addError("password", "Password must contain at least one uppercase letter");
}
```

## Thymeleaf Integration

### Display Form Fields

```html
<form th:object="${form}" method="post">
    <div>
        <label>Name:</label>
        <input type="text" th:field="*{name}" />
        <span th:if="${errors != null and errors.containsKey('name')}" 
              th:text="${errors['name']}" class="error"></span>
    </div>
    
    <button type="submit">Submit</button>
</form>
```

### Display Validation Errors

```html
<div th:if="${errors != null and !errors.isEmpty()}" class="alert alert-danger">
    <ul>
        <li th:each="error : ${errors}" th:text="${error.value}"></li>
    </ul>
</div>
```

---

**Source**: openkoda/src/main/java/com/openkoda/form/  
**Package Documentation**: See package-info.java files in subpackages  
**API Reference**: See [Javadoc](../../../../target/site/apidocs/com/openkoda/form/package-summary.html)

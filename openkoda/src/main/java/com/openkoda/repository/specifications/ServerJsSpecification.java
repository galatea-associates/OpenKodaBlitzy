package com.openkoda.repository.specifications;


import com.openkoda.model.component.ServerJs;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * JPA Criteria API Specification builders for ServerJs entity queries.
 * <p>
 * Provides static factory methods that return {@link Specification} instances for type-safe
 * ServerJs query construction using the JPA Criteria API. These specifications are stateless,
 * reusable, and composable via {@code and()}/{@code or()} operators to build complex filtering
 * conditions. Used by repositories for flexible search and filtering of server-side JavaScript
 * code entities.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * Specification<ServerJs> spec = ServerJsSpecification.getByName("emailValidator.js");
 * List<ServerJs> results = serverJsRepository.findAll(spec);
 * }</pre>
 * </p>
 * <p>
 * <b>Note:</b> Uses string-based attribute name ('name') which is fragile to entity refactoring.
 * Consider migrating to JPA metamodel for type safety.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.model.component.ServerJs
 * @see org.springframework.data.jpa.domain.Specification
 * @see jakarta.persistence.criteria.CriteriaBuilder
 */
public class ServerJsSpecification {
    
    /**
     * Creates a Specification that matches ServerJs entities by exact name.
     * <p>
     * Constructs a lambda-based specification that performs exact equality matching on the
     * name attribute using {@link jakarta.persistence.criteria.CriteriaBuilder#equal}.
     * The specification is stateless and can be safely reused across multiple queries.
     * </p>
     * <p>
     * Usage example:
     * <pre>{@code
     * Specification<ServerJs> spec = getByName("emailValidator.js");
     * }</pre>
     * </p>
     *
     * @param name The exact name of the ServerJs entity to search for. Case-sensitive
     *             equality matching is used
     * @return Specification for ServerJs filtering by name attribute. Returns a lambda-based
     *         Specification instance
     */
    public static Specification<ServerJs> getByName(String name) {

        return (root, query, cb) -> cb.equal(root.get("name"), name);

    }
}

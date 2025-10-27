package com.openkoda.repository;

import com.openkoda.model.OpenkodaModule;
import com.openkoda.model.common.ComponentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Generic base repository interface providing module-scoped persistence operations for ComponentEntity subclasses.
 * <p>
 * This interface extends {@link JpaRepository} to inherit standard CRUD operations and defines domain-level
 * contracts for module-scoped queries and bulk deletions. Annotated with {@link NoRepositoryBean} to prevent
 * Spring Data from creating proxy instances directly for this base interface.
 * </p>
 * <p>
 * All concrete repository interfaces extending this interface inherit {@code findByModule} and {@code deleteByModule}
 * methods, enabling module-scoped operations across different component entity types. The type parameter T must
 * extend {@link ComponentEntity} to ensure module association capability.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * public interface FormRepository extends ComponentEntityRepository<Form> { }
 * }</pre>
 * </p>
 *
 * @param <T> Entity type extending ComponentEntity with module association
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentEntity
 * @see OpenkodaModule
 * @see JpaRepository
 */
@NoRepositoryBean
public interface ComponentEntityRepository <T extends ComponentEntity> extends JpaRepository<T, Long> {

    /**
     * Finds all component entities belonging to the specified module.
     * <p>
     * This method uses Spring Data query derivation to generate a query based on the
     * {@code module} property of the entity. Returns all entities where the module
     * association matches the provided parameter.
     * </p>
     *
     * @param module The OpenkodaModule to query components for, must not be null
     * @return List of component entities associated with the module, empty list if no components found
     */
    List<ComponentEntity> findByModule(OpenkodaModule module);

    /**
     * Deletes all component entities belonging to the specified module.
     * <p>
     * This method performs a bulk delete operation on all entities associated with the
     * specified module. The operation is executed within a transaction. Note that bulk
     * delete operations do not trigger JPA lifecycle callbacks such as {@code @PreRemove}.
     * </p>
     * <p>
     * <b>Warning:</b> This operation permanently removes all component entities for the
     * given module. Ensure proper cascade configuration if related entities must also be deleted.
     * </p>
     *
     * @param module The OpenkodaModule whose components should be deleted, must not be null
     */
    void deleteByModule(OpenkodaModule module);
}

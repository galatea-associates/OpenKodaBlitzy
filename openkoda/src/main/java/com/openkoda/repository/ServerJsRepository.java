/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, 
and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR 
A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR 
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.repository;

import com.openkoda.core.flow.Tuple;
import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.OpenkodaModule;
import com.openkoda.model.component.ServerJs;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository managing ServerJs entities for server-side JavaScript code storage.
 * <p>
 * This repository extends typed repository interfaces for ServerJs persistence, providing
 * data access operations for JavaScript code executed by the GraalVM polyglot engine.
 * It manages server-side JavaScript code that can be stored, retrieved, and executed
 * within the OpenKoda platform. The repository provides lookups by code name, module
 * association, and organization context.
 * </p>
 * <p>
 * ServerJs entities are used by {@link com.openkoda.uicomponent.JsFlowRunner} for
 * script execution and {@link com.openkoda.uicomponent.FileSystemImpl} for polyglot
 * filesystem operations. The repository extends {@link ComponentEntityRepository} to
 * support module-scoped operations, enabling JavaScript code organization within
 * OpenKoda modules.
 * </p>
 * <p>
 * This is an unsecured repository (extends UnsecuredFunctionalRepositoryWithLongId),
 * meaning privilege checks are not automatically enforced on repository operations.
 * Security rules should be applied at the service layer when accessing ServerJs entities.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * ServerJs jsCode = serverJsRepository.findByName("myScript");
 * List&lt;Tuple&gt; allScripts = serverJsRepository.findAllAsTuple();
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ServerJs
 * @see com.openkoda.uicomponent.JsFlowRunner
 * @see com.openkoda.uicomponent.FileSystemImpl
 * @see ComponentEntityRepository
 * @see UnsecuredFunctionalRepositoryWithLongId
 */
@Repository
public interface ServerJsRepository extends UnsecuredFunctionalRepositoryWithLongId<ServerJs>, HasSecurityRules, ComponentEntityRepository<ServerJs> {

    /**
     * Finds a ServerJs entity by its unique name.
     * <p>
     * Retrieves server-side JavaScript code by its name identifier. The name
     * should be unique within the system. This method is commonly used to
     * locate specific JavaScript code for execution by the GraalVM engine.
     * </p>
     *
     * @param name the unique name of the ServerJs entity, must not be null
     * @return the ServerJs entity with the specified name, or null if not found
     */
    ServerJs findByName(String name);

    /**
     * Retrieves all ServerJs entities as lightweight tuples containing id and name.
     * <p>
     * Returns a list of tuples where each tuple contains the ServerJs entity's
     * database identifier (id) and name, ordered alphabetically by name. This
     * method is optimized for scenarios requiring only basic entity information
     * without loading complete entity graphs, such as populating dropdown lists
     * or building entity references.
     * </p>
     *
     * @return list of tuples containing (id, name) for all ServerJs entities,
     *         ordered by name alphabetically; empty list if no entities exist
     */
    @Query("select new com.openkoda.core.flow.Tuple(s.id, s.name) FROM ServerJs s order by name")
    List<Tuple> findAllAsTuple();

    /**
     * Deletes all ServerJs entities associated with a specific OpenKoda module.
     * <p>
     * Removes all server-side JavaScript code entries that belong to the specified
     * module. This operation is typically used during module uninstallation or
     * cleanup procedures. The deletion is executed as a bulk operation within a
     * single database transaction.
     * </p>
     * <p>
     * <b>Important:</b> This is a modifying query that requires an active transaction.
     * The method should be called within a transactional context (e.g., from a
     * service method annotated with @Transactional).
     * </p>
     *
     * @param module the OpenkodaModule whose associated ServerJs entities should
     *               be deleted, must not be null
     */
    @Modifying
    @Query("delete from ServerJs where module = :module")
    void deleteByModule(OpenkodaModule module);

}

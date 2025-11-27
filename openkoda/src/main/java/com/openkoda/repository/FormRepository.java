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

import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.OpenkodaModule;
import com.openkoda.model.component.Form;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Repository managing Form entities for dynamic form definitions and data capture.
 * <p>
 * This repository manages Form entities that define dynamic data models with validation rules.
 * Forms serve as metadata templates for runtime JPA entity generation via Byte Buddy class synthesis.
 * The DynamicEntityRegistrationService uses Form definitions to create database tables and corresponding
 * JPA entity classes at runtime, enabling the dynamic entity generation pipeline.
 * 
 * <p>
 * Extends UnsecuredFunctionalRepositoryWithLongId for functional repository operations,
 * HasSecurityRules for privilege-based access control integration, and ComponentEntityRepository
 * for module-scoped persistence semantics. Provides standard CRUD operations plus custom finders
 * for form retrieval by name, table name mapping queries, and module-scoped bulk deletions.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Form
 * @see com.openkoda.service.dynamicentity.DynamicEntityRegistrationService
 * @see UnsecuredFunctionalRepositoryWithLongId
 * @see ComponentEntityRepository
 */
@Repository
public interface FormRepository extends UnsecuredFunctionalRepositoryWithLongId<Form>, HasSecurityRules, ComponentEntityRepository<Form> {

    /**
     * Finds a Form entity by its unique name.
     * <p>
     * Uses Spring Data query derivation to locate forms by the name field.
     * Returns null if no form with the specified name exists.
     * 
     *
     * @param name the unique form name to search for, must not be null
     * @return the Form entity with the specified name, or null if not found
     */
    Form findByName(String name);
    
    /**
     * Retrieves all form names mapped to their corresponding database table names.
     * <p>
     * This default method converts the JPQL query results from getNameAndTableName() into
     * a convenient Map structure for quick name-to-table lookups. The stream processing
     * collects Object array pairs into a Map where form names are keys and table names are values.
     * 
     *
     * @return Map with form names as keys and table names as values, empty map if no forms exist
     */
    default Map<String,String> getNameAndTableNameAsMap(){
        return getNameAndTableName().stream().collect(toMap(o -> (String) o[0], o-> (String) o[1]));
    }
    
    /**
     * Executes JPQL projection query to retrieve form name and table name pairs.
     * <p>
     * Returns a list of Object arrays where each array contains two elements:
     * index 0 = form name (String), index 1 = table name (String).
     * Used by getNameAndTableNameAsMap() to build lookup maps for dynamic entity registration.
     * 
     *
     * @return List of Object arrays containing [name, tableName] pairs for all forms
     */
    @Query(value = "SELECT name, tableName FROM Form")
    List<Object[]> getNameAndTableName();

    /**
     * Bulk deletes all Form entities belonging to the specified module.
     * <p>
     * Executes a JPQL bulk delete operation to remove all forms associated with the given module.
     * This method is used during module uninstallation or cleanup operations to remove module-specific
     * form definitions. The @Modifying annotation indicates this is a write operation requiring a transaction.
     * 
     * <p>
     * <b>Warning:</b> Bulk delete operations bypass JPA lifecycle callbacks and cascade rules.
     * Ensure related entities (dynamic tables, entity metadata) are cleaned up separately if needed.
     * 
     *
     * @param module the OpenkodaModule whose forms should be deleted, must not be null
     */
    @Modifying
    @Query("delete from Form where module = :module")
    void deleteByModule(OpenkodaModule module);
}

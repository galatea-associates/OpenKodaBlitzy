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

package com.openkoda.core.lifecycle;

import com.openkoda.core.helper.SpringProfilesHelper;
import com.openkoda.core.multitenancy.QueryExecutor;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.Organization;
import com.openkoda.model.common.*;
import com.openkoda.repository.SearchableRepositories;
import com.openkoda.repository.SecureRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Formula;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.openkoda.model.common.ModelConstants.*;

/**
 * Creates and maintains the global_search_view database view for unified search functionality.
 * <p>
 * This component generates a database view that unions all searchable entities into a single
 * queryable structure. The view provides a standardized interface for searching across multiple
 * entity types with consistent column mappings for id, name, organization_id, timestamps,
 * description, privilege requirements, and URL paths.
 * </p>
 * <p>
 * The view creation process discovers all searchable repositories via the {@link SearchableRepositories}
 * registry and uses metadata annotations to construct appropriate SQL subqueries for each entity type.
 * Interface-based feature detection determines which columns are available for each entity
 * (e.g., {@link OrganizationRelatedEntity}, {@link TimestampedEntity}).
 * </p>
 * <p>
 * This component is invoked during application lifecycle events, typically at startup or during
 * maintenance operations. View creation uses CREATE OR REPLACE semantics, with optional DROP VIEW
 * behavior when not running in initialization profiles.
 * </p>
 * <p>
 * <b>Thread Safety:</b> This component should be invoked in a controlled manner during application
 * lifecycle events. Concurrent executions may result in database-level conflicts during DDL operations.
 * </p>
 * <p>
 * <b>Database Privileges:</b> Requires DDL privileges (CREATE VIEW, DROP VIEW) on the target database
 * schema to execute view creation and maintenance operations.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SearchableRepositories
 * @see SearchableRepositoryMetadata
 * @see QueryExecutor
 */
@Component
public class SearchViewCreator implements LoggingComponentWithRequestId {

    /**
     * Executes DDL statements transactionally for view creation and maintenance.
     * <p>
     * This executor handles the transactional execution of DROP VIEW and CREATE OR REPLACE VIEW
     * statements, ensuring database consistency during view maintenance operations.
     * </p>
     */
    @Inject
    protected QueryExecutor queryExecutor;

    /**
     * Creates or refreshes the global_search_view database view by discovering and unioning all searchable repositories.
     * <p>
     * This is the main entry point for view maintenance operations. The method performs the following steps:
     * </p>
     * <ol>
     *   <li>Discovers all searchable repositories via {@link SearchableRepositories#getSearchableRepositoriesWithEntityKeys()}</li>
     *   <li>Conditionally drops the existing view (only when not in initialization profile)</li>
     *   <li>Generates SQL subqueries for each searchable entity type</li>
     *   <li>Constructs a CREATE OR REPLACE VIEW statement with UNION of all subqueries</li>
     *   <li>Executes the DDL statement transactionally via {@link QueryExecutor}</li>
     * </ol>
     * <p>
     * <b>DROP VIEW Behavior:</b> When running outside initialization profiles (e.g., drop_and_init_database),
     * this method executes DROP VIEW IF EXISTS before creating the view. This prevents conflicts during
     * database initialization scenarios where the view might not exist yet.
     * </p>
     * <p>
     * <b>Transactional Execution:</b> All DDL operations are executed transactionally through
     * {@link QueryExecutor#runQueriesInTransaction(String)}, ensuring atomic view creation.
     * </p>
     *
     * @see SearchableRepositories#getSearchableRepositoriesWithEntityKeys()
     * @see SearchableRepositories#getGlobalSearchableRepositoryAnnotation(SecureRepository)
     * @see QueryExecutor#runQueriesInTransaction(String)
     */
    public void prepareSearchableRepositories() {
        debug("[prepareSearchableRepositories]");
        String queryString = "";

        if (not(SpringProfilesHelper.isInitializationProfile())) {
            queryString = "DROP VIEW IF EXISTS global_search_view; ";
        }

        Map<String, SecureRepository> repositories = SearchableRepositories.getSearchableRepositoriesWithEntityKeys();
        EntityManager em = null;

        List<String> queries = new ArrayList<>(repositories.size());

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, SecureRepository> r : repositories.entrySet()) {
            SearchableRepositoryMetadata gsa = SearchableRepositories.getGlobalSearchableRepositoryAnnotation(r.getValue());
            Class c = gsa.entityClass();
            String tableName = SearchableRepositories.discoverTableName(c);
            queries.add(prepareSubquery(c, tableName, gsa));
        }

        queryString += "CREATE OR REPLACE VIEW global_search_view AS " + StringUtils.join(queries, " union ");

        queryExecutor.runQueriesInTransaction(queryString);

    }

    /**
     * Generates a SQL subquery for a single entity type to be included in the global search view.
     * <p>
     * This method constructs a SELECT statement that maps entity-specific columns to the standardized
     * global_search_view schema. The subquery includes the following columns:
     * </p>
     * <ul>
     *   <li><b>id:</b> Entity primary key</li>
     *   <li><b>name:</b> Entity key from {@link SearchableRepositoryMetadata#entityKey()}</li>
     *   <li><b>organization_id:</b> Tenant identifier (from entity column or NULL for non-tenant entities)</li>
     *   <li><b>created_on:</b> Creation timestamp (from entity or NULL if not timestamped)</li>
     *   <li><b>updated_on:</b> Last update timestamp (from entity or NULL if not timestamped)</li>
     *   <li><b>description:</b> Searchable content from {@link SearchableRepositoryMetadata#searchIndexFormula()}</li>
     *   <li><b>requiredReadPrivilege:</b> Privilege formula extracted via {@link #getRequiredReadPrivilege(Class)}</li>
     *   <li><b>urlPath:</b> URL formula for entity access (organization-relative or global)</li>
     *   <li><b>indexString:</b> Entity index string for search optimization</li>
     * </ul>
     * <p>
     * <b>Interface-Based Feature Detection:</b> The method uses interface assignments to determine
     * available entity features:
     * </p>
     * <ul>
     *   <li>{@link OrganizationRelatedEntity}: Entity has organization_id (multi-tenant)</li>
     *   <li>{@link TimestampedEntity} or {@link OpenkodaEntity}: Entity has created_on/updated_on timestamps</li>
     *   <li>{@link EntityWithRequiredPrivilege}: Entity defines requiredReadPrivilege field</li>
     * </ul>
     * <p>
     * <b>Formula Resolution:</b> URL path formulas are resolved from metadata annotations with fallbacks:
     * organization-relative entities use organizationRelatedPathFormula or default pattern,
     * global entities use globalPathFormula or default pattern.
     * </p>
     *
     * @param c the entity class to generate a subquery for (e.g., Organization.class, User.class)
     * @param tableName the database table name discovered via {@link SearchableRepositories#discoverTableName(Class)}
     * @param gsa the searchable repository metadata annotation containing entity key, formulas, and configuration
     * @return a SQL subquery string in the format "(select id, 'entityKey' as name, ... from tableName)"
     *         ready for UNION with other entity subqueries
     */
    private String prepareSubquery(Class c, String tableName, SearchableRepositoryMetadata gsa) {
        debug("[prepareSubquery]");
        boolean isOrganizationRelated = (OrganizationRelatedEntity.class.isAssignableFrom(c));
        boolean isTimestamped = (TimestampedEntity.class.isAssignableFrom(c)) || (OpenkodaEntity.class.isAssignableFrom(c));
        String organizationColumnName = (Organization.class.isAssignableFrom(c) ? "id" : ModelConstants.ORGANIZATION_ID);
        String urlFormula = isOrganizationRelated ?
                StringUtils.defaultIfBlank(gsa.organizationRelatedPathFormula(),
                ORG_RELATED_PATH_FORMULA_BASE + gsa.entityKey() + ID_VIEW_PATH_FORMULA) :
                StringUtils.defaultIfBlank(gsa.globalPathFormula(),
                GLOBAL_PATH_FORMULA_BASE + gsa.entityKey() + ID_VIEW_PATH_FORMULA);

        return String.format(
            "(select id, '%s' as name, %s as " + ModelConstants.ORGANIZATION_ID
                    + ", %s as " + ModelConstants.CREATED_ON
                    + ", %s as " + ModelConstants.UPDATED_ON
                    + ", (%s) as description"
                    + ", %s as " + REQUIRED_READ_PRIVILEGE_COLUMN
                    + ", (%s) as urlPath"
                    + ", %s from %s)",
                gsa.entityKey(),
                (isOrganizationRelated ? organizationColumnName : "null \\:\\: bigint"),
                (isTimestamped ? ModelConstants.CREATED_ON : "null \\:\\: timestamp"),
                (isTimestamped ? ModelConstants.UPDATED_ON : "null \\:\\: timestamp"),
                gsa.searchIndexFormula(),
                getRequiredReadPrivilege(c),
                urlFormula,
                ModelConstants.INDEX_STRING_COLUMN,
                tableName);
    }

    /**
     * Extracts the requiredReadPrivilege field's @Formula annotation value from an entity class.
     * <p>
     * This method uses reflection to read the SQL formula that computes the required read privilege
     * for accessing an entity. The formula is defined in the entity's requiredReadPrivilege field
     * via Hibernate's {@link Formula} annotation.
     * </p>
     * <p>
     * <b>Privilege Resolution Logic:</b>
     * </p>
     * <ul>
     *   <li>If the class implements {@link EntityWithRequiredPrivilege}, the method attempts to
     *       reflectively access the "requiredReadPrivilege" field and extract its @Formula annotation value</li>
     *   <li>If the field is not found or the class doesn't implement the interface, returns "null" as SQL literal</li>
     * </ul>
     * <p>
     * <b>Exception Handling:</b> If {@link NoSuchFieldException} is thrown during reflective field access,
     * the exception is printed to standard error and "null" is returned as the default privilege value.
     * This ensures view creation continues even when privilege formulas are missing.
     * </p>
     *
     * @param c the entity class to extract privilege formula from (e.g., Organization.class)
     * @return the SQL formula string from the @Formula annotation (e.g., "CASE WHEN removable THEN 'canAccessGlobalSettings' END"),
     *         or the string literal "null" if no privilege is defined or field is missing
     */
    private String getRequiredReadPrivilege(Class c){
        String requiredReadPrivilege = "null";
        if(EntityWithRequiredPrivilege.class.isAssignableFrom(c)){
           try {
               requiredReadPrivilege = c.getDeclaredField("requiredReadPrivilege").getAnnotation(Formula.class).value();
           } catch (NoSuchFieldException e) {
               e.printStackTrace();
           }
        }
        return requiredReadPrivilege;
    }
}

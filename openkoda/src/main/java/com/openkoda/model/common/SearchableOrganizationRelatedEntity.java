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

package com.openkoda.model.common;

/**
 * Composite marker interface combining organization-scoped entities with full-text search capabilities.
 * <p>
 * This interface extends both {@link OrganizationRelatedEntity} and {@link SearchableEntity}, providing
 * a unified contract for entities that require both tenant isolation and search indexing functionality.
 * Entities implementing this interface support organization-scoped data access (multi-tenancy) and 
 * full-text search through indexed string fields.
 * </p>
 * <p>
 * <b>Inherited Functionality:</b>
 * </p>
 * <ul>
 *   <li>From {@link OrganizationRelatedEntity}: Provides {@code getOrganizationId()} for tenant isolation
 *       and {@code getReferenceString()} for human-readable organization context</li>
 *   <li>From {@link SearchableEntity}: Provides {@code getIndexString()} containing concatenated searchable
 *       fields for full-text search operations</li>
 * </ul>
 * <p>
 * <b>Usage Pattern:</b><br>
 * Domain entities that need to be scoped to specific organizations and searchable via text queries should
 * implement this interface. The primary implementer is {@link com.openkoda.model.OpenkodaEntity}, which
 * serves as the base class for most organization-scoped, searchable entities in the system.
 * </p>
 * <p>
 * <b>Implementation Requirements:</b>
 * </p>
 * <ul>
 *   <li>Implementers must provide organization-scoped data isolation by maintaining a valid organization ID</li>
 *   <li>Implementers must maintain an {@code indexString} field containing all searchable content for
 *       full-text search functionality</li>
 *   <li>The {@code indexString} should be updated automatically when searchable fields change</li>
 *   <li>Repositories for implementing entities should leverage {@link com.openkoda.repository.SearchableRepositoryMetadata}
 *       for search index configuration</li>
 * </ul>
 * <p>
 * This is a marker interface with no additional methods beyond those inherited from its parent interfaces.
 * All behavior is provided through the combination of {@link OrganizationRelatedEntity} and 
 * {@link SearchableEntity} contracts.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see OrganizationRelatedEntity
 * @see SearchableEntity
 * @see com.openkoda.model.OpenkodaEntity
 * @see com.openkoda.repository.SearchableRepositoryMetadata
 */
public interface SearchableOrganizationRelatedEntity extends OrganizationRelatedEntity, SearchableEntity {

}

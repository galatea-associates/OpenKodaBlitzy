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

/**
 * Central Spring Data JPA persistence layer providing repository interfaces, privilege enforcement, and search indexing.
 * <p>
 * This package contains lightweight repository interfaces (stateless contracts), DI aggregation holders,
 * security/scoping wrappers, native-SQL utilities, and discovery/metadata components that integrate Spring Data,
 * Hibernate/JPA, Spring Security expressions, and custom search indexing (SearchableRepositoryMetadata).

 *
 * <b>Package Structure</b>
 * <ul>
 *   <li><b>Base Repository Interfaces:</b> ComponentEntityRepository, SecureRepository providing foundational contracts</li>
 *   <li><b>Aggregators:</b> Repositories, SecureRepositories, UnsecureRepositories for simplified DI</li>
 *   <li><b>Security Wrappers:</b> SecureRepositoryWrapper, SecureRepository interface for privilege enforcement</li>
 *   <li><b>Utilities:</b> NativeQueries (native SQL execution), AliasToEntityHashMapResultTransformer (result mapping)</li>
 *   <li><b>Discovery:</b> SearchableRepositories, SecureEntityDictionaryRepository for metadata resolution</li>
 *   <li><b>Domain Repositories:</b> ControllerEndpoint, DbVersion, DynamicEntity, Form, FrontendResource, etc.</li>
 *   <li><b>Marker Interfaces:</b> Secure*Repository interfaces with SearchableRepositoryMetadata annotations</li>
 * </ul>
 *
 * <b>Key Components</b>
 *
 * <b>Repositories Aggregator Pattern</b>
 * <p>
 * {@link Repositories} provides centralized access via secure/unsecure groups:
 * <pre>
 *   &#64;Autowired Repositories repositories;
 *   repositories.secure.user.findOne(userId);  // Privilege-enforced
 *   repositories.unsecure.organization.findById(id);  // Direct access
 * </pre>

 *
 * <b>SecureRepository Privilege Enforcement</b>
 * <p>
 * {@link com.openkoda.repository.SecureRepository} interface wraps repository operations with DEFAULT_SCOPE = USER:
 * <ul>
 *   <li>All operations enforce read/write privilege checks</li>
 *   <li>Privileges computed from entity @Formula annotations (requiredReadPrivilege, requiredWritePrivilege)</li>
 *   <li>Throws AccessDeniedException if user lacks required privilege</li>
 *   <li>Use scoped(SecurityScope) for temporary scope elevation (ORGANIZATION, GLOBAL)</li>
 * </ul>

 *
 * <b>SearchableRepositories Discovery</b>
 * <p>
 * {@link SearchableRepositories} discovers SecureRepository beans at startup:
 * <ul>
 *   <li>Enforces SearchableRepositoryMetadata annotation presence</li>
 *   <li>Resolves table names via @Table or Hibernate naming strategy</li>
 *   <li>Caches UPDATE SQL templates for index maintenance</li>
 *   <li>Supports runtime plugin registration</li>
 * </ul>

 *
 * <b>NativeQueries Utilities</b>
 * <p>
 * {@link NativeQueries} provides native SQL execution:
 * <ul>
 *   <li>createTable/createTableSql - Dynamic table DDL generation</li>
 *   <li>runUpdateQuery - SQL script execution via ScriptUtils + JDBC</li>
 *   <li>ifTableExists - PostgreSQL pg_tables catalog check</li>
 *   <li>runReadOnly - Native SELECT with LinkedHashMap transformation</li>
 * </ul>

 *
 * <b>Subpackages</b>
 * <ul>
 *   <li>{@link com.openkoda.repository.admin} - Audit logging repositories</li>
 *   <li>{@link com.openkoda.repository.ai} - AI/ML query report repositories</li>
 *   <li>{@link com.openkoda.repository.event} - Event listener and scheduler repositories</li>
 *   <li>{@link com.openkoda.repository.file} - File storage repositories</li>
 *   <li>{@link com.openkoda.repository.notifications} - Notification repositories</li>
 *   <li>{@link com.openkoda.repository.organization} - Organization/tenant repositories</li>
 *   <li>{@link com.openkoda.repository.specifications} - JPA Criteria API Specification builders</li>
 *   <li>{@link com.openkoda.repository.task} - Task/email/HTTP request repositories</li>
 *   <li>{@link com.openkoda.repository.user} - User, role, privilege repositories</li>
 * </ul>
 *
 * <b>Usage Patterns</b>
 *
 * <b>Standard Repository Injection</b>
 * <pre>
 *   &#64;Autowired
 *   private SecureUserRepository userRepository;
 *   
 *   User user = userRepository.findOne(userId);  // Privilege-checked
 * </pre>
 *
 * <b>Aggregator Pattern (Legacy)</b>
 * <pre>
 *   &#64;Autowired
 *   Repositories repositories;
 *   
 *   List&lt;Organization&gt; orgs = repositories.secure.organization.search("name", "Acme");
 * </pre>
 *
 * <b>Temporary Scope Elevation</b>
 * <pre>
 *   SecureRepositoryWrapper&lt;User&gt; orgScope = 
 *       repositories.secure.user.scoped(SecurityScope.ORGANIZATION);
 *   List&lt;User&gt; allOrgUsers = orgScope.findAll();  // Org-admin scope
 * </pre>
 *
 * <b>Native SQL Execution</b>
 * <pre>
 *   &#64;Autowired
 *   NativeQueries nativeQueries;
 *   
 *   List&lt;LinkedHashMap&lt;String, Object&gt;&gt; results = 
 *       nativeQueries.runReadOnly("SELECT id, name FROM users");
 * </pre>
 *
 * <b>Design Principles</b>
 * <ul>
 *   <li><b>Stateless Contracts:</b> All repository interfaces proxied by Spring Data at startup</li>
 *   <li><b>Privilege-First:</b> Secure repositories default to user-level access control</li>
 *   <li><b>Metadata-Driven:</b> SearchableRepositoryMetadata enables dynamic discovery</li>
 *   <li><b>PostgreSQL-Optimized:</b> Native queries leverage PostgreSQL system catalogs</li>
 *   <li><b>Aggregation for Convenience:</b> Repositories class simplifies multi-repository injection</li>
 * </ul>
 *
 * <b>Dependencies</b>
 * <ul>
 *   <li>Spring Data JPA - Repository proxying and query derivation</li>
 *   <li>Hibernate/JPA - ORM and native query execution</li>
 *   <li>Spring Security - HasSecurityRules integration for privilege enforcement</li>
 *   <li>com.openkoda.model - Entity classes and SearchableEntity interface</li>
 *   <li>com.openkoda.core.repository - Base repository interfaces (SearchableFunctionalRepositoryWithLongId)</li>
 * </ul>
 *
 * <b>Relationships</b>
 * <ul>
 *   <li><b>Depends on:</b> com.openkoda.model (entity classes), com.openkoda.core.repository (base interfaces)</li>
 *   <li><b>Used by:</b> com.openkoda.service (business logic), com.openkoda.controller (request handling)</li>
 * </ul>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.springframework.data.jpa.repository.JpaRepository
 * @see com.openkoda.model.common.SearchableEntity
 * @see com.openkoda.core.security.HasSecurityRules
 */
package com.openkoda.repository;
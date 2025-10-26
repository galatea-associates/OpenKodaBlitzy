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

import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.model.component.ServerJs;
import org.springframework.stereotype.Repository;

import static com.openkoda.controller.common.URLConstants.SERVERJS;

/**
 * Secure repository marker interface for ServerJs JavaScript code entities with SearchableRepositoryMetadata.
 * <p>
 * Extends SecureRepository&lt;ServerJs&gt; to provide privilege-enforced repository operations for server-side
 * JavaScript code entities. This interface adds metadata capabilities for code search and GraalVM script indexing
 * through the {@code @SearchableRepositoryMetadata} annotation.
 * </p>
 * <p>
 * The repository enables secure management of JavaScript code fragments that are executed by the GraalVM
 * JavaScript engine. It supports both storage and retrieval of server-side scripts while enforcing
 * organization-scoped access control through the privilege system.
 * </p>
 * <p>
 * This repository is primarily used by JsFlowRunner for loading JavaScript flows and by FileSystemImpl
 * for providing polyglot filesystem access to JavaScript contexts.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * SecureServerJsRepository repo = secureRepositories.serverJs;
 * Optional&lt;ServerJs&gt; script = repo.findOne(scriptId);
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SecureRepository
 * @see ServerJs
 * @see SearchableRepositoryMetadata
 * @see com.openkoda.uicomponent.JsFlowRunner
 * @see com.openkoda.uicomponent.FileSystemImpl
 */
@Repository
@SearchableRepositoryMetadata(
        entityKey = "serverJs",
        descriptionFormula = "(''||name)",
        entityClass = ServerJs.class
)
public interface SecureServerJsRepository extends SecureRepository<ServerJs> {


}

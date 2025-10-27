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

package com.openkoda.repository.user;

import com.openkoda.core.repository.common.FunctionalRepositoryWithLongId;
import com.openkoda.model.authentication.ApiKey;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository managing ApiKey entities for stateless API authentication.
 * <p>
 * This interface extends {@link FunctionalRepositoryWithLongId} to provide typed DI contract
 * for ApiKey persistence. It serves as a marker repository with no custom query methods,
 * relying on inherited CRUD operations (save, findById, findAll, delete) from the base interface.
 * ApiKey entities store authentication tokens for API clients enabling stateless authentication
 * without session management.
 * </p>
 * <p>
 * Usage: API authentication filters lookup keys by value, validate expiration, and authenticate
 * requests. Keys are typically scoped to organizations for multi-tenant API access control.
 * </p>
 * <p>
 * Persists to 'api_key' table with columns: id (PK), key_value (unique), user_id (FK), 
 * organization_id (FK), expiration_date, created_on.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see ApiKey
 * @see FunctionalRepositoryWithLongId
 * @see com.openkoda.core.security.ApiKeyAuthenticationFilter
 */
@Repository
public interface ApiKeyRepository extends FunctionalRepositoryWithLongId<ApiKey> {

}
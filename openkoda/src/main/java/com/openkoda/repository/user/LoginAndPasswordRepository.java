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

import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.authentication.LoginAndPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository managing LoginAndPassword entities for username/password authentication credentials.
 * <p>
 * This interface extends {@link JpaRepository} and implements {@link HasSecurityRules} to provide
 * typed DI contract for credential persistence. It serves as a marker repository with no custom query
 * methods, relying on inherited CRUD operations (save, findById, findAll, delete) from JpaRepository.
 * LoginAndPassword entities store hashed passwords (bcrypt) and authentication metadata for local
 * username/password authentication.
 * </p>
 * <p>
 * Usage: Authentication filters and services load credentials by login field (typically via custom
 * service methods), verify passwords using {@link org.springframework.security.crypto.password.PasswordEncoder},
 * and establish authenticated sessions upon success.
 * </p>
 * <p>
 * Persists to 'login_and_password' table with columns: id (PK), login (unique), password (bcrypt hash),
 * user_id (FK), password_expiration_date, failed_login_attempts.
 * </p>
 *
 * @author Martyna Litkowska (mlitkowska@stratoflow.com)
 * @version 1.7.1
 * @since 2019-07-18
 * @see LoginAndPassword
 * @see JpaRepository
 * @see HasSecurityRules
 * @see org.springframework.security.crypto.password.PasswordEncoder
 */
@Repository
public interface LoginAndPasswordRepository extends JpaRepository<LoginAndPassword, Long>, HasSecurityRules {

}

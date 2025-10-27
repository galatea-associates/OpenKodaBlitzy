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

import com.openkoda.core.flow.Tuple;
import com.openkoda.core.repository.common.FunctionalRepositoryWithLongId;
import com.openkoda.model.Token;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.codec.binary.Base64;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.Optional;

/**
 * Spring Data JPA repository managing Token entities for session, JWT, and remember-me tokens.
 * <p>
 * This interface extends {@link FunctionalRepositoryWithLongId} to provide repository operations
 * for Token entities used in authentication and authorization flows. Tokens support session management,
 * password reset, email verification, and remember-me functionality with expiration tracking.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Derived finder {@link #findByTokenAndIsValidTrue(String)} for valid token lookup</li>
 *   <li>JPQL projection {@link #findByUserIdAndTokenWithInvalidationReasons(Long, String)} returning Tuple with validation booleans (used, expired)</li>
 *   <li>Base64 token helper {@link #findByBase64UserIdTokenIsValidTrue(String)} decoding "userId:token" format and validating</li>
 *   <li>Error constants: {@link #INVALID}, {@link #ALREADY_USED}, {@link #EXPIRED} for validation messages</li>
 *   <li>Uses Apache Commons Codec Base64 for decoding and Reactor Tuple2 for multi-value returns</li>
 * </ul>
 * </p>
 * <p>
 * Note: Contains business logic in default method {@link #findByBase64UserIdTokenIsValidTrue(String)} with TODO
 * comment indicating it should be moved to service layer per architecture rule 3.3.
 * </p>
 * <p>
 * Persists to 'token' table with columns: id (PK), token (unique), user_id (FK), expires_on, used (boolean), created_on.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see Token
 * @see FunctionalRepositoryWithLongId
 * @see org.apache.commons.codec.binary.Base64
 * @see reactor.util.function.Tuple2
 */
@Repository
public interface TokenRepository extends FunctionalRepositoryWithLongId<Token> {

    /**
     * Finds valid Token entity by token string value.
     * <p>
     * Uses Spring Data query derivation: {@code SELECT * FROM token WHERE token = ? AND is_valid = true}.
     * Only returns tokens with is_valid flag set to true (not used, not expired).
     * </p>
     *
     * @param token Token string value to search for, must not be null
     * @return Optional containing Token if found and valid, empty Optional otherwise
     */
    Optional<Token> findByTokenAndIsValidTrue(String token);

    /**
     * Retrieves Token with validation status booleans (used, expired) in Tuple projection.
     * <p>
     * Executes JPQL constructor expression:
     * {@code SELECT new Tuple(t, t.used, t.expiresOn < CURRENT_TIMESTAMP) FROM Token t WHERE userId = ? AND token = ?}
     * </p>
     * <p>
     * Returns Tuple containing:
     * <ul>
     *   <li>T1: Token entity</li>
     *   <li>T2: Boolean - token.used flag (true if already used)</li>
     *   <li>T3: Boolean - expired flag (true if expiresOn < current timestamp)</li>
     * </ul>
     * Used by {@link #findByBase64UserIdTokenIsValidTrue(String)} to determine specific invalidation reason.
     * </p>
     *
     * @param userId User ID associated with token, must not be null
     * @param token Token string value, must not be null
     * @return Tuple containing Token and validation booleans, null if token not found
     */
    @Query("select new com.openkoda.core.flow.Tuple(t, t.used, t.expiresOn < CURRENT_TIMESTAMP) FROM " +
            "Token t WHERE (t.userId = :userId AND t.token = :token)")
    Tuple findByUserIdAndTokenWithInvalidationReasons(@Param("userId") Long userId, @Param("token") String token);
    
    /**
     * Finds Token entity by user ID and token string combination.
     * <p>
     * Uses Spring Data query derivation: {@code SELECT * FROM token WHERE user_id = ? AND token = ?}.
     * Returns token regardless of validation status (may be used or expired).
     * </p>
     *
     * @param userId User ID associated with token, must not be null
     * @param token Token string value, must not be null
     * @return Optional containing Token if found, empty Optional otherwise
     */
    Optional<Token> findByUserIdAndToken(Long userId, String token);

    /**
     * Error message constant for invalid token format or non-existent tokens.
     * Returned when base64 decoding fails, userId:token format invalid, or token not found in database.
     */
    String INVALID = "Invalid token";
    
    /**
     * Error message constant for already-used tokens.
     * Returned when token.used flag is true. Suggests token has been consumed in previous operation.
     */
    String ALREADY_USED = "This link has expired.";
    
    /**
     * Error message constant for expired tokens.
     * Returned when token.expiresOn < CURRENT_TIMESTAMP. Includes guidance to request new link.
     */
    String EXPIRED = "This link has expired. You may contact your team members to get a new link.";

    /**
     * Decodes base64-encoded "userId:token" string and validates token with detailed error messaging.
     * <p>
     * Validation pipeline:
     * <ol>
     *   <li>Checks base64UserIdToken not null (returns INVALID if null)</li>
     *   <li>Validates base64 encoding via {@link Base64#isBase64(byte[])} (returns INVALID if invalid)</li>
     *   <li>Decodes and splits on ':' expecting "userId:token" format (returns INVALID if malformed)</li>
     *   <li>Parses userId as Long (returns INVALID if not numeric)</li>
     *   <li>Queries {@link #findByUserIdAndTokenWithInvalidationReasons(Long, String)} for validation booleans</li>
     *   <li>Checks token.used flag (returns ALREADY_USED if true)</li>
     *   <li>Checks expiration (returns EXPIRED if expiresOn < current timestamp)</li>
     *   <li>Returns valid Token with empty error message if all checks pass</li>
     * </ol>
     * </p>
     * <p>
     * Uses Apache Commons Codec {@link Base64#decodeBase64(String)} for decoding.
     * Returns {@link Tuple2} with Token (null if invalid) and error message (empty string if valid).
     * </p>
     * <p>
     * <b>TODO:</b> Rule 3.3 - Repository must not have business logic code. This validation logic
     * should be moved to a service layer method (TokenService) per architecture guidelines.
     * </p>
     * <p>
     * Usage: Password reset links, email verification tokens, remember-me authentication.
     * </p>
     *
     * @param base64UserIdToken Base64-encoded string in "userId:token" format, may be null
     * @return Tuple2 with Token (null if invalid) and error message (INVALID, ALREADY_USED, EXPIRED, or empty string)
     * @throws NumberFormatException caught internally, returns INVALID tuple
     */
    //TODO Rule 3.3: Repository must not have business logic code.
    default @NotNull Tuple2<Token, String /* error message */> findByBase64UserIdTokenIsValidTrue(String base64UserIdToken) {
        boolean isBasicHeader = base64UserIdToken != null;
        if (!isBasicHeader) { return Tuples.of(null, INVALID); }
        String base64 = base64UserIdToken.trim();
        boolean isBase64 = Base64.isBase64(base64);
        if (!isBase64) { return Tuples.of(null, INVALID); }
        String[] idAndToken = new String(Base64.decodeBase64(base64)).split(":", 2);
        boolean isIdAndToken = idAndToken.length == 2;
        if (!isIdAndToken) { return Tuples.of(null, INVALID); }
        String idString = idAndToken[0];
        String tokenString = idAndToken[1];
        Long id;
        try {
            id = Long.parseLong(idString);
        } catch (NumberFormatException e) {
            //id is not a number;
            return Tuples.of(null, INVALID);
        }
        Tuple t = this.findByUserIdAndTokenWithInvalidationReasons(id, tokenString);
        if (t == null) {
            return Tuples.of(null, INVALID);
        }

        Tuple3<Token, Boolean, Boolean> tokenWithInvalidationReasons = t.getT3();
        if (tokenWithInvalidationReasons.getT2()) {
            return Tuples.of(null, ALREADY_USED);
        }
        if (tokenWithInvalidationReasons.getT3()) {
            return Tuples.of(null, EXPIRED);
        }
        return Tuples.of(tokenWithInvalidationReasons.getT1(), "");
    }


}

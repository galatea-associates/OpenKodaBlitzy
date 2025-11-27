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

package com.openkoda.model.authentication;

import com.openkoda.model.common.AuditableEntity;
import com.openkoda.model.common.TimestampedEntity;

/**
 * Abstract base class for all authentication entity types in the OpenKoda system.
 * <p>
 * This class serves as the common superclass for all concrete authentication implementations
 * including username/password authentication ({@link LoginAndPassword}), API key authentication
 * ({@link ApiKey}), and all OAuth provider integrations ({@link FacebookUser}, {@link GoogleUser},
 * {@link LinkedinUser}, {@link SalesforceUser}), as well as enterprise directory authentication
 * ({@link LDAPUser}).

 * <p>
 * <b>Design Pattern:</b> Implements the Template Method pattern, providing shared audit trail
 * functionality through inheritance while requiring concrete subclasses to implement
 * authentication-specific persistence and identification logic.

 * <p>
 * <b>Inheritance Structure:</b>
 * <ul>
 * <li>Extends {@link TimestampedEntity} for automatic timestamp management (createdOn, updatedOn)</li>
 * <li>Implements {@link AuditableEntity} for audit logging integration</li>
 * </ul>

 * <p>
 * <b>JPA Mapping:</b> This is an abstract entity not mapped to any database table. Concrete
 * subclasses provide their own {@code @Entity} and {@code @Table} annotations for persistence.
 * All subclasses use the @MapsId pattern for one-to-one shared primary key relationships with
 * the User entity.

 * <p>
 * <b>AuthenticationMethods Enum:</b> Contains enumeration of all supported authentication
 * mechanisms in the system:
 * <ul>
 * <li>{@code PASSWORD} - Local username/password authentication via LoginAndPassword entity</li>
 * <li>{@code API_KEY} - Programmatic API key authentication via ApiKey entity</li>
 * <li>{@code SOCIAL_GOOGLE} - Google OAuth 2.0 authentication via GoogleUser entity</li>
 * <li>{@code SOCIAL_SALESFORCE} - Salesforce OAuth 2.0 authentication via SalesforceUser entity</li>
 * <li>{@code SOCIAL_FACEBOOK} - Facebook OAuth 2.0 authentication via FacebookUser entity</li>
 * <li>{@code SOCIAL_LINKEDIN} - LinkedIn OAuth 2.0 authentication via LinkedinUser entity</li>
 * <li>{@code LDAP} - Enterprise LDAP/Active Directory authentication via LDAPUser entity</li>
 * <li>{@code TOKEN} - Token-based authentication for temporary access grants</li>
 * </ul>

 * <p>
 * <b>Usage Requirements:</b> Concrete subclasses must implement:
 * <ul>
 * <li>{@code toAuditString()} - Returns concise string representation for audit trail</li>
 * <li>{@code getId()} - Returns entity primary key (typically shared with User.id via @MapsId)</li>
 * </ul>

 * <p>
 * <b>Thread Safety:</b> This base class does not introduce thread-safety concerns. Individual
 * subclass implementations should document their own thread-safety guarantees, particularly
 * regarding static PasswordEncoder initialization in LoginAndPassword and ApiKey entities.

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see TimestampedEntity
 * @see AuditableEntity
 * @see LoginAndPassword
 * @see ApiKey
 * @see FacebookUser
 * @see GoogleUser
 * @see LinkedinUser
 * @see SalesforceUser
 * @see LDAPUser
 */
public abstract class LoggedUser extends TimestampedEntity implements AuditableEntity {
    public enum AuthenticationMethods {
        PASSWORD, API_KEY, SOCIAL_GOOGLE, SOCIAL_SALESFORCE, SOCIAL_FACEBOOK, SOCIAL_LINKEDIN, LDAP, TOKEN
    }

}

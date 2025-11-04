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

package com.openkoda.model;

import com.openkoda.core.security.OrganizationUser;
import com.openkoda.dto.user.BasicUser;
import com.openkoda.model.authentication.*;
import com.openkoda.model.common.*;
import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
/**
 * Core user authentication and authorization entity representing system users with multi-organization membership and role-based access control.
 * <p>
 * Persisted to 'users' table with unique email constraint. Central entity for user management supporting authentication via 
 * multiple providers (local password, Facebook, Google, LinkedIn, LDAP, Salesforce). Users can be associated with multiple 
 * organizations through UserRole join entities, enabling multi-tenant user access. Implements {@code @DynamicUpdate} for 
 * selective column updates. Stores user profile data (firstName, lastName, computed name via {@code @Formula}), authentication 
 * state (enabled, tokenExpired), notification preferences (emailNotificationsEnabled), and language settings. Uses 
 * GLOBAL_ID_GENERATOR sequence with initial value 10000, allocationSize 10.
 * </p>
 * <p>
 * <b>Authentication:</b> Supports multiple authentication methods via one-to-one relationships: FacebookUser, GoogleUser, 
 * LinkedinUser, LDAPUser, SalesforceUser, and local LoginAndPassword.
 * </p>
 * <p>
 * <b>Multi-tenancy:</b> Implements IsManyOrganizationsRelatedEntity for cross-organizational user access. UserRole entities 
 * link users to organizations with specific roles.
 * </p>
 * <p>
 * <b>Computed fields:</b> {@code @Formula}-derived name (first_name || ' ' || last_name), requiredReadPrivilege, 
 * requiredWritePrivilege, indexString (database-generated).
 * </p>
 * <p>
 * <b>Audit:</b> Extends TimestampedEntity for createdOn/updatedOn timestamps via Spring Data auditing.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see UserRole for user-organization-role association
 * @see Role for privilege definitions
 * @see Organization for tenant entities
 * @see LoginAndPassword for local authentication
 */
@DynamicUpdate
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = ModelConstants.EMAIL))
public class User extends TimestampedEntity implements AuditableEntity, SearchableEntity, EntityWithRequiredPrivilege, IsManyOrganizationsRelatedEntity {

    /**
     * Primary key from seqGlobalId sequence, allocationSize 10 for batch allocation.
     */
    @Id
    @SequenceGenerator(name = GLOBAL_ID_GENERATOR, sequenceName = GLOBAL_ID_GENERATOR, initialValue = ModelConstants.INITIAL_GLOBAL_VALUE, allocationSize = 10)
    @GeneratedValue(generator = ModelConstants.GLOBAL_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * User profile first name component. Nullable.
     */
    private String firstName;
    
    /**
     * User profile last name component. Nullable.
     */
    private String lastName;

    /**
     * {@code @Formula} computed field concatenating first_name and last_name with space. Used for display and search.
     */
    @Formula("(coalesce(first_name, '')||' '||coalesce(last_name, ''))")
    private String name;

    /**
     * Unique user email, serves as primary identifier. Unique constraint enforced at database level.
     */
    @Column(name = ModelConstants.EMAIL)
    private String email;

    /**
     * Account activation flag. Disabled users cannot authenticate.
     */
    private boolean enabled;
    
    /**
     * Authentication token expiration flag for password reset/verification flows.
     */
    private boolean tokenExpired;

    /**
     * User's preferred language code for localization.
     */
    @Column
    private String language;

    /**
     * Eager-loaded collection of UserRole associations linking user to organizations with specific roles. 
     * Empty collection if user has no role assignments.
     */
    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, targetEntity = UserRole.class)
    private Collection<UserRole> roles = Collections.emptyList();

    /**
     * One-to-one optional relationship for Facebook authentication provider integration. Cascade ALL for lifecycle management. 
     * Null if user doesn't use Facebook authentication.
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    @PrimaryKeyJoinColumn
    private FacebookUser facebookUser;

    /**
     * One-to-one optional relationship for Google authentication provider integration. Cascade ALL for lifecycle management. 
     * Null if user doesn't use Google authentication.
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    @PrimaryKeyJoinColumn
    private GoogleUser googleUser;

    /**
     * One-to-one relationship for local password authentication. Null for users authenticating via external providers only.
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    @PrimaryKeyJoinColumn
    private LoginAndPassword loginAndPassword;

    /**
     * One-to-one optional relationship for LDAP authentication provider integration. Cascade ALL for lifecycle management. 
     * Null if user doesn't use LDAP authentication.
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    @PrimaryKeyJoinColumn
    private LDAPUser ldapUser;

    /**
     * One-to-one optional relationship for Salesforce authentication provider integration. Cascade ALL for lifecycle management. 
     * Null if user doesn't use Salesforce authentication.
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    @PrimaryKeyJoinColumn
    private SalesforceUser salesforceUser;

    /**
     * One-to-one optional relationship for LinkedIn authentication provider integration. Cascade ALL for lifecycle management. 
     * Null if user doesn't use LinkedIn authentication.
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    @PrimaryKeyJoinColumn
    private LinkedinUser linkedinUser;

    /**
     * One-to-one relationship to API key for programmatic authentication. Null if user has no API key assigned.
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    @PrimaryKeyJoinColumn
    private ApiKey apiKey;

    /**
     * User profile picture URL from authentication provider or uploaded image. Nullable.
     */
    private String picture;

    /**
     * Timestamp of user's last successful login. Used for activity tracking and security auditing.
     */
    @Column
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime lastLogin;

    /**
     * {@code @Formula}-derived privilege requirement for reading user data. Computes to '_readUserData' privilege token 
     * or NULL for placeholder user. Used for access control checks.
     */
    @Formula("( CASE id WHEN " + ModelConstants.USER_ID_PLACEHOLDER + " THEN NULL ELSE '" + PrivilegeNames._readUserData + "' END )")
    private String requiredReadPrivilege;

    /**
     * {@code @Formula}-derived privilege requirement for modifying user data. Computes to '_manageUserData' privilege token. 
     * Used for access control checks.
     */
    @Formula("( '" + PrivilegeNames._manageUserData + "' )")
    private String requiredWritePrivilege;

    /**
     * {@code @ElementCollection} Map persisted to user_property join table. Flexible key-value storage for user-specific 
     * configuration and custom properties without schema changes.
     */
    @ElementCollection
    @CollectionTable(name = "user_property",
            joinColumns = {
                    @JoinColumn(name = "user_id", referencedColumnName = "id")
            })
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    private Map<String, String> properties = new HashMap<>();


    /**
     * Default constructor for JPA and frameworks.
     */
    public User() {
    }

    /**
     * Constructs User from Google authentication provider. Initializes email (lowercased), name, firstName, lastName, 
     * picture from GoogleUser. Sets enabled=true and establishes GoogleUser association.
     *
     * @param googleUser the Google authentication provider entity containing user profile data
     */
    public User(GoogleUser googleUser) {
        this.email = googleUser.getEmail().toLowerCase();
        this.name = googleUser.getName();
        this.firstName = googleUser.getFirstName();
        this.lastName = googleUser.getLastName();
        this.picture = googleUser.getPicture();
        this.googleUser = googleUser;
        this.enabled = true;
    }

    /**
     * Constructs User from Facebook authentication provider. Initializes email (lowercased), name, firstName, lastName, 
     * picture from FacebookUser. Sets enabled=true and establishes FacebookUser association.
     *
     * @param facebookUser the Facebook authentication provider entity containing user profile data
     */
    public User(FacebookUser facebookUser) {
        this.email = facebookUser.getEmail().toLowerCase();
        this.name = facebookUser.getName();
        this.firstName = facebookUser.getFirstName();
        this.lastName = facebookUser.getLastName();
        this.picture = facebookUser.getPicture();
        this.facebookUser = facebookUser;
        this.enabled = true;
    }

    /**
     * Constructs User from LDAP authentication provider. Initializes email (lowercased), name (from cn), firstName (from givenName), 
     * lastName (from sn). Sets enabled=true and establishes LDAPUser association.
     *
     * @param ldapUser the LDAP authentication provider entity containing user directory data
     */
    public User(LDAPUser ldapUser) {
        this.email = ldapUser.getEmail().toLowerCase();
        this.name = ldapUser.getCn();
        this.firstName = ldapUser.getGivenName();
        this.lastName = ldapUser.getSn();
        this.ldapUser = ldapUser;
        this.enabled = true;
    }

    /**
     * Constructs User from Salesforce authentication provider. Initializes email (lowercased), name, firstName, lastName, 
     * picture from SalesforceUser. Sets enabled=true and establishes SalesforceUser association.
     *
     * @param salesforceUser the Salesforce authentication provider entity containing user profile data
     */
    public User(SalesforceUser salesforceUser) {
        this.email = salesforceUser.getEmail().toLowerCase();
        this.name = salesforceUser.getName();
        this.firstName = salesforceUser.getFirstName();
        this.lastName = salesforceUser.getLastName();
        this.picture = salesforceUser.getPicture();
        this.salesforceUser = salesforceUser;
        this.enabled = true;
    }

    /**
     * Constructs User from LinkedIn authentication provider. Initializes email (lowercased), firstName, lastName, 
     * picture (from profilePicture). Sets enabled=true and establishes LinkedinUser association.
     *
     * @param linkedinUser the LinkedIn authentication provider entity containing user profile data
     */
    public User(LinkedinUser linkedinUser) {
        this.email = linkedinUser.getEmail().toLowerCase();
        this.firstName = linkedinUser.getFirstName();
        this.lastName = linkedinUser.getLastName();
        this.picture = linkedinUser.getProfilePicture();
        this.linkedinUser = linkedinUser;
        this.enabled = true;
    }

    /**
     * Constructs User with basic profile data. Email is lowercased for uniqueness constraint.
     *
     * @param firstName user's first name, may be null
     * @param lastName user's last name, may be null
     * @param email user's email address, will be lowercased
     */
    public User(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email.toLowerCase();
    }

    /**
     * Constructs User with explicit ID. Used for testing or entity references without full load.
     *
     * @param id the user ID
     */
    public User(Long id) {
        this.id = id;
    }

    /**
     * Returns the user's primary key ID.
     *
     * @return the user ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the user's first name.
     *
     * @return the first name, may be null
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name.
     *
     * @param firstName the first name to set, may be null
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the user's last name.
     *
     * @return the last name, may be null
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name.
     *
     * @param lastName the last name to set, may be null
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the user's email address (primary identifier).
     *
     * @return the email address, lowercased
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address. Automatically lowercases for uniqueness constraint.
     *
     * @param email the email address to set, will be lowercased if non-null
     */
    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase() : null;
    }

    /**
     * Returns the local password authentication entity for this user.
     *
     * @return the LoginAndPassword entity, null if user doesn't use local password authentication
     */
    public LoginAndPassword getLoginAndPassword() {
        return loginAndPassword;
    }

    /**
     * Sets the local password authentication entity.
     *
     * @param loginAndPassword the LoginAndPassword entity to associate with this user
     */
    public void setLoginAndPassword(LoginAndPassword loginAndPassword) {
        this.loginAndPassword = loginAndPassword;
    }

    /**
     * Creates and sets a new local password authentication for this user.
     *
     * @param login the username/login identifier
     * @param plainPassword the plain text password (will be encrypted by LoginAndPassword constructor)
     * @param enabled whether the login credentials should be enabled
     */
    public void setLoginAndPassword(String login, String plainPassword, boolean enabled) {
        this.loginAndPassword = new LoginAndPassword(login, plainPassword, this, enabled);
    }

    /**
     * Returns whether the user account is enabled. Disabled users cannot authenticate.
     *
     * @return true if account is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the user account enabled status.
     *
     * @param enabled true to enable account, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns whether the user's authentication token has expired. Used for password reset/verification flows.
     *
     * @return true if token expired, false otherwise
     */
    public boolean isTokenExpired() {
        return tokenExpired;
    }

    /**
     * Sets the authentication token expiration status.
     *
     * @param tokenExpired true if token expired, false otherwise
     */
    public void setTokenExpired(boolean tokenExpired) {
        this.tokenExpired = tokenExpired;
    }

    /**
     * Returns the collection of UserRole associations linking this user to organizations with specific roles.
     *
     * @return the eager-loaded collection of UserRole assignments, empty if no role assignments
     */
    public Collection<UserRole> getRoles() {
        return roles;
    }

    /**
     * Sets the collection of UserRole associations for this user.
     *
     * @param roles the UserRole collection to set
     */
    public void setRoles(Collection<UserRole> roles) {
        this.roles = roles;
    }

    /**
     * Returns email for audit trail identification. Formats as "firstName lastName, email" if names present, 
     * otherwise just email.
     *
     * @return the formatted audit string
     */
    @Override
    public String toAuditString() {
        return StringUtils.isNotEmpty(firstName) && StringUtils.isNotEmpty(lastName) ?
                String.format("%s %s, %s", firstName, lastName, email) : String.format("%s", email);
    }

    /**
     * Full-text search index column, database-generated with default empty string. Non-insertable from application code.
     * Used for full-text search functionality.
     */
    @Column(name = INDEX_STRING_COLUMN, length = INDEX_STRING_COLUMN_LENGTH, insertable = false)
    @ColumnDefault("''")
    private String indexString;

    /**
     * Returns the database-generated full-text search index string.
     *
     * @return the index string used for search functionality
     */
    @Override
    public String getIndexString() {
        return indexString;
    }

    /**
     * Returns organization-scoped UserRole assignments visible to the specified user. Filters out global roles and 
     * applies visibility rules: all non-global roles visible if user has canReadBackend privilege, otherwise only 
     * roles in user's accessible organizations.
     *
     * @param u the optional OrganizationUser context for visibility filtering
     * @return list of visible UserRole assignments, empty if user not present
     */
    public List<UserRole> projectRolesVisibleTo(Optional<OrganizationUser> u) {
        if (!u.isPresent()) {
            return Collections.emptyList();
        }
        Collection<Long> organizationIds = u.get().getOrganizationIds();
        boolean returnAll = u.get().hasGlobalPrivilege(Privilege.canReadBackend);
        return roles.stream().filter( a -> !a.isGlobal() && (returnAll || organizationIds.contains(a.getOrganizationId()))).collect(Collectors.toList());
    }

    /**
     * Returns organization-scoped UserRole assignments for the specified organization. Filters out global roles.
     *
     * @param orgId the organization ID to filter by
     * @return list of UserRole assignments for the organization, empty if orgId is null
     */
    public List<UserRole> projectRolesVisibleForOrg(Long orgId) {
        if (orgId == null) {
            return Collections.emptyList();
        }
        return roles.stream().filter( a -> !a.isGlobal() && Objects.equals(orgId, a.getOrganizationId())).collect(Collectors.toList());
    }

    /**
     * Returns the user's display name. Formula-computed from first_name and last_name. Falls back to email if name is null.
     *
     * @return the display name or email if name is null
     */
    public String getName() {
        return name == null ? this.email : name;
    }

    /**
     * Returns the Facebook authentication provider entity for this user.
     *
     * @return the FacebookUser entity, null if user doesn't use Facebook authentication
     */
    public FacebookUser getFacebookUser() {
        return facebookUser;
    }

    /**
     * Returns the Google authentication provider entity for this user.
     *
     * @return the GoogleUser entity, null if user doesn't use Google authentication
     */
    public GoogleUser getGoogleUser() {
        return googleUser;
    }

    /**
     * Returns the user's profile picture URL.
     *
     * @return the picture URL from authentication provider or uploaded image, may be null
     */
    public String getPicture() {
        return picture;
    }

    /**
     * Sets the user's profile picture URL.
     *
     * @param picture the picture URL to set
     */
    public void setPicture(String picture) {
        this.picture = picture;
    }

    /**
     * Sets the Facebook authentication provider entity for this user.
     *
     * @param facebookUser the FacebookUser entity to associate
     */
    public void setFacebookUser(FacebookUser facebookUser) {
        this.facebookUser = facebookUser;
    }

    /**
     * Sets the Google authentication provider entity for this user.
     *
     * @param googleUser the GoogleUser entity to associate
     */
    public void setGoogleUser(GoogleUser googleUser) {
        this.googleUser = googleUser;
    }

    /**
     * Returns the LDAP authentication provider entity for this user.
     *
     * @return the LDAPUser entity, null if user doesn't use LDAP authentication
     */
    public LDAPUser getLdapUser() {
        return ldapUser;
    }

    /**
     * Sets the LDAP authentication provider entity for this user.
     *
     * @param ldapUser the LDAPUser entity to associate
     */
    public void setLdapUser(LDAPUser ldapUser) {
        this.ldapUser = ldapUser;
    }

    /**
     * Returns the Salesforce authentication provider entity for this user.
     *
     * @return the SalesforceUser entity, null if user doesn't use Salesforce authentication
     */
    public SalesforceUser getSalesforceUser() {
        return salesforceUser;
    }

    /**
     * Sets the Salesforce authentication provider entity for this user.
     *
     * @param salesforceUser the SalesforceUser entity to associate
     */
    public void setSalesforceUser(SalesforceUser salesforceUser) {
        this.salesforceUser = salesforceUser;
    }

    /**
     * Returns the LinkedIn authentication provider entity for this user.
     *
     * @return the LinkedinUser entity, null if user doesn't use LinkedIn authentication
     */
    public LinkedinUser getLinkedinUser() {
        return linkedinUser;
    }

    /**
     * Sets the LinkedIn authentication provider entity for this user.
     *
     * @param linkedinUser the LinkedinUser entity to associate
     */
    public void setLinkedinUser(LinkedinUser linkedinUser) {
        this.linkedinUser = linkedinUser;
    }

    /**
     * Returns comma-separated list of authentication methods available for this user. Checks all authentication 
     * provider associations and includes those that are non-null.
     *
     * @return comma-separated string of login methods (e.g., "Login / Password, Google, Facebook")
     */
    public String getLoginMethods() {
        List<String> loginMethods = new ArrayList<>();
        if(this.getLoginAndPassword() != null) loginMethods.add("Login / Password");
        if(this.getFacebookUser() != null) loginMethods.add("Facebook");
        if(this.getGoogleUser() != null) loginMethods.add("Google");
        if(this.getLdapUser() != null) loginMethods.add("LDAP");
        if(this.getSalesforceUser() != null) loginMethods.add("Salesforce");
        if(this.getLinkedinUser() != null) loginMethods.add("Linkedin");
        return String.join(", ", loginMethods);
    }

    /**
     * Returns the timestamp of user's last successful login.
     *
     * @return the last login timestamp, null if never logged in
     */
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    /**
     * Sets the timestamp of user's last successful login.
     *
     * @param loggedIn the login timestamp to set
     */
    public void setLastLogin(LocalDateTime loggedIn) {
        this.lastLogin = loggedIn;
    }

    /**
     * Sets the entity's last updated timestamp. Used for audit tracking.
     *
     * @param updatedOn the update timestamp to set
     */
    public void setUpdatedOn(LocalDateTime updatedOn) {
        this.updatedOn = updatedOn;
    }

    /**
     * Returns the privilege token required for reading user data. Formula-derived from PrivilegeNames constants.
     *
     * @return the required read privilege token ('_readUserData' or NULL for placeholder users)
     */
    @Override
    public String getRequiredReadPrivilege() {
        return requiredReadPrivilege;
    }

    /**
     * Returns the privilege token required for modifying user data. Formula-derived from PrivilegeNames constants.
     *
     * @return the required write privilege token ('_manageUserData')
     */
    @Override
    public String getRequiredWritePrivilege() {
        return requiredWritePrivilege;
    }

    /**
     * {@code @Formula}-derived array of organization IDs user has access to via UserRole associations. 
     * Aggregated from users_roles table. Used for multi-tenant filtering. Returns empty array if no organizations.
     */
    @Formula("(select array_agg(ur.organization_id) from users_roles ur where ur.organization_id is not null and ur.user_id = id)")
//    @Type(value = com.openkoda.core.customisation.LongArrayType.class)
    @JdbcTypeCode(SqlTypes.ARRAY)
    private Long[] organizationIds;

    /**
     * Returns array of organization IDs user has access to via UserRole associations. Used for multi-tenant filtering. 
     * Formula-aggregated from users_roles table.
     *
     * @return array of organization IDs, empty array if no organizations, null if not yet loaded
     */
    @Override
    public Long[] getOrganizationIds() {
        return organizationIds;
    }

    /**
     * Returns the API key entity for programmatic authentication.
     *
     * @return the ApiKey entity, null if no API key assigned
     */
    public ApiKey getApiKey() {
        return apiKey;
    }

    /**
     * Sets the API key entity for programmatic authentication.
     *
     * @param apiKey the ApiKey entity to associate with this user
     */
    public void setApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Returns the user's preferred language code for localization.
     *
     * @return the language code (e.g., "en", "pl"), may be null
     */
    public String getLanguage() { return language; }

    /**
     * Sets the user's preferred language code for localization.
     *
     * @param language the language code to set (e.g., "en", "pl")
     */
    public void setLanguage(String language) { this.language = language; }

    /**
     * Converts this User entity to BasicUser DTO for lightweight data transfer. Copies id, email, firstName, lastName.
     *
     * @return new BasicUser DTO instance with populated fields
     */
    public BasicUser getBasicUser() {
        BasicUser user = new BasicUser();
        user.setId(this.id);
        user.setEmail(this.email);
        user.setFirstName(this.firstName);
        user.setLastName(this.lastName);
        return user;
    }

    /**
     * Returns set of authentication methods available for this user based on associated authentication provider entities.
     * Used for authentication UI and flow control.
     *
     * @return set of AuthenticationMethods enums, empty if no authentication methods configured
     */
    public Set<LoggedUser.AuthenticationMethods> getAuthenticationMethods() {
        Set<LoggedUser.AuthenticationMethods> result = new HashSet<>();
        if (this.getLoginAndPassword() != null) { result.add(LoggedUser.AuthenticationMethods.PASSWORD); }
        if (this.getGoogleUser() != null) { result.add(LoggedUser.AuthenticationMethods.SOCIAL_GOOGLE); }
        if (this.getLinkedinUser() != null) { result.add(LoggedUser.AuthenticationMethods.SOCIAL_LINKEDIN); }
        if (this.getSalesforceUser() != null) { result.add(LoggedUser.AuthenticationMethods.SOCIAL_SALESFORCE); }
        if (this.getFacebookUser() != null) { result.add(LoggedUser.AuthenticationMethods.SOCIAL_FACEBOOK); }
        if (this.getLdapUser() != null) { result.add(LoggedUser.AuthenticationMethods.LDAP); }
        return result;
    }

    /**
     * Returns the name of the user's first global role if any. Used for authorization checks.
     *
     * @return the global role name, null if user has no global roles
     */
    public String getGlobalRoleName() {
        List<Role> globals = roles.stream()
                .map(a -> a.getRole())
                .filter(role -> role.getType().equals("GLOBAL"))
                .collect(Collectors.toList());
        return globals.isEmpty() ? null : globals.get(0).getName();
    }

    /**
     * Gets a user property value from the properties Map.
     *
     * @param name the property key
     * @return the property value, null if key not found
     */
    public String getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Sets a user property value in the properties Map.
     *
     * @param name the property key
     * @param value the property value to set
     * @return the previous value associated with the key, null if none
     */
    public String setProperty(String name, String value) {
        return properties.put(name, value);
    }

}
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

package com.openkoda.core.helper;

import com.google.gson.internal.LinkedTreeMap;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.core.security.UserProvider;
import com.openkoda.model.DynamicPrivilege;
import com.openkoda.model.Privilege;
import com.openkoda.model.PrivilegeBase;
import com.openkoda.model.PrivilegeGroup;
import com.openkoda.service.user.BasicPrivilegeService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.persistence.AttributeConverter;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.openkoda.core.helper.NameHelper.getClasses;
import static com.openkoda.model.Privilege.*;
import static java.util.stream.Collectors.toList;

/**
 * Manages privilege enumeration, serialization, and conversion for OpenKoda's Role-Based Access Control (RBAC) system.
 * <p>
 * This helper combines static enum privileges (defined in {@link Privilege}) with dynamic database privileges
 * (defined in {@link DynamicPrivilege}) to provide a unified privilege management interface. It implements
 * JPA's {@link AttributeConverter} interface for automatic privilege persistence and provides JSON serialization
 * and string format conversions for privilege sets.
 * <p>
 * The class uses an {@link AtomicReference}-backed singleton pattern initialized via {@link PostConstruct} and
 * accessible through {@link #getInstance()}. During initialization, it registers all privilege enum classes and
 * populates predefined role-specific privilege sets (admin, orgAdmin, user, orgUser).
 * <p>
 * Privileges are stored in the database using a parenthesized format: {@code "(PRIV1),(PRIV2),(PRIV3)"}. The helper
 * provides conversion methods between this format and Java collections ({@link Set}, arrays) as well as JSON arrays
 * for frontend consumption.
 * <p>
 * Example usage:
 * <pre>
 * // Convert comma-separated string to privilege set
 * Set&lt;PrivilegeBase&gt; privileges = PrivilegeHelper.fromJoinedStringToSet("ADMIN,USER");
 * 
 * // Convert privilege set to database format
 * String dbFormat = PrivilegeHelper.toJoinedStringInParenthesis(privileges);
 * 
 * // Get current user context
 * Long userId = privilegeHelper.getCurrentUserId();
 * </pre>
 * <p>
 * <b>Thread Safety:</b> This class uses an {@link AtomicReference} for singleton management and populates
 * static maps during initialization. After startup, most operations are read-only and safe for concurrent use.
 * The {@code nameToEnum} map and privilege set constants are populated during {@link #init()} and should not
 * be modified afterward.
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Privilege
 * @see DynamicPrivilege
 * @see PrivilegeBase
 * @see HasSecurityRules
 * @see AttributeConverter
 */
@Component("auth")
@Scope("singleton")
public class PrivilegeHelper implements HasSecurityRules, AttributeConverter<PrivilegeBase, String> {

    /**
     * Configuration flag controlling whether login and password authentication is enabled.
     * Injected from application property {@code authentication.loginAndPassword}, defaults to {@code true}.
     */
    @Value("${authentication.loginAndPassword:true}")
    public boolean loginAndPasswordAuthentication;
    
    /**
     * Configuration flag controlling whether the sign-up link is displayed on the login page.
     * Injected from application property {@code login.sign-up.link}, defaults to {@code false}.
     */
    @Value("${login.sign-up.link:false}")
    public boolean signUpLink;
    
    /**
     * Comma-separated list of fully qualified privilege enum class names to register.
     * Injected from application property {@code application.classes.privileges-enum}.
     * Classes are dynamically loaded and registered in the {@code nameToEnum} map during initialization.
     */
    @Value("${application.classes.privileges-enum:}")
    private String[] privilegesEnumClasses;

    /**
     * Cached list of all registered enum privileges (both static and dynamic).
     * Populated lazily by {@link #getAllEnumPrivileges()}.
     */
    private static List<PrivilegeBase> allEnumPrivileges;
    
    /**
     * Cached list of all non-hidden enum privileges, sorted by ID.
     * Populated lazily by {@link #getAllNonHiddenEnumPrivileges()}.
     */
    private static List<PrivilegeBase> allNonHiddenEnumPrivileges;
    
    /**
     * Map from privilege name to privilege instance.
     * Populated during initialization by {@link #registerEnumClasses(Class[])}.
     * Used for fast lookup by {@link #valueOfString(String)}.
     */
    private static final Map<String, PrivilegeBase> nameToEnum = new LinkedTreeMap<>();
    
    /**
     * Cached JSON array of enum privileges without group labels.
     * Populated lazily by {@link #allEnumsAsPrivilegeBaseJsonStringInstance(boolean)}.
     */
    private static JSONArray enumJsonArray;
    
    /**
     * Cached JSON array of enum privileges with concatenated group labels.
     * Populated lazily by {@link #allEnumsAsPrivilegeBaseJsonStringInstance(boolean)}.
     */
    private static JSONArray enumJsonArrayWithLabel;
    
    /**
     * Set of all privileges for global administrators.
     * Initialized with all {@link Privilege} enum values.
     */
    private static final Set<PrivilegeBase> adminPrivileges = new HashSet<>(Arrays.asList(Privilege.values()));
    
    /**
     * Set of privileges for regular users (non-organization-specific).
     * Populated during {@link #init()} with {@code isUser} privilege.
     */
    private static final Set<PrivilegeBase> userPrivileges = new HashSet<>();
    
    /**
     * Set of privileges for organization administrators.
     * Populated during {@link #init()} with all admin privileges except global-only privileges
     * (canAccessGlobalSettings, canImpersonate, canSeeUserEmail, canResetPassword, canChangeEntityOrganization).
     */
    private static final Set<PrivilegeBase> orgAdminPrivileges = new HashSet<>();
    
    /**
     * Set of privileges for organization users (non-admin members).
     * Populated during {@link #init()} with readUserData and readOrgData privileges.
     */
    private static final Set<PrivilegeBase> orgUserPrivileges = new HashSet<>();
    
    /**
     * Singleton instance holder using atomic reference for thread-safe lazy initialization.
     * Set during construction and accessible via {@link #getInstance()}.
     */
    private static AtomicReference<PrivilegeHelper> instance;
    
    /**
     * Service for accessing dynamic privileges stored in the database.
     * Used by {@link #valueOfString(String)} for privilege lookup when not found in static enum map.
     * All repository access goes through this service to leverage Spring's {@code @Cacheable} mechanism.
     */
    @Inject private BasicPrivilegeService privilegeService;

    /**
     * Private constructor that initializes the singleton instance reference.
     * Sets the static {@link #instance} field to make this instance accessible via {@link #getInstance()}.
     * Called by Spring during component initialization.
     */
    private PrivilegeHelper() {
        PrivilegeHelper.instance = new AtomicReference<>(this);
    }
    
    /**
     * Returns the singleton instance of PrivilegeHelper.
     * Creates a new instance if not yet initialized (defensive null check for non-Spring contexts).
     * 
     * @return the singleton PrivilegeHelper instance
     */
    public static final PrivilegeHelper getInstance() {
        if(instance == null) {
            instance =  new AtomicReference<>(new PrivilegeHelper());
        }
        
        return instance.get();
    }

    /**
     * Retrieves the current user's ID from the security context.
     * 
     * @return the current user's ID, or {@code null} if no user is authenticated
     * @see UserProvider#getFromContext()
     */
    public Long getCurrentUserId() {
        return UserProvider.getFromContext().map( a-> a.getUser() ).map( a -> a.getId() ).orElse(null );
    }

    /**
     * Retrieves the current user's default organization ID from the security context.
     * 
     * @return the current organization ID, or {@code null} if no user is authenticated or user has no default organization
     * @see UserProvider#getFromContext()
     */
    public Long getCurrentOrganizationId() {
        return UserProvider.getFromContext().map( a-> a.getDefaultOrganizationId() ).orElse(null );
    }

    /**
     * Converts a comma-separated string of privilege names to an array of privilege enums.
     * 
     * @param joinedPrivileges comma-separated privilege names (e.g., "ADMIN,USER,READ_DATA")
     * @return array of privilege enum instances corresponding to the names
     * @see #valueOfString(String)
     */
    public static Enum[] fromJoinedStringToArray(String joinedPrivileges) {
        return Arrays.stream(joinedPrivileges.split(",")).map(PrivilegeHelper::valueOfString).toArray(Enum[]::new);
    }

    /**
     * Converts a comma-separated string of privilege names to an immutable set of privileges.
     * 
     * @param joinedPrivileges comma-separated privilege names (e.g., "ADMIN,USER,READ_DATA")
     * @return immutable set of PrivilegeBase instances
     * @see #valueOfString(String)
     */
    public static Set<PrivilegeBase> fromJoinedStringToSet(String joinedPrivileges) {
        return Arrays.stream(joinedPrivileges.split(",")).map(PrivilegeHelper::valueOfString).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Converts a parenthesized string format to an immutable set of privilege name strings.
     * Input format: {@code "(PRIV1),(PRIV2),(PRIV3)"} - the database storage format.
     * 
     * @param joinedPrivileges privilege names in parenthesized format
     * @return immutable set of privilege name strings without parentheses
     * @see #splitAndRemoveParenthesis(String)
     */
    public static Set<String> fromJoinedStringToStringSet(String joinedPrivileges) {
        return streamSplitAndRemoveParenthesis(joinedPrivileges).collect(Collectors.toUnmodifiableSet());
    }

    //TODO: this method is handy not only for privileges, move to some more general helper
    public static Stream<String> streamSplitAndRemoveParenthesis(String joinedPrivileges) {
        return Arrays.stream(splitAndRemoveParenthesis(joinedPrivileges));
    }

    public static Set<String> fromPrivilegesToStringSet(PrivilegeBase... p) {
        return Arrays.stream(p).map(a -> a.name()).collect(Collectors.toUnmodifiableSet());
    }

    public static Set<PrivilegeBase> fromJoinedStringInParenthesisToPrivilegeEnumSet(String joinedValuesInParenthesis) {
        Stream<String> k = streamSplitAndRemoveParenthesis(joinedValuesInParenthesis);
        return k.map(PrivilegeHelper::valueOfString).collect(Collectors.toUnmodifiableSet());
    }


    public static Set<Long> fromJoinedStringInParenthesisToLongSet(String joinedValuesInParenthesis) {
        return streamSplitAndRemoveParenthesis(joinedValuesInParenthesis).map(Long::valueOf).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Converts a privilege name string to a PrivilegeBase instance.
     * Performs two-stage lookup: first checks the static {@code nameToEnum} map for enum privileges,
     * then queries the database via {@code privilegeService} for dynamic privileges if not found.
     * 
     * @param s the privilege name (e.g., "ADMIN", "READ_DATA"), can be null or blank
     * @return the corresponding PrivilegeBase instance, or {@code null} if not found or input is blank
     * @see #registerEnumClasses(Class[])
     * @see BasicPrivilegeService#findByName(String)
     */
    public static PrivilegeBase valueOfString(String s) {
        if(StringUtils.isBlank(s)) {
            return null;
        }
        
        PrivilegeBase privelege = nameToEnum.get(s);
        if(privelege == null && getInstance().privilegeService != null) {
            privelege = getInstance().privilegeService.findByName(s);
        }
        
        return privelege;
    }


    /**
     * Splits a parenthesized privilege string and removes parentheses from each element.
     * Parses the database storage format {@code "(PRIV1),(PRIV2),(PRIV3)"} into individual privilege names.
     * <p>
     * The method strips the outer parentheses (first and last characters) and splits on {@code "),("}.
     * Empty input returns an empty array.
     * 
     * 
     * @param joinedPrivileges privilege string in parenthesized format, can be null or empty
     * @return array of privilege name strings without parentheses
     */
    private static String[] splitAndRemoveParenthesis(String joinedPrivileges) {
        return StringUtils.isEmpty(joinedPrivileges) ? new String[0]
                : joinedPrivileges.substring(1, joinedPrivileges.length() - 1).split("\\),\\(");
    }

    /**
     * Wraps a privilege name in parentheses for database storage format.
     * 
     * @param e the privilege to wrap
     * @return the privilege name wrapped in parentheses, e.g., {@code "(ADMIN)"}
     */
    public static String inParenthesis(PrivilegeBase e) {
        return "(" + e.name() + ")";
    }


    /**
     * Converts a set of privileges to a comma-separated string in parenthesized format.
     * Output format: {@code "(PRIV1),(PRIV2),(PRIV3)"} - used for database persistence.
     * 
     * @param privilegesSet set of privileges to convert
     * @return joined string in parenthesized format for database storage
     * @see #inParenthesis(PrivilegeBase)
     */
    public static String toJoinedStringInParenthesis(Set<PrivilegeBase> privilegesSet) {
        return StringUtils.join(privilegesSet.stream().map(PrivilegeHelper::inParenthesis).collect(toList()), ',');
    }

    /**
     * Converts varargs privileges to a comma-separated string in parenthesized format.
     * Output format: {@code "(PRIV1),(PRIV2),(PRIV3)"} - used for database persistence.
     * 
     * @param privileges variable number of privileges to convert
     * @return joined string in parenthesized format for database storage
     * @see #inParenthesis(PrivilegeBase)
     */
    public static String toJoinedStringInParenthesis(PrivilegeBase ... privileges) {
        return StringUtils.join(Stream.of(privileges).map(PrivilegeHelper::inParenthesis).collect(toList()), ',');
    }

    /**
     * Returns a combined list of all enum privileges and dynamic privileges from the database.
     * If no dynamic privileges exist, returns only the enum privileges.
     * 
     * @return list containing both static enum privileges and dynamic database privileges
     * @see #getAllEnumPrivileges()
     * @see BasicPrivilegeService#findAll()
     */
    public static List<PrivilegeBase> allEnumsToList() {
        List<DynamicPrivilege> dymamicPrivileges = 
                getInstance().privilegeService != null 
                ? getInstance().privilegeService.findAll() 
                : Collections.emptyList();
        if(dymamicPrivileges.isEmpty()) {
            return allEnumPrivileges;
        }
        
        List<PrivilegeBase> privileges = new ArrayList<>(allEnumPrivileges);
        privileges.addAll(dymamicPrivileges);
        return privileges;
    }

    /**
     * Returns the internal name-to-privilege mapping used for privilege lookup.
     * 
     * @return map from privilege name to PrivilegeBase instance
     * @deprecated This method may be removed in future versions. Use {@link #valueOfString(String)} instead.
     */
    // TODO : can be removed?
    public static Map<String, PrivilegeBase> getNameToEnum() {
        return nameToEnum;
    }

    /**
     * Returns all non-hidden enum privileges sorted by ID.
     * Lazy-initializes and caches the list on first invocation.
     * Excludes privileges marked as hidden via {@link PrivilegeBase#isHidden()}.
     * 
     * @return immutable list of non-hidden privileges sorted by ID
     */
    public static List<PrivilegeBase> getAllNonHiddenEnumPrivileges() {
        if (allNonHiddenEnumPrivileges == null) {
            allNonHiddenEnumPrivileges = nameToEnum.values().stream().filter( p -> !p.isHidden()).sorted( (p1, p2) -> p1.getId().compareTo(p2.getId()) ).toList();
        }
        
        return allNonHiddenEnumPrivileges;
    }
    
    /**
     * @return map of all enums as privilege base list
     */
    public static List<PrivilegeBase> allEnumsAsPrivilegeBaseList() {
        List<DynamicPrivilege> dynamicPrivileges = getInstance().privilegeService != null 
                ? getInstance().privilegeService.findAll() 
                : Collections.emptyList();
        if(dynamicPrivileges.isEmpty()) {
            // just return already pre-populated list
            return getAllNonHiddenEnumPrivileges();
        }
        
        List<PrivilegeBase> result = new ArrayList<>(getAllNonHiddenEnumPrivileges());
        result.sort((p1, p2) -> p1.getId().compareTo(p2.getId()));
        result.addAll(0, dynamicPrivileges);
        return result;
    }

    public static PrivilegeBase[] allEnumsAsPrivilegeBase() {
        return allEnumsAsPrivilegeBaseList().toArray(PrivilegeBase[]::new);
    }

    // TODO : is that used at all? can be removed?
    /**
     * @return map of all enums as privilege base linked map
     */
    public static Map<PrivilegeBase, String> allEnumsAsPrivilegeBaseLinkedMap() {
        TreeMap<PrivilegeBase, String> result = new TreeMap<>(Comparator.comparing(PrivilegeBase::name));
        for (Map.Entry<String, PrivilegeBase> e: nameToEnum.entrySet()) {
            PrivilegeBase pb = e.getValue();
            if (pb.isHidden()) { continue; }
            result.put(pb, pb.getLabel());
        }
        
        if(getInstance().privilegeService != null) {
            getInstance().privilegeService.findAll().forEach( pb -> result.put(pb, pb.name()));
        }
        
        return result;
    }

    /**
     * Converts all enum and dynamic privileges to a JSON array string for frontend consumption.
     * Delegates to the instance method for caching support.
     * 
     * @param concatLabel if {@code true}, concatenates group label with privilege label; if {@code false}, includes separate category and group fields
     * @return JSON array string containing privilege metadata (key, value, category, group, hidden)
     * @throws JSONException if JSON serialization fails
     * @see #allEnumsAsPrivilegeBaseJsonStringInstance(boolean)
     */
    public static String allEnumsAsPrivilegeBaseJsonString(boolean concatLabel) throws JSONException {
        return getInstance().allEnumsAsPrivilegeBaseJsonStringInstance(concatLabel);
    }
    
    /**
     * Returns all registered enum privileges.
     * Lazy-initializes and caches the list on first invocation.
     * 
     * @return list of all enum privileges from the {@code nameToEnum} map
     */
    public static List<? extends PrivilegeBase> getAllEnumPrivileges() {
        if (allEnumPrivileges == null) {
            allEnumPrivileges = new ArrayList<>(nameToEnum.values());
        }
        
        return allEnumPrivileges;
    }
    
    public String allEnumsAsPrivilegeBaseJsonStringInstance(boolean concatLabel) throws JSONException {
        if(!concatLabel && enumJsonArray == null) {
            enumJsonArray = privilegeListToJson(new JSONArray(), getAllEnumPrivileges(), concatLabel);
        } else if(concatLabel && enumJsonArrayWithLabel == null) {
            enumJsonArrayWithLabel = privilegeListToJson(new JSONArray(), getAllEnumPrivileges(), concatLabel);
        }
        
        List<DynamicPrivilege> dynamicPrivileges = privilegeService.findAll();
        if (dynamicPrivileges.isEmpty()) {
            if(concatLabel) {
                return enumJsonArrayWithLabel.toString();
            }
            
            return enumJsonArray.toString();
        }

        JSONArray results = new JSONArray();
        privilegeListToJson(results, getAllEnumPrivileges(), concatLabel);
        privilegeListToJson(results, dynamicPrivileges, concatLabel);
        return results.toString();
    }
    
    private JSONArray privilegeListToJson(JSONArray results, List<? extends PrivilegeBase> privileges, boolean concatLabel) throws JSONException {
        JSONObject result;
        for (PrivilegeBase pb : privileges) {
            result = new JSONObject();
            result.put("k", pb.name());
            if(!concatLabel) {
                result.put("c", pb.getCategory());
                if(pb.getGroup() != null) {
                    result.put("g", pb.getGroup().getLabel());
                }
                result.put("v", pb.getLabel());
            } else {
                result.put("v", (pb.getGroup() != null ? pb.getGroup().getLabel() : pb.getCategory()) + ": " + pb.getLabel());
            }
            result.put("hidden", String.valueOf(pb.isHidden()));
            results.put(result);
        }
        
        return results;
    }

    /**
     * Registers privilege enum classes by adding all their enum values to the {@code nameToEnum} map.
     * Called during {@link #init()} to populate the privilege lookup map with all configured enum classes.
     * Enums are sorted by label before registration.
     * <p>
     * This method enables dynamic privilege enum registration via the
     * {@code application.classes.privileges-enum} configuration property.
     * 
     *
     * @param enumClasses array of privilege enum classes to register (must implement {@link PrivilegeBase})
     * @see #init()
     * @see PrivilegeBase
     */
    public static void registerEnumClasses(Class<Enum>[] enumClasses) {
        for (Class<Enum> ec : enumClasses) {
            List<Enum> enums = Arrays.stream(ec.getEnumConstants()).sorted(Comparator.comparing(o -> ((PrivilegeBase) o).getLabel())).collect(toList());
            for (Enum e : enums) {
                nameToEnum.put(e.name(), (PrivilegeBase)e);
            }
        }
    }

    /**
     * Returns the predefined privilege set for global administrators.
     * Contains all privileges defined in the {@link Privilege} enum.
     * 
     * @return set of all admin privileges
     * @see #init()
     */
    public static Set<PrivilegeBase> getAdminPrivilegeSet() {
        return adminPrivileges.stream().map( p -> (PrivilegeBase)p).collect(Collectors.toSet());
    }

    /**
     * Returns the predefined privilege set for organization administrators.
     * Contains all admin privileges except global-only privileges:
     * {@code canAccessGlobalSettings}, {@code canImpersonate}, {@code canSeeUserEmail},
     * {@code canResetPassword}, {@code canChangeEntityOrganization}.
     * 
     * @return set of organization admin privileges
     * @see #init()
     */
    public static Set<PrivilegeBase> getOrgAdminPrivilegeSet() {
        return orgAdminPrivileges.stream().map( p -> (PrivilegeBase)p).collect(Collectors.toSet());
    }

    /**
     * Returns the predefined privilege set for regular users (non-organization-specific).
     * Contains the {@code isUser} privilege.
     * 
     * @return set of user privileges
     * @see #init()
     */
    public static Set<PrivilegeBase> getUserPrivilegeSet() {
        return userPrivileges.stream().map( p -> (PrivilegeBase)p).collect(Collectors.toSet());
    }

    /**
     * Returns the predefined privilege set for organization users (non-admin members).
     * Contains {@code readUserData} and {@code readOrgData} privileges.
     * 
     * @return set of organization user privileges
     * @see #init()
     */
    public static Set<PrivilegeBase> getOrgUserPrivilegeSet() {
        return orgUserPrivileges.stream().map( p -> (PrivilegeBase)p).collect(Collectors.toSet());
    }

    public static Set<String> getAdminPrivilegeStrings() {
        return fromPrivilegesToStringSet(allEnumsAsPrivilegeBase());
    }

    public static Privilege[] getAdminPrivileges() {
        return Privilege.values();
    }
    
    public static List<PrivilegeGroup> allPrivilegeGroups() {
        return Arrays.asList(PrivilegeGroup.values());
    }
    
    public static List<String> allCategories() {
        return Stream.of(Privilege.values()).map(p -> p.getCategory()).sorted().toList();
    }

    /**
     * Initializes the PrivilegeHelper after dependency injection completes.
     * Performs the following initialization steps:
     * <ol>
     *   <li>Registers all configured privilege enum classes via {@link #registerEnumClasses(Class[])}</li>
     *   <li>Populates {@code orgAdminPrivileges} with all admin privileges except global-only privileges</li>
     *   <li>Populates {@code orgUserPrivileges} with read-only data access privileges</li>
     *   <li>Populates {@code userPrivileges} with the basic user privilege</li>
     * </ol>
     * <p>
     * Called automatically by Spring after bean construction and dependency injection.
     * 
     * 
     * @see #registerEnumClasses(Class[])
     * @see Privilege
     */
    @PostConstruct
    void init() {
        registerEnumClasses((Class<Enum>[]) getClasses(privilegesEnumClasses));
        orgAdminPrivileges.addAll(adminPrivileges);
        orgAdminPrivileges.removeAll(Arrays.asList(canAccessGlobalSettings,canImpersonate,canSeeUserEmail,canResetPassword,canChangeEntityOrganization));
        orgUserPrivileges.addAll(Arrays.asList(readUserData, readOrgData));
        userPrivileges.addAll(Arrays.asList(isUser));
    }

    /**
     * Converts a PrivilegeBase entity attribute to its database column representation.
     * Part of the JPA {@link AttributeConverter} implementation for automatic privilege persistence.
     * Converts the privilege to its name string for database storage.
     * 
     * @param attribute the privilege to convert, can be null
     * @return the privilege name string, or {@code null} if attribute is null
     * @see AttributeConverter#convertToDatabaseColumn(Object)
     */
    @Override
    public String convertToDatabaseColumn(PrivilegeBase attribute) {
        return attribute != null ? attribute.name() : null;
    }

    /**
     * Converts a database column value to a PrivilegeBase entity attribute.
     * Part of the JPA {@link AttributeConverter} implementation for automatic privilege retrieval.
     * Uses {@link #valueOfString(String)} to look up the privilege by name.
     * 
     * @param dbData the privilege name from the database
     * @return the corresponding PrivilegeBase instance, or {@code null} if not found
     * @see AttributeConverter#convertToEntityAttribute(Object)
     * @see #valueOfString(String)
     */
    @Override
    public PrivilegeBase convertToEntityAttribute(String dbData) {
        return PrivilegeHelper.valueOfString(dbData);
    }
}

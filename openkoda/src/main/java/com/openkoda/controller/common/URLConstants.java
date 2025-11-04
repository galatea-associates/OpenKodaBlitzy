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

package com.openkoda.controller.common;

/**
 * Application URL path constants and patterns for consistent request mapping across controllers.
 * <p>
 * Centralizes URL fragments, composed path templates, API version prefixes, Ant-style security patterns,
 * parameter templates with regex constraints, and validation regexes. Controllers reference these in
 * {@code @RequestMapping} annotations to prevent typos and enable easy refactoring. Security configuration
 * uses Ant expressions for permit/deny rules. All constants are compile-time literals enabling inline optimization.
 * </p>
 * <p>
 * This interface organizes constants into ten logical categories:
 * </p>
 * <ol>
 * <li><b>Regex Patterns:</b> Path variable constraints and validation patterns (NUMBERREGEX, URL_REGEX, etc.)</li>
 * <li><b>Base Path Fragments:</b> Root-level navigation paths with underscore prefix (_HOME, _ADMIN, _API, etc.)</li>
 * <li><b>Entity/Resource Names:</b> Entity type identifiers without slash prefix (ORGANIZATION, USER, ROLE, etc.)</li>
 * <li><b>Path Parameter Templates:</b> Path variable placeholders with regex constraints (_ORGANIZATIONID, _USERID, _ID, etc.)</li>
 * <li><b>Composed Paths:</b> Pre-composed frequently-used path patterns (_ORGANIZATION_ORGANIZATIONID, _API_V1, etc.)</li>
 * <li><b>Ant Expressions:</b> Ant-style wildcard patterns for Spring Security (_API_V1_ANT_EXPRESSION, etc.)</li>
 * <li><b>Action Suffixes:</b> Standard CRUD and action operation paths (_SAVE, _REMOVE, _UPLOAD, etc.)</li>
 * <li><b>View/Content Types:</b> Content-type or view format indicators (_HTML, _XML, _CSV, etc.)</li>
 * <li><b>Special Tokens:</b> Spring MVC prefixes and special request parameters (REDIRECT, FORWARD, API_TOKEN, etc.)</li>
 * <li><b>Validation Regexes:</b> Input validation and pattern matching (FRONTENDRESOURCE_AUTH_PARAMS_REGEX, IP_COMMA_SEPARATED_LIST, etc.)</li>
 * </ol>
 * <p>
 * <b>Usage Examples:</b>
 * </p>
 * <pre>
 * // Controller mapping
 * {@code @RequestMapping(URLConstants._ADMIN + URLConstants._LOGS)}
 * 
 * // Security configuration
 * http.antMatchers(URLConstants._API_AUTH_ANT_EXPRESSION).permitAll()
 * 
 * // Path building
 * String path = URLConstants._ORGANIZATION + "/" + orgId + URLConstants._SETTINGS
 * </pre>
 * <p>
 * <b>Naming Conventions:</b>
 * </p>
 * <ul>
 * <li>Underscore prefix (_HOME) indicates slash-prefixed fragment ("/home")</li>
 * <li>No underscore prefix (HOME) indicates raw identifier without slash</li>
 * <li>Path parameters follow pattern "/{paramName}" or "/{paramName:regex}"</li>
 * <li>Ant expressions end with "/**" suffix for wildcard matching</li>
 * </ul>
 * <p>
 * <b>Thread-Safety:</b> All constants are compile-time immutable strings, safe for concurrent access.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 */
public interface URLConstants {

    // ========================================
    // 1. REGEX PATTERNS
    // ========================================
    // Purpose: Path variable constraints and validation patterns
    // Usage: @RequestMapping path templates {id:[0-9]+}, input validation
    // Examples: NUMBERREGEX for numeric IDs, URL_REGEX for HTTP/HTTPS URL validation
    
    /**
     * Regular expression matching numeric strings (one or more digits).
     * Used in path variable constraints for numeric IDs.
     * <p>Example: {@code @RequestMapping("/{id:" + NUMBERREGEX + "}")}</p>
     */
    String NUMBERREGEX = "[0-9]+";
    
    // ========================================
    // 2. BASE PATH FRAGMENTS & SPECIAL TOKENS
    // ========================================
    // Purpose: Root-level navigation paths and Spring MVC prefixes
    // Convention: Underscore prefix indicates slash-prefixed fragment
    // Usage: @RequestMapping(_ADMIN + _LOGS) maps to "/admin/logs"
    
    /** Home page path ("/home"). */
    String _HOME = "/home";
    
    /** Send action identifier (no slash prefix). */
    String SEND = "send";
    //String CREATE_EVENT = "createEvent";
    
    /**
     * Spring MVC redirect prefix ("redirect:").
     * <p>Example: {@code return REDIRECT + "/" + URLConstants._HOME;}</p>
     */
    String REDIRECT = "redirect:";
    
    /**
     * Spring MVC forward prefix ("forward:").
     * <p>Example: {@code return FORWARD + "/internal/handler";}</p>
     */
    String FORWARD = "forward:";
    
    // ========================================
    // 3. VIEW/CONTENT TYPE CONSTANTS
    // ========================================
    // Purpose: Content-type or view format indicators
    // Usage: _HTML_ORGANIZATION for HTML views, _XML_EXTENSION for XML responses
    
    /** XML Accept header value for content negotiation. */
    String _XML_HEADER = "Accept=application/xml";
    
    /** XML file extension (".xml"). */
    String _XML_EXTENSION = ".xml";
    
    /** XML format identifier (no prefix). */
    String XML = "xml";
    
    /** HTML view path prefix ("/html"). */
    String _HTML = "/html";
    
    /** All entities path segment ("/all"). */
    String _ALL = "/all";
    
    // ========================================
    // 4. ACTION SUFFIXES
    // ========================================
    // Purpose: Standard CRUD and action operation paths
    // Convention: Slash-prefixed, append to entity paths
    // Usage: _ORGANIZATION + _ID + _SAVE maps to "/organization/{id}/save"
    
    /** Save action path ("/save"). */
    String _SAVE = "/save";
    
    /** Exit action path ("/exit"). */
    String _EXIT = "/exit";
    
    /** ZIP export action path ("/zip"). */
    String _ZIP = "/zip";
    
    /** YAML format action path ("/yaml"). */
    String _YAML = "/yaml";
    
    /** Component entity identifier (no prefix). */
    String COMPONENT = "component";
    
    /** Component path with slash prefix ("/component"). */
    String _COMPONENT = "/" + COMPONENT;
    
    /** Export action identifier (no prefix). */
    String EXPORT = "export";
    
    /** Export action path with slash prefix ("/export"). */
    String _EXPORT = "/" + EXPORT;
    
    /** Composed path for YAML export ("/export/yaml"). */
    String _EXPORT_YAML = _EXPORT + _YAML;
    
    /** Composed identifier for YAML export (no prefix). */
    String EXPORT_YAML = EXPORT + _YAML;

    /** Import action identifier (no prefix). */
    String IMPORT = "import";
    
    /** Import action path with slash prefix ("/import"). */
    String _IMPORT = "/" + IMPORT;

    /** Publish action path ("/publish"). */
    String _PUBLISH = "/publish";
    
    /** Clear action path ("/clear"). */
    String _CLEAR = "/clear";
    
    /** Reload action path ("/reload"). */
    String _RELOAD = "/reload";
    
    /** Reload to draft action path ("/reload-to-draft"). */
    String _RELOAD_TO_DRAFT = "/reload-to-draft";
    
    /** Public access path prefix ("/public"). */
    String _PUBLIC = "/public";
    
    /** Register action identifier (no prefix). */
    String REGISTER = "register";
    
    /** Register action path with slash prefix ("/register"). */
    String _REGISTER = "/" + REGISTER;
    
    /** Attempt action path ("/attempt"). */
    String _ATTEMPT = "/attempt";
    
    /** Sitemap index path ("/sitemap_index"). */
    String _SITEMAP_INDEX = "/sitemap_index";
    
    /** Sitemap path ("/sitemap"). */
    String _SITEMAP = "/sitemap";
    
    /** Pages sitemap path ("/pages-sitemap"). */
    String _PAGES_SITEMAP = "/pages-sitemap";
    
    /** General sitemap path ("/general-sitemap"). */
    String _GENERAL_SITEMAP = "/general-sitemap";
    
    /** Password recovery path ("/recovery"). */
    String _RECOVERY = "/recovery";
    
    /** Change action path ("/change"). */
    String _CHANGE = "/change";
    
    /** Verification action path ("/verify"). */
    String _VERIFY = "/verify";
    
    /** Login path ("/login"). */
    String _LOGIN = "/login";
    
    /** Form entity identifier (no prefix). */
    String FORM = "form";
    
    /** Form path with slash prefix ("/form"). */
    String _FORM = "/" + FORM;
    
    /** OpenKoda module entity identifier (no prefix). */
    String OPENKODA_MODULE = "openkodaModule";
    
    /** OpenKoda module path with slash prefix ("/openkodaModule"). */
    String _OPENKODA_MODULE = "/" + OPENKODA_MODULE;
    
    /** Rule entity path ("/rule"). */
    String _RULE = "/rule";
    
    /** Rule line entity path ("/rule-line"). */
    String _RULE_LINE = "/rule-line";
    
    /** Logout path ("/logout"). */
    String _LOGOUT = "/logout";
    
    /**
     * Ant-style wildcard pattern matching any nested path ("/**").
     * <p>Used in Spring Security configuration for catch-all patterns.</p>
     */
    String _ANY = "/**";
    
    // ========================================
    // 5. ENTITY/RESOURCE NAMES
    // ========================================
    // Purpose: Entity type identifiers without slash prefix
    // Usage: Composed into full paths with underscore-prefixed versions
    // Example: _ORGANIZATION = "/" + ORGANIZATION
    
    /** Organization entity identifier (no prefix). */
    String ORGANIZATION = "organization";
    
    /** Email configuration entity identifier (no prefix). */
    String EMAIL_CONFIG = "emailConfig";
    
    /** Settings entity identifier (no prefix). */
    String SETTINGS = "settings";
    
    /** Test entity identifier (no prefix). */
    String TEST = "test";
    
    /** Invite entity identifier (no prefix). */
    String INVITE = "invite";
    
    /** Forms entity identifier (no prefix). */
    String FORMS = "forms";
    
    /** Dashboard entity identifier (no prefix). */
    String DASHBOARD = "dashboard";
    
    /** Registered CRUDs entity identifier (no prefix). */
    String REGISTERED_CRUDS = "registered-cruds";
    
    /** Registered CRUDs path with slash prefix ("/registered-cruds"). */
    String _REGISTERED_CRUDS = "/" + REGISTERED_CRUDS;
    
    /** Profile entity identifier (no prefix). */
    String PROFILE = "profile";
    
    /** Member entity identifier (no prefix). */
    String MEMBER = "member";
    
    /** File entity identifier (no prefix). */
    String FILE = "file";
    
    /** History entity identifier (no prefix). */
    String HISTORY = "history";
    
    /** Events entity identifier (no prefix). */
    String EVENTS = "events";
    
    /** Admin area identifier (no prefix). */
    String ADMIN = "admin";
    
    /** Logs entity identifier (no prefix). */
    String LOGS = "logs";
    
    /** User entity identifier (no prefix). */
    String USER = "user";
    
    /** Token entity identifier (no prefix). */
    String TOKEN = "token";
    
    /** Verify token action identifier (no prefix). */
    String VERIFY_TOKEN = "verifyToken";
    
    /** API key identifier (no prefix). */
    String KEY = "key";
    
    /** Password entity identifier (no prefix). */
    String PASSWORD = "password";
    
    /** Frontend resource entity identifier (no prefix). */
    String FRONTENDRESOURCE = "frontendresource";
    
    /** Frontend resource path with slash prefix ("/frontendresource"). */
    String _FRONTENDRESOURCE = "/" + FRONTENDRESOURCE;

    /** UI component entity identifier (no prefix). */
    String UI_COMPONENT = "uiComponent";
    
    /** UI component path with slash prefix ("/uiComponent"). */
    String _UI_COMPONENT = "/" + UI_COMPONENT;
    
    /** Web endpoint entity identifier (no prefix). */
    String WEBENDPOINT = "webEndpoint";
    
    /** Page builder entity identifier (no prefix). */
    String PAGEBUILDER = "pageBuilder";
    
    /** Controller endpoint entity identifier (no prefix). */
    String CONTROLLER_ENDPOINT = "controllerEndpoint";
    
    /** Server-side JavaScript entity identifier (no prefix). */
    String SERVERJS = "serverjs";
    
    /** Role entity identifier (no prefix). */
    String ROLE = "role";
    
    /** Privilege entity identifier (no prefix). */
    String PRIVILEGE = "privilege";
    
    /** Privileges collection identifier (no prefix). */
    String PRIVILEGES = "privileges";
    
    /** Module entity identifier (no prefix). */
    String MODULE = "module";
    
    /** Content entity identifier (no prefix). */
    String CONTENT = "content";
    
    /** Type entity identifier (no prefix). */
    String TYPE = "type";
    
    /** Event listener entity identifier (no prefix). */
    String EVENTLISTENER = "eventListener";
    
    /** Custom event entity identifier (no prefix). */
    String CUSTOM_EVENT = "customEvent";
    
    /** Scheduler entity identifier (no prefix). */
    String SCHEDULER = "scheduler";
    
    /** Spoof action identifier (no prefix). */
    String SPOOF = "spoof";
    
    /** Entity generic identifier (no prefix). */
    String ENTITY = "entity";
    
    /** Entity path with slash prefix ("/entity"). */
    String _ENTITY = "/" + ENTITY;
    
    /** New action identifier (no prefix). */
    String NEW = "new";
    
    /** New action path with slash prefix ("/new"). */
    String _NEW = "/" + NEW;
    
    /** All entities identifier (no prefix). */
    String ALL = "all";
    
    /** Audit entity identifier (no prefix). */
    String AUDIT = "audit";
    
    /** Preview action identifier (no prefix). */
    String PREVIEW = "preview";
    
    /** Settings path with slash prefix ("/settings"). */
    String _SETTINGS = "/" + SETTINGS;
    
    /** Audit path with slash prefix ("/audit"). */
    String _AUDIT = "/" + AUDIT;
    
    /** Module path with slash prefix ("/module"). */
    String _MODULE = "/" + MODULE;
    
    // ========================================
    // 6. PATH PARAMETER TEMPLATES
    // ========================================
    // Purpose: Path variable placeholders with regex constraints
    // Format: "/{paramName}" or "/{paramName:regex}"
    // Usage: _ORGANIZATION + _ORGANIZATIONID maps to "/organization/{organizationId}"
    
    /** Module name parameter identifier (no prefix). */
    String MODULENAME = "moduleName";
    
    /**
     * Module name path parameter template ("/{moduleName}").
     * <p>Used in dynamic module routing.</p>
     */
    String _MODULENAME = "/{" + MODULENAME + "}";
    
    /** Organization ID parameter identifier (no prefix). */
    String ORGANIZATIONID = "organizationId";
    
    /**
     * Organization ID path parameter template ("/{organizationId}").
     * <p>Example: {@code @RequestMapping(_ORGANIZATION + _ORGANIZATIONID)}</p>
     */
    String _ORGANIZATIONID = "/{" + ORGANIZATIONID + "}";
    
    /** Generic ID parameter identifier (no prefix). */
    String ID = "id";
    
    /**
     * Numeric ID path parameter template with regex constraint ("/{id:[0-9]+}").
     * <p>Restricts ID to numeric values only.</p>
     */
    String _ID = "/{" + ID + ":" + NUMBERREGEX + "}";
    
    /** Object ID parameter identifier (no prefix). */
    String OBJID = "objid";
    
    /**
     * Object ID path parameter template ("/{objid}").
     * <p>Generic object identifier for flexible routing.</p>
     */
    String _OBJID = "/{" + OBJID + "}";
    
    /** User ID parameter identifier (no prefix). */
    String USERID = "userId";
    
    /**
     * User ID path parameter template ("/{userId}").
     * <p>Example: {@code @RequestMapping(_USER + _USERID)}</p>
     */
    String _USERID = "/{" + USERID + "}";
    
    /** Profile path with slash prefix ("/profile"). */
    String _PROFILE = "/" + PROFILE;
    
    /** Member path with slash prefix ("/member"). */
    String _MEMBER = "/" + MEMBER;
    
    /** File path with slash prefix ("/file"). */
    String _FILE = "/" + FILE;
    
    /** Web endpoint path with slash prefix ("/webEndpoint"). */
    String _WEBENDPOINT = "/" + WEBENDPOINT;
    
    /** Page builder path with slash prefix ("/pageBuilder"). */
    String _PAGEBUILDER = "/" + PAGEBUILDER;
    
    /** Server-side JavaScript path with slash prefix ("/serverjs"). */
    String _SERVERJS = "/" + SERVERJS;
    
    /** Organization path with slash prefix ("/organization"). */
    String _ORGANIZATION = "/" + ORGANIZATION;
    
    /** Email configuration path with slash prefix ("/emailConfig"). */
    String _EMAIL_CONFIG = "/" + EMAIL_CONFIG;
    
    /** Dashboard path with slash prefix ("/dashboard"). */
    String _DASHBOARD = "/" + DASHBOARD;
    
    /** History path with slash prefix ("/history"). */
    String _HISTORY = "/" + HISTORY;
    
    /** Events path with slash prefix ("/events"). */
    String _EVENTS = "/" + EVENTS;
    
    /** Test path with slash prefix ("/test"). */
    String _TEST = "/" + TEST;
    
    /** Invite path with slash prefix ("/invite"). */
    String _INVITE = "/" + INVITE;
    
    /** Remove action path ("/remove"). */
    String _REMOVE = "/remove";
    
    /** Interrupt action path ("/interrupt"). */
    String _INTERRUPT = "/interrupt";
    
    /** Admin area path with slash prefix ("/admin"). */
    String _ADMIN = "/" + ADMIN;
    
    /** Logs path with slash prefix ("/logs"). */
    String _LOGS = "/" + LOGS;
    
    /** User path with slash prefix ("/user"). */
    String _USER = "/" + USER;
    
    /** Role path with slash prefix ("/role"). */
    String _ROLE = "/" + ROLE;
    
    /** Privilege path with slash prefix ("/privilege"). */
    String _PRIVILEGE = "/" + PRIVILEGE;
    
    /** Content path with slash prefix ("/content"). */
    String _CONTENT = "/" + CONTENT;
    
    /** Upload action path ("/upload"). */
    String _UPLOAD = "/upload";
    
    /** Event listener path with slash prefix ("/eventListener"). */
    String _EVENTLISTENER = "/" + EVENTLISTENER;
    
    /** Custom event path with slash prefix ("/customEvent"). */
    String _CUSTOM_EVENT = "/" + CUSTOM_EVENT;
    //String _CREATE_EVENT = "/" + CREATE_EVENT;
    
    /** Scheduler path with slash prefix ("/scheduler"). */
    String _SCHEDULER = "/" + SCHEDULER;
    
    /** Spoof action path with slash prefix ("/spoof"). */
    String _SPOOF = "/" + SPOOF;
    
    // ========================================
    // 7. COMPOSED PATHS
    // ========================================
    // Purpose: Pre-composed frequently-used path patterns
    // Benefits: DRY principle, consistent routing across controllers
    // Usage: @RequestMapping(_API_V1_ORGANIZATION) maps to "/api/v1/organization"
    
    /**
     * New settings path ("/new/settings").
     * <p>Route for creating new settings entities.</p>
     */
    String _NEW_SETTINGS = _NEW + _SETTINGS;
    
    /**
     * ID-based settings path ("/{id:[0-9]+}/settings").
     * <p>Route for editing specific settings by numeric ID.</p>
     */
    String _ID_SETTINGS = _ID + _SETTINGS;
    
    /**
     * ID-based remove path ("/{id:[0-9]+}/remove").
     * <p>Route for deleting entities by numeric ID.</p>
     */
    String _ID_REMOVE = _ID + _REMOVE;
    
    /** HTML view path for organizations ("/html/organization"). */
    String _HTML_ORGANIZATION = _HTML + _ORGANIZATION;
    
    /** HTML view path for users ("/html/user"). */
    String _HTML_USER = _HTML + _USER;
    
    /** HTML view path for roles ("/html/role"). */
    String _HTML_ROLE = _HTML + _ROLE ;
    
    /** HTML view path for privileges ("/html/privilege"). */
    String _HTML_PRIVILEGE = _HTML + _PRIVILEGE ;
    
    /**
     * User settings path with user ID parameter ("/user/{userId}/settings").
     * <p>Route for user-specific settings management.</p>
     */
    String _USER_SETTINGS = "/" + USER + _USERID + _SETTINGS;
    
    /** Preview action path with slash prefix ("/preview"). */
    String _PREVIEW = "/" + PREVIEW;
    
    /** View action identifier (no prefix). */
    String VIEW = "view";
    
    /** View action path with slash prefix ("/view"). */
    String _VIEW = "/" + VIEW;

    /**
     * Module settings path with module name parameter ("/module/{moduleName}/settings").
     * <p>Route for module-specific configuration.</p>
     */
    String _MODULE_MODULENAME_SETTINGS = _MODULE + _MODULENAME + _SETTINGS;
    
    /**
     * Module user settings path ("/module/{moduleName}/user/{userId}/settings").
     * <p>Route for user-specific module settings.</p>
     */
    String _MODULE_MODULENAME_USER_SETTINGS = _MODULE + _MODULENAME + _USER_SETTINGS;

    /**
     * Organization by ID path ("/organization/{organizationId}").
     * <p>Base route for organization-specific operations.</p>
     */
    String _ORGANIZATION_ORGANIZATIONID = _ORGANIZATION + _ORGANIZATIONID;

    /**
     * Organization module path ("/organization/{organizationId}/module").
     * <p>Base route for modules within an organization.</p>
     */
    String _ORGANIZATION_ORGANIZATIONID_MODULE = _ORGANIZATION_ORGANIZATIONID + _MODULE;
    
    /**
     * Organization module by name path ("/organization/{organizationId}/module/{moduleName}").
     * <p>Route for specific module within an organization.</p>
     */
    String _ORGANIZATION_ORGANIZATIONID_MODULE_MODULENAME = _ORGANIZATION_ORGANIZATIONID_MODULE + _MODULENAME;
    
    /**
     * Organization module settings path ("/organization/{organizationId}/module/{moduleName}/settings").
     * <p>Route for organization-scoped module configuration.</p>
     */
    String _ORGANIZATION_ORGANIZATIONID_MODULE_MODULENAME_SETTINGS = _ORGANIZATION_ORGANIZATIONID_MODULE_MODULENAME + _SETTINGS;
    
    /**
     * Organization module user settings path ("/organization/{organizationId}/module/{moduleName}/user/{userId}/settings").
     * <p>Route for user-specific settings within organization module context.</p>
     */
    String _ORGANIZATION_ORGANIZATIONID_MODULE_MODULENAME_USER_SETTINGS = _ORGANIZATION_ORGANIZATIONID_MODULE_MODULENAME + _USER_SETTINGS;

    /**
     * HTML organization by ID path ("/html/organization/{organizationId}").
     * <p>HTML view route for specific organization.</p>
     */
    String _HTML_ORGANIZATION_ORGANIZATIONID = _HTML_ORGANIZATION + _ORGANIZATIONID;
    
    /** Password path with slash prefix ("/password"). */
    String _PASSWORD = "/" + PASSWORD;
    
    /** Privileges collection path with slash prefix ("/privileges"). */
    String _PRIVILEGES = "/" + PRIVILEGES;
    
    /** Download action path ("/download"). */
    String _DOWNLOAD = "/download";
    
    /** Search action path ("/search"). */
    String _SEARCH = "/search";
    
    /** Selected items path ("/selected"). */
    String _SELECTED = "/selected";
    
    /** Attribute entity identifier (no prefix). */
    String ATTRIBUTE = "attribute";
    
    /** Attribute path with slash prefix ("/attribute"). */
    String _ATTRIBUTE = "/" + ATTRIBUTE;
    
    /** Notification entity identifier (no prefix). */
    String NOTIFICATION = "notification";
    
    /** Notification path with slash prefix ("/notification"). */
    String _NOTIFICATION = "/" + NOTIFICATION;
    
    /** Connect action path ("/connect"). */
    String _CONNECT = "/connect";
    
    /** Mark as read action path ("/mark-read"). */
    String _MARK_READ = "/mark-read";
    
    /** System health endpoint identifier (no prefix). */
    String SYSTEM_HEATH = "system-health";
    
    /** System health endpoint path ("/system-health"). */
    String _SYSTEM_HEATH = "/" + SYSTEM_HEATH;
    
    /** Validate action identifier (no prefix). */
    String VALIDATE = "validate";
    
    /** Validate action path with slash prefix ("/validate"). */
    String _VALIDATE = "/" + VALIDATE;
    
    /** Thread entity identifier (no prefix). */
    String THREAD = "thread";
    
    /** Thread path with slash prefix ("/thread"). */
    String _THREAD = "/" + THREAD;
    
    /**
     * Thread interrupt by ID path ("/thread/{id:[0-9]+}/interrupt").
     * <p>Route for interrupting specific thread execution.</p>
     */
    String _THREAD_ID_INTERRUPT = _THREAD + _ID + _INTERRUPT;
    
    /**
     * Thread remove by ID path ("/thread/{id:[0-9]+}/remove").
     * <p>Route for removing specific thread.</p>
     */
    String _THREAD_ID_REMOVE = _THREAD + _ID_REMOVE;
    
    /** Local scope path prefix ("/local"). */
    String _LOCAL = "/local";
    
    /** Resend action path ("/resend"). */
    String _RESEND = "/resend";
    
    /** Send action path with slash prefix ("/send"). */
    String _SEND = "/" + SEND;
    
    /** Emit action path ("/emit"). */
    String _EMIT = "/emit";
    
    /** Verification path ("/verification"). */
    String _VERIFICATION = "/verification";
    
    /** Components collection path ("/components"). */
    String _COMPONENTS = "/components";
    
    /** Integrations entity identifier (no prefix). */
    String INTEGRATIONS = "integrations";
    
    /** Integrations path with slash prefix ("/integrations"). */
    String _INTEGRATIONS = "/" + INTEGRATIONS;

    // ========================================
    // 8. VALIDATION REGEXES
    // ========================================
    // Purpose: Input validation and pattern matching
    // Usage: Form validation, configuration validation, path constraints
    
    /**
     * Regular expression for frontend resource paths.
     * <p>Matches alphanumeric paths with dashes, slashes, optional @ symbol, and file extensions
     * (.css, .js, .xml, .txt, .csv, .json, .html).</p>
     * <p>Pattern: {@code [0-9a-zA-Z\-\/]*\@?(?:\.css|\.js|\.xml|\.txt|\.csv|\.json|\.html)?}</p>
     */
    String FRONTENDRESOURCEREGEX = "[0-9a-zA-Z\\-\\/]*\\@?(?:\\.css|\\.js|\\.xml|\\.txt|\\.csv|\\.json|\\.html)?";
    
    /**
     * Regular expression for organization ID query parameter.
     * <p>Matches optional query string: {@code (?:\?organizationId=?.*)?}</p>
     */
    String FRONTENDRESOURCE_ORGID_PARAM_REGEX = "(?:\\?organizationId=?.*)?";
    
    /**
     * Email resource discriminator character ("@").
     * <p>Used to distinguish email-scoped frontend resources.</p>
     */
    String EMAILRESOURCE_DISCRIMINATOR = "@";

    /**
     * Regular expression for frontend resource authentication parameters.
     * <p>Matches optional query parameters with key-value pairs.</p>
     * <p>Pattern: {@code (\?(.*=?.*)?(\\&.*=?.*)?)?}</p>
     */
    String FRONTENDRESOURCE_AUTH_PARAMS_REGEX = "(\\?(.*=?.*)?(\\&.*=?.*)?)?";
    
    /**
     * Default language prefix ("pl").
     * <p>Used for internationalization routing.</p>
     */
    String LANGUAGEPREFIX = "pl";
    
    /**
     * Regular expression excluding Swagger UI paths.
     * <p>Negative lookahead pattern: {@code ^(?!.*swagger-ui)}</p>
     */
    String EXCLUDE_SWAGGER_UI_REGEX = "^(?!.*swagger-ui)";
    
    /**
     * Regular expression for URL-safe strings with dashes.
     * <p>Matches alphanumeric characters and dashes: {@code [0-9a-zA-Z\-]+}</p>
     */
    String URL_WITH_DASH_REGEX = "[0-9a-zA-Z\\-]+";
    
    /**
     * Regular expression for HTTP/HTTPS URLs.
     * <p>Case-insensitive match for http:// or https:// followed by any characters.</p>
     * <p>Pattern: {@code [hH][Tt][Tt][Pp][Ss]?://.*}</p>
     */
    String URL_REGEX = "[hH][Tt][Tt][Pp][Ss]?://.*";
    
    /**
     * Regular expression for comma-separated IP address list with optional CIDR notation.
     * <p>Validates IPv4 addresses (0.0.0.0 to 255.255.255.255) with optional /4-32 subnet mask.</p>
     * <p>Accepts comma-separated list of IPs.</p>
     */
    String IP_COMMA_SEPARATED_LIST = "(((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/([4-9]|[12][0-9]|3[0-2]))?)([,]|$))*";
    
    /**
     * Regular expression for lowercase alphanumeric identifiers with underscores.
     * <p>Pattern: {@code [a-z0-9_]+$}</p>
     * <p>Used for database table names and entity identifiers.</p>
     */
    String LOWERCASE_NUMERIC_UNDERSCORE_REGEXP = "[a-z0-9_]+$";

    /** Debug model parameter name ("debugModel"). */
    String DEBUG_MODEL = "debugModel";
    
    /** Draft entity identifier (no prefix). */
    String DRAFT = "draft";
    
    /** Draft path with slash prefix ("/draft"). */
    String _DRAFT = "/" + DRAFT;
    
    /** Copy action identifier (no prefix). */
    String COPY = "copy";
    
    /** Copy action path with slash prefix ("/copy"). */
    String _COPY = "/" + COPY;
    
    /** Resource entity identifier (no prefix). */
    String RESOURCE = "resource";
    
    /** Resource path with slash prefix ("/resource"). */
    String _RESOURCE = "/" + RESOURCE;
    
    /** Live state identifier (no prefix). */
    String LIVE = "live";
    
    /** Live path with slash prefix ("/live"). */
    String _LIVE = "/" + LIVE;

    /** API key path ("/apikey"). */
    String _APIKEY = "/apikey";
    
    /** Token path with slash prefix ("/token"). */
    String _TOKEN = "/token";
    
    /**
     * Token prefix path for special token-based routing ("/_t_").
     * <p>Used for public token-authenticated endpoints.</p>
     */
    String __T_ = "/_t_";
    
    /** Token refresher endpoint path ("/tokenrefresher"). */
    String _TOKENREFRESHER = "/tokenrefresher";
    
    /** Refresh action path ("/refresh"). */
    String _REFRESH = "/refresh";
    
    /** Reset action path ("/reset"). */
    String _RESET = "/reset";
    
    /** API base path ("/api"). */
    String _API = "/api";
    
    /** API version 1 segment ("/v1"). */
    String _V1 = "/v1";
    
    /** API version 2 segment ("/v2"). */
    String _V2 = "/v2";
    
    /** Authentication path segment ("/auth"). */
    String _AUTH = "/auth";

    // ========================================
    // 9. API VERSION PATHS & ANT EXPRESSIONS
    // ========================================
    // Purpose: Versioned API routes and Ant-style security patterns
    // Usage: @RequestMapping(_API_V1_ORGANIZATION), antMatchers(_API_V1_ANT_EXPRESSION)
    
    /**
     * API authentication base path ("/api/auth").
     * <p>Base route for authentication endpoints.</p>
     */
    String _API_AUTH = _API + _AUTH;
    
    /**
     * API authentication Ant expression ("/api/auth/**").
     * <p>Wildcard pattern for all authentication endpoints.</p>
     * <p>Example: {@code http.antMatchers(_API_AUTH_ANT_EXPRESSION).permitAll()}</p>
     */
    String _API_AUTH_ANT_EXPRESSION = _API_AUTH + "/**";
    
    /**
     * Token prefix Ant expression ("/_t_&#42;&#42;/&#42;&#42;").
     * <p>Wildcard pattern for all token-authenticated public endpoints.</p>
     */
    String _TOKEN_PREFIX_ANT_EXPRESSION = __T_ + "**/**";

    /**
     * API version 1 base path ("/api/v1").
     * <p>Root path for all v1 API endpoints.</p>
     */
    String _API_V1 = _API + _V1;
    
    /**
     * API v1 Ant expression ("/api/v1/**").
     * <p>Wildcard pattern for all v1 API endpoints.</p>
     */
    String _API_V1_ANT_EXPRESSION = _API_V1 + "/**";
    
    /**
     * API v1 organization path ("/api/v1/organization").
     * <p>Base route for organization API endpoints in version 1.</p>
     */
    String _API_V1_ORGANIZATION = _API_V1 + _ORGANIZATION;

    /**
     * API version 2 base path ("/api/v2").
     * <p>Root path for all v2 API endpoints.</p>
     */
    String _API_V2 = _API + _V2;
    
    /**
     * API v2 Ant expression ("/api/v2/**").
     * <p>Wildcard pattern for all v2 API endpoints.</p>
     */
    String _API_V2_ANT_EXPRESSION = _API_V2 + "/**";
    
    /**
     * API v2 organization path ("/api/v2/organization").
     * <p>Base route for organization API endpoints in version 2.</p>
     */
    String _API_V2_ORGANIZATION = _API_V2 + _ORGANIZATION;
    
    /**
     * API v2 organization by ID path ("/api/v2/organization/{organizationId}").
     * <p>Route for organization-specific operations in version 2.</p>
     */
    String _API_V2_ORGANIZATION_ORGANIZATIONID = _API_V2_ORGANIZATION + _ORGANIZATIONID;

    // ========================================
    // 10. SPECIAL REQUEST PARAMETERS & TOKENS
    // ========================================
    // Purpose: Request parameter names and special identifiers
    // Usage: Request parameter extraction, header values, session attributes
    
    /**
     * API token parameter name ("api-token").
     * <p>Used for API authentication in request parameters or headers.</p>
     */
    String API_TOKEN = "api-token";
    
    /**
     * External session ID parameter name ("esid").
     * <p>Used for tracking external session identifiers.</p>
     */
    String EXTERNAL_SESSION_ID = "esid";
    
    /**
     * reCAPTCHA token parameter name ("g-recaptcha-response").
     * <p>Standard Google reCAPTCHA response parameter.</p>
     */
    String RECAPTCHA_TOKEN = "g-recaptcha-response";
    
    /**
     * CAPTCHA verification status attribute ("captchaVerified").
     * <p>Session attribute indicating successful CAPTCHA validation.</p>
     */
    String CAPTCHA_VERIFIED = "captchaVerified";

    /**
     * Affiliation code entity identifier (no prefix).
     * <p>Note: TODO - consider changing to lowercase or removing if unused.</p>
     */
    String AFFILIATION_CODE = "affiliationCode"; //TODO change to affiliationcode or remove if not used
    
    /** Affiliation code path with slash prefix ("/affiliationCode"). */
    String _AFFILIATION_CODE = "/" + AFFILIATION_CODE;
    
    /**
     * Affiliation event entity identifier (no prefix).
     * <p>Note: TODO - consider changing to lowercase or removing if unused.</p>
     */
    String AFFILIATION_EVENT = "affiliationEvent";  //TODO change to affiliationevent or remove if not used
    
    /** Affiliation event path with slash prefix ("/affiliationEvent"). */
    String _AFFILIATION_EVENT = "/" + AFFILIATION_EVENT;

    /** Component instance identifier (no prefix). */
    String CI = "ci";
    
    /** Component instance path with slash prefix ("/ci"). */
    String _CI = "/" + CI;
    
    /** Component name identifier (no prefix). */
    String CN = "cn";
    
    /** Component name path with slash prefix ("/cn"). */
    String _CN = "/" + CN;
    
    /**
     * File asset path prefix ("/file-asset-").
     * <p>Used for constructing file asset URLs with dynamic suffixes.</p>
     */
    String FILE_ASSET = "/file-asset-";
    
    /** AI feature identifier (no prefix). */
    String AI = "ai";
    
    /** AI feature path with slash prefix ("/ai"). */
    String _AI = "/" + AI;
    
    /** AI prompt entity identifier (no prefix). */
    String PROMPT = "prompt";
    
    /** AI prompt path with slash prefix ("/prompt"). */
    String _PROMPT = "/" + PROMPT;
    
    /** Query entity identifier (no prefix). */
    String QUERY = "query";
    
    /** Query path with slash prefix ("/query"). */
    String _QUERY = "/" + QUERY;

    /** CSV export path ("/csv"). */
    String _CSV = "/csv";
    
    /** Report path ("/report"). */
    String _REPORT = "/report";
    
    /** Query report entity identifier (no prefix). */
    String QUERY_REPORT = "queryreport";
    
    /** Query report path with slash prefix ("/queryreport"). */
    String _QUERY_REPORT = "/" + QUERY_REPORT;

}

package com.openkoda.service.dynamicentity;

import com.openkoda.core.form.FrontendMappingFieldDefinition;
import net.bytebuddy.description.type.TypeDescription;

import java.util.Collection;

import static com.openkoda.service.dynamicentity.DynamicEntityRegistrationService.PACKAGE;

/**
 * Mutable metadata descriptor for Byte Buddy runtime JPA entity class generation.
 * <p>
 * Value object storing complete entity definition metadata used by Byte Buddy for dynamic class generation.
 * Contains entity class name, database table name, field definitions (name, Java type, nullable, length,
 * default value, SQL formulas), JPA mapping metadata, validation constraints, and load state tracking.
 * </p>
 * <p>
 * <b>Descriptor Lifecycle:</b>
 * </p>
 * <ol>
 * <li><b>Creation:</b> DynamicEntityDescriptorFactory.create() builds descriptor from Form entity</li>
 * <li><b>Storage:</b> Registered in DynamicEntityDescriptorFactory static map keyed by entityKey</li>
 * <li><b>Retrieval:</b> DynamicEntityRegistrationService.buildAndLoadDynamicClasses() fetches loadable descriptors</li>
 * <li><b>Usage:</b> Passed to Byte Buddy DynamicType.Builder for bytecode generation</li>
 * <li><b>Completion:</b> setLoaded(true) marks descriptor as processed</li>
 * <li><b>Runtime:</b> Descriptor metadata referenced during entity instantiation and queries</li>
 * </ol>
 * <p>
 * <b>Mutability Design:</b>
 * Unlike typical value objects, this class is mutable with public setters to support incremental descriptor
 * building during complex form processing workflows. Thread-safety must be managed externally.
 * </p>
 * <p>
 * <b>Key Components:</b>
 * </p>
 * <ul>
 * <li>entityClassName: Base name without package (e.g., 'FormEntity')</li>
 * <li>tableName: Database table name (e.g., 'dynamic_entity_form_123')</li>
 * <li>entityKey: Unique identifier derived from form name</li>
 * <li>repositoryName: Base repository interface name</li>
 * <li>fields: Collection of FrontendMappingFieldDefinition with types/constraints</li>
 * <li>timeMillis: Optional timestamp suffix for class versioning</li>
 * <li>isLoaded: Flag indicating Byte Buddy processing completion</li>
 * <li>typeDescription: Eager-initialized DynamicEntityTypeDescription for Byte Buddy</li>
 * </ul>
 * <p>
 * <b>Example Usage:</b>
 * </p>
 * <pre>
 * DynamicEntityDescriptor descriptor = factory.createDescriptor(formEntity);
 * DynamicType.Unloaded entityClass = byteBuddy.subclass(Object.class).name(descriptor.getSuffixedEntityClassName()).make();
 * </pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see DynamicEntityDescriptorFactory
 * @see DynamicEntityRegistrationService
 * @see FrontendMappingFieldDefinition
 * @see DynamicEntityTypeDescription
 */
public class DynamicEntityDescriptor {

    private String entityClassName;
    private String tableName;

    private String entityKey;
    private String repositoryName;
    private Collection<FrontendMappingFieldDefinition> fields;
    private Long timeMillis;
    private boolean isLoaded;

    private TypeDescription.Latent typeDescription;

    /**
     * Constructs a new DynamicEntityDescriptor with complete entity metadata.
     * <p>
     * Package-private access ensures only DynamicEntityDescriptorFactory creates instances.
     * Initializes descriptor with metadata, sets isLoaded=false, and creates DynamicEntityTypeDescription
     * with fully qualified suffixed name.
     * </p>
     *
     * @param entityClassName Base entity class name without package prefix
     * @param tableName Database table name for CREATE TABLE DDL
     * @param entityKey Unique identifier for registry lookups and many_to_one references
     * @param repositoryName Base repository interface name (Spring Data JPA)
     * @param fields Collection of field definitions with types, constraints, formulas (by reference, not defensive copy)
     * @param timeMillis Optional timestamp for class name versioning (null means no suffix)
     */
    DynamicEntityDescriptor(String entityClassName, String tableName, String entityKey, String repositoryName,
                            Collection<FrontendMappingFieldDefinition> fields, Long timeMillis) {
        this.entityClassName = entityClassName;
        this.tableName = tableName;
        this.entityKey = entityKey;
        this.repositoryName = repositoryName;
        this.fields = fields;
        this.timeMillis = timeMillis;
        this.isLoaded = false;
        this.typeDescription = new DynamicEntityTypeDescription(PACKAGE + getSuffixedEntityClassName(), 0, null, null);
    }

    /**
     * Returns entity class name with optional timestamp suffix.
     * <p>
     * Returns versioned class name for Byte Buddy generation. Suffix enables class reloading
     * during hot-deploy scenarios.
     * </p>
     *
     * @return Entity class name with optional '_timeMillis' suffix (e.g., 'FormEntity_1234567890')
     */
    public String getSuffixedEntityClassName(){
        return entityClassName + getTimeSuffix();
    }

    /**
     * Returns repository interface name with optional timestamp suffix.
     * <p>
     * Returns versioned repository name matching entity naming pattern.
     * </p>
     *
     * @return Repository interface name with optional '_timeMillis' suffix
     */
    public String getSuffixedRepositoryName(){
        return repositoryName + getTimeSuffix();
    }

    /**
     * Indicates whether descriptor is ready for Byte Buddy processing.
     * <p>
     * Used by loadableInstances() filtering in DynamicEntityRegistrationService to identify
     * descriptors not yet processed.
     * </p>
     *
     * @return true if descriptor ready for Byte Buddy processing (!isLoaded)
     */
    public boolean isLoadable(){
        return !isLoaded;
    }
    private String getTimeSuffix(){
        return timeMillis != null ? "_" + timeMillis : "";
    }

    /**
     * Returns the base entity class name without package prefix.
     *
     * @return Base entity class name (e.g., 'FormEntity')
     */
    public String getEntityClassName() {
        return entityClassName;
    }

    /**
     * Sets the base entity class name.
     *
     * @param entityClassName Base entity class name without package prefix
     */
    public void setEntityClassName(String entityClassName) {
        this.entityClassName = entityClassName;
    }

    /**
     * Returns the database table name.
     *
     * @return Database table name for CREATE TABLE DDL (e.g., 'dynamic_entity_form_123')
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the database table name.
     *
     * @param tableName Database table name for CREATE TABLE DDL
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the base repository interface name.
     *
     * @return Repository interface name (Spring Data JPA)
     */
    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * Sets the base repository interface name.
     *
     * @param repositoryName Repository interface name (Spring Data JPA)
     */
    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    /**
     * Returns the collection of field definitions.
     * <p>
     * WARNING: Returns collection by reference, not defensive copy. External modifications
     * will affect the descriptor.
     * </p>
     *
     * @return Collection of field definitions with types, constraints, and formulas
     */
    public Collection<FrontendMappingFieldDefinition> getFields() {
        return fields;
    }

    /**
     * Sets the collection of field definitions.
     *
     * @param fields Collection of field definitions (stored by reference)
     */
    public void setFields(Collection<FrontendMappingFieldDefinition> fields) {
        this.fields = fields;
    }

    /**
     * Returns the load state indicating Byte Buddy processing completion.
     *
     * @return true if descriptor has been processed by Byte Buddy, false otherwise
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Sets the load state to mark descriptor as processed.
     * <p>
     * Called by DynamicEntityRegistrationService after successful Byte Buddy class generation
     * and JPA registration.
     * </p>
     *
     * @param loaded true to mark as processed, false to mark as pending
     */
    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    /**
     * Returns the optional timestamp for class versioning.
     *
     * @return Timestamp in milliseconds for class name suffix, or null for no suffix
     */
    public Long getTimeMillis() {
        return timeMillis;
    }

    /**
     * Sets the optional timestamp for class versioning.
     *
     * @param timeMillis Timestamp in milliseconds for class name suffix, or null for no suffix
     */
    public void setTimeMillis(Long timeMillis) {
        this.timeMillis = timeMillis;
    }

    /**
     * Returns the unique entity identifier.
     *
     * @return Unique identifier for registry lookups and many_to_one references
     */
    public String getEntityKey() {
        return entityKey;
    }

    /**
     * Sets the unique entity identifier.
     *
     * @param entityKey Unique identifier derived from form name
     */
    public void setEntityKey(String entityKey) {
        this.entityKey = entityKey;
    }

    /**
     * Returns the Byte Buddy type description.
     *
     * @return Latent type description containing fully qualified suffixed name for Byte Buddy
     */
    public TypeDescription.Latent getTypeDescription() {
        return typeDescription;
    }

    /**
     * Sets the Byte Buddy type description.
     *
     * @param typeDescription Latent type description for Byte Buddy metadata
     */
    public void setTypeDescription(TypeDescription.Latent typeDescription) {
        this.typeDescription = typeDescription;
    }
}

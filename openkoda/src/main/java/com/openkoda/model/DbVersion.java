package com.openkoda.model;

import com.openkoda.model.common.AuditableEntity;
import com.openkoda.model.common.ModelConstants;
import com.openkoda.model.common.TimestampedEntity;
import jakarta.persistence.*;

/**
 * JPA entity representing database schema version for migration tracking and upgrade orchestration.
 * <p>
 * Persisted to 'db_version' table. Tracks applied database schema migrations via semantic version 
 * numbers (major.minor.build.revision). Each version encodes to single comparable integer: 
 * major*10000000 + minor*100000 + build*100 + revision. Implements {@link Comparable} via 
 * {@link #value()} for version ordering.
 * 
 * <p>
 * Used by DbVersionService to determine which migrations to execute during application startup. 
 * Extends {@link TimestampedEntity} for creation/update timestamps. Implements {@link AuditableEntity} 
 * for audit trail.
 * 
 * <p>
 * <b>Version Encoding:</b> Composite version (major, minor, build, revision) encoded to integer 
 * for comparison. Example: version 1.7.1.0 = 1*10000000 + 7*100000 + 1*100 = 10700100
 * 
 * <p>
 * <b>Migration Workflow:</b> DbVersionService queries db_version records to determine current schema 
 * state, executes pending SQL blocks, persists new version records with done=true on success.
 * 
 * <p>
 * <b>WARNING:</b> {@link #value()} and {@link #toString()} methods use Integer wrapper unboxing 
 * without null checks - potential NPE risk if version components not initialized.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.openkoda.service.upgrade.DbVersionService
 */
@Entity
public class DbVersion extends TimestampedEntity implements AuditableEntity, Comparable<DbVersion> {

    private static final long serialVersionUID = -5528831881473946144L;

    /**
     * Primary key from seqGlobalId sequence.
     */
    @Id
    @SequenceGenerator(name = GLOBAL_ID_GENERATOR, sequenceName = GLOBAL_ID_GENERATOR, initialValue = ModelConstants.INITIAL_GLOBAL_VALUE, allocationSize = 10)
    @GeneratedValue(generator = ModelConstants.GLOBAL_ID_GENERATOR, strategy = GenerationType.SEQUENCE)
    private Long id;
    
    /**
     * Semantic version number component - major version.
     * <p>
     * Null values treated as 0 in {@link #value()} calculation but cause NPE in {@link #toString()}.
     * 
     */
    @Column(nullable = false, updatable = true, name = "major")
    private Integer major;
    
    /**
     * Semantic version number component - minor version.
     * <p>
     * Null values treated as 0 in {@link #value()} calculation but cause NPE in {@link #toString()}.
     * 
     */
    @Column(nullable = false, updatable = true, name = "minor")
    private Integer minor;
    
    /**
     * Semantic version number component - build number.
     * <p>
     * Null values treated as 0 in {@link #value()} calculation but cause NPE in {@link #toString()}.
     * 
     */
    @Column(nullable = false, updatable = true, name = "build")
    private Integer build;
    
    /**
     * Semantic version number component - revision number.
     * <p>
     * Null values treated as 0 in {@link #value()} calculation but cause NPE in {@link #toString()}.
     * 
     */
    @Column(nullable = false, updatable = true, name = "revision")
    private Integer revision;
    
    /**
     * Boolean flag indicating successful migration completion.
     * <p>
     * True if version applied successfully, false or null otherwise.
     * 
     */
    @Column(nullable = true, updatable = true, name = "done")
    private Boolean done;
    
    /**
     * Optional description or comment about migration.
     */
    @Column(nullable = true, updatable = true, name = "note")
    private String note;
    
    /**
     * Transient flag controlling if migration should run during initialization.
     * <p>
     * Not persisted to database. Used by DbVersionService to determine execution timing.
     * 
     */
    @Transient
    private boolean runOnInit = false;
    
    /**
     * Constructs a DbVersion with specified semantic version components.
     *
     * @param major major version number (should not be null to avoid NPE in toString/value methods)
     * @param minor minor version number (should not be null to avoid NPE in toString/value methods)
     * @param build build number (should not be null to avoid NPE in toString/value methods)
     * @param revision revision number (should not be null to avoid NPE in toString/value methods)
     */
    public DbVersion(Integer major, Integer minor, Integer build, Integer revision) {
        super();
        this.major = major;
        this.minor = minor;
        this.build = build;
        this.revision = revision;
    }

    /**
     * Default no-argument constructor required by JPA.
     */
    public DbVersion() {
        // TODO Auto-generated constructor stub
    }
    
    /**
     * Returns the primary key identifier.
     *
     * @return the database primary key, or null if not yet persisted
     */
    @Override
    public Long getId() {
        return this.id;
    }
    
    /**
     * Sets the primary key identifier.
     *
     * @param id the database primary key to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns audit trail string representation of this entity.
     * <p>
     * Currently returns null - audit string not implemented for DbVersion.
     * 
     *
     * @return null (audit string not implemented)
     */
    @Override
    public String toAuditString() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Returns the major version number component.
     *
     * @return the major version number, may be null
     */
    public Integer getMajor() {
        return major;
    }

    /**
     * Sets the major version number component.
     *
     * @param major the major version number to set (should not be null to avoid NPE)
     */
    public void setMajor(Integer major) {
        this.major = major;
    }

    /**
     * Returns the minor version number component.
     *
     * @return the minor version number, may be null
     */
    public Integer getMinor() {
        return minor;
    }

    /**
     * Sets the minor version number component.
     *
     * @param minor the minor version number to set (should not be null to avoid NPE)
     */
    public void setMinor(Integer minor) {
        this.minor = minor;
    }

    /**
     * Returns the build number component.
     *
     * @return the build number, may be null
     */
    public Integer getBuild() {
        return build;
    }

    /**
     * Sets the build number component.
     *
     * @param build the build number to set (should not be null to avoid NPE)
     */
    public void setBuild(Integer build) {
        this.build = build;
    }

    /**
     * Returns the revision number component.
     *
     * @return the revision number, may be null
     */
    public Integer getRevision() {
        return revision;
    }

    /**
     * Sets the revision number component.
     *
     * @param revision the revision number to set (should not be null to avoid NPE)
     */
    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    /**
     * Returns the migration completion flag.
     *
     * @return true if migration completed successfully, false or null otherwise
     */
    public Boolean getDone() {
        return done;
    }

    /**
     * Sets the migration completion flag.
     *
     * @param done true if migration completed successfully, false otherwise
     */
    public void setDone(Boolean done) {
        this.done = done;
    }

    /**
     * Returns the optional migration note.
     *
     * @return the migration description or comment, may be null
     */
    public String getNote() {
        return note;
    }

    /**
     * Sets the optional migration note.
     *
     * @param note the migration description or comment to set
     */
    public void setNote(String note) {
        this.note = note;
    }
    
    /**
     * Returns whether migration should run during initialization.
     *
     * @return true if migration should run on init, false otherwise
     */
    public boolean isRunOnInit() {
        return runOnInit;
    }

    /**
     * Sets whether migration should run during initialization.
     *
     * @param runOnInit true to run migration on init, false otherwise
     */
    public void setRunOnInit(boolean runOnInit) {
        this.runOnInit = runOnInit;
    }

    /**
     * Returns formatted version string in major.minor.build.revision format.
     * <p>
     * <b>WARNING:</b> NPE if any version component is null. Example: "1.7.1.0"
     * 
     *
     * @return formatted version string (e.g., "1.7.1.0")
     * @throws NullPointerException if any version component (major, minor, build, revision) is null
     */
    @Override
    public String toString() {
        return String.format("%d.%d.%d.%d", major, minor, build, revision);
    }
    
    /**
     * Returns encoded integer version for comparison.
     * <p>
     * Calculation: major*10000000 + minor*100000 + build*100 + revision.
     * Example: version 1.7.1.0 encodes to 10700100.
     * 
     * <p>
     * <b>WARNING:</b> NPE if any version component is null during unboxing.
     * 
     *
     * @return encoded integer version value for comparison
     * @throws NullPointerException if any version component (major, minor, build, revision) is null
     */
    private int value() {
        return this.major * 10000000 + this.minor * 100000 + this.build * 100 + this.revision;
    }
    
    /**
     * Returns hash code based on encoded version value.
     *
     * @return hash code computed from version components
     * @throws NullPointerException if any version component is null (via {@link #value()})
     */
    @Override
    public int hashCode() {
        return value();
    }
    
    /**
     * Compares this DbVersion to another object for equality.
     * <p>
     * Two DbVersion instances are equal if all version components (major, minor, build, revision)
     * and the done flag have equal values.
     * 
     *
     * @param obj the object to compare with
     * @return true if all version components and done flag are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        return obj != null && ((DbVersion)obj).major == this.major && 
               ((DbVersion)obj).minor == this.minor &&
               ((DbVersion)obj).build == this.build &&
               ((DbVersion)obj).revision == this.revision &&
               ((DbVersion)obj).done == this.done;
    }

    /**
     * Compares versions via encoded {@link #value()} for ordering.
     * <p>
     * Enables version ordering for migration sequence determination. Lower encoded values
     * represent earlier versions.
     * 
     *
     * @param obj the DbVersion to compare with
     * @return negative if this version is earlier, zero if equal, positive if this version is later
     * @throws NullPointerException if version components are null (via {@link #value()})
     */
    @Override
    public int compareTo(DbVersion obj) {
        return Integer.compare(this.value(), obj.value());
    }
}

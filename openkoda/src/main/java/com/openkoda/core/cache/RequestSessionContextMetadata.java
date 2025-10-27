package com.openkoda.core.cache;

import java.util.Objects;

/**
 * A typed generic metadata and identity holder used by request-session cache buckets to manage
 * cached data with precise scope and lifecycle control.
 * <p>
 * This class stores immutable identifiers (sessionId, externalSessionId, timestamp, requestURI)
 * that define the cache key, plus mutable state (isWidget flag and cached payload) that supports
 * dynamic widget detection and result storage. The timestamp ensures cache freshness by requiring
 * exact millisecond-level matching between requests. The requestURI enables cache key disambiguation
 * for multiple concurrent requests within the same session.
 * </p>
 * <p>
 * Thread-safety: This class has no internal synchronization. Callers must coordinate concurrent
 * mutation if instances are shared across threads. The immutable identifier fields are safe for
 * concurrent reads, but the isWidget flag and cached payload require external synchronization
 * during modification.
 * </p>
 * <p>
 * Warning: The equals() implementation uses unchecked cast and String.equals() without null guards.
 * It may throw ClassCastException if compared with objects of incompatible types, or NullPointerException
 * if identifier fields are null. The cached payload (type T) is intentionally excluded from
 * equals/hashCode calculations to maintain cache key identity regardless of stored results.
 * </p>
 *
 * @param <T> type of cached payload object
 * @author OpenKoda Team (original author: borowa)
 * @version 1.7.1
 * @since 1.7.1
 */
public class RequestSessionContextMetadata<T> {
    /**
     * HTTP session identifier used as primary cache bucket key. Immutable after construction.
     */
    private final String sessionId;
    
    /**
     * External session identifier for cross-session correlation. Immutable after construction.
     */
    private final String externalSessionId;
    
    /**
     * Request timestamp in milliseconds. Cache entries are considered fresh only when timestamps
     * match exactly. Immutable after construction.
     */
    private final long timestamp;
    
    /**
     * Flag indicating if request originated from a widget component within a dashboard.
     * Used when a request is fired by a widget being part of a whole dashboard or set of
     * other independent requests fired in parallel. Mutable to support dynamic widget detection.
     */
    private boolean isWidget;
    
    /**
     * Request URI path for cache key disambiguation. Immutable after construction.
     */
    private final String requestURI;
    
    /**
     * Cached payload object of type T. Mutable to store computation results.
     */
    private T cached;
    
    /**
     * Constructs a new request-session context metadata instance with the specified identifiers
     * and state flags.
     *
     * @param sessionId HTTP session ID
     * @param externalSessionId external session ID for correlation (may be null)
     * @param timestamp request timestamp in milliseconds from System.currentTimeMillis()
     * @param isWidget true if request is from a widget component
     * @param requestURI request URI path
     */
    public RequestSessionContextMetadata(String sessionId, String externalSessionId, long timestamp,
            boolean isWidget, String requestURI) {
        super();
        this.sessionId = sessionId;
        this.externalSessionId = externalSessionId;
        this.timestamp = timestamp;
        this.isWidget = isWidget;
        this.requestURI = requestURI;
    }
    
    /**
     * Returns the HTTP session identifier.
     *
     * @return the HTTP session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the external session identifier used for cross-session correlation.
     *
     * @return the external session ID, may be null
     */
    public String getExternalSessionId() {
        return externalSessionId;
    }

    /**
     * Returns the request timestamp in milliseconds.
     *
     * @return the timestamp in milliseconds from System.currentTimeMillis()
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns whether the request originated from a widget component.
     *
     * @return true if the request is from a widget component, false otherwise
     */
    public boolean isWidget() {
        return isWidget;
    }
    
    /**
     * Sets whether the request originated from a widget component.
     * This allows dynamic widget detection after initial construction.
     *
     * @param isWidget true to mark the request as from a widget component, false otherwise
     */
    public void setWidget(boolean isWidget) {
        this.isWidget = isWidget;
    }
    
    /**
     * Returns the request URI path used for cache key disambiguation.
     *
     * @return the request URI path
     */
    public String getRequestURI() {
        return requestURI;
    }

    /**
     * Returns the cached payload object.
     *
     * @return the cached object of type T, may be null if not yet cached
     */
    public T getCached() {
        return cached;
    }
    
    /**
     * Sets the cached payload object, storing a computation result for reuse.
     *
     * @param cached the cached object of type T to store
     */
    public void setCached(T cached) {
        this.cached = cached;
    }
    
    /**
     * Compares this metadata instance with another object for equality based on all identifier
     * fields except the cached payload.
     * <p>
     * Warning: This implementation uses unchecked cast without type checking and String.equals()
     * without null guards. May throw ClassCastException if obj is not of type
     * RequestSessionContextMetadata, or NullPointerException if any identifier field is null.
     * </p>
     *
     * @param obj the object to compare with
     * @return true if all identifier fields match (sessionId, externalSessionId, timestamp,
     *         isWidget, requestURI), false otherwise
     */
    @Override
    public boolean equals(Object obj) {     
        @SuppressWarnings("unchecked")
        RequestSessionContextMetadata<T> objCasted = (RequestSessionContextMetadata<T>)obj;
        return objCasted != null && this.isWidget == objCasted.isWidget && this.timestamp == objCasted.timestamp && this.sessionId.equals(objCasted.sessionId) 
                && this.externalSessionId.equals(objCasted.externalSessionId) && this.requestURI.equals(objCasted.requestURI);
    }
    
    /**
     * Returns the hash code for this metadata instance based on all identifier fields.
     * The cached payload is excluded from the hash calculation to maintain consistent
     * cache key identity regardless of stored results.
     *
     * @return hash code computed from sessionId, externalSessionId, timestamp, isWidget, and requestURI
     */
    @Override
    public int hashCode() {            
        return Objects.hash(sessionId, externalSessionId, timestamp, isWidget, requestURI);
    }
}
package com.openkoda.core.cache;

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import static com.openkoda.controller.common.URLConstants.EXTERNAL_SESSION_ID;

/**
 * Request-scoped memoization cache service for expensive operations within HTTP session boundaries.
 * <p>
 * This service implements the tryGet(key, supplier) pattern: checks cache for existing result, executes 
 * supplier function if cache miss, stores result for subsequent requests within same session and timestamp.
 * </p>
 * <p>
 * Cache lifecycle is bound to HTTP session ID and request timestamp. Entries persist across multiple 
 * requests within the same session ONLY when timestamp parameter matches exactly. This enables consistent 
 * data views within a single user interaction flow while preventing stale data across different operations.
 * </p>
 * <p>
 * Cache structure maintains per-Class type buckets using ConcurrentHashMap. Each bucket contains 
 * session-keyed metadata entries with ReentrantReadWriteLock for write serialization. Type-safe 
 * access is provided through Class tokens as cache keys.
 * </p>
 * <p>
 * Limitations: In-memory only cache, not distributed. No automatic eviction beyond timestamp equality 
 * checking. Entries persist until application restart and can cause memory retention in long-lived sessions.
 * Bucket creation in objectCache is not fully synchronized and can race under heavy concurrency.
 * </p>
 * <p>
 * Thread-safety: Per-bucket ConcurrentHashMaps provide thread-safe reads. Write operations (cache population) 
 * are serialized via ReentrantReadWriteLock per bucket.
 * </p>
 * <p>
 * Configuration is controlled by property ${cache.request.session.enabled:false}. When disabled, 
 * supplier functions execute directly with no caching overhead.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * MyData data = cache.tryGet(MyData.class, () -> expensiveOperation());
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see RequestSessionContextMetadata
 */
@Service
public class RequestSessionCacheService implements LoggingComponentWithRequestId {

    /**
     * Configuration flag controlling cache behavior. When false, supplier functions execute directly 
     * without caching. Configured via property ${cache.request.session.enabled:false} with default 
     * false for safety.
     */
    @Value("${cache.request.session.enabled:false}")
    private boolean requestSessionCacheEnabled;
    
    /**
     * Top-level cache mapping Class tokens to per-type CacheObject buckets. Uses ConcurrentHashMap 
     * for concurrent reads but bucket creation can race. Raw types suppressed.
     */
    @SuppressWarnings("rawtypes")
    private final Map<Class, CacheObject> objectCache = new ConcurrentHashMap<>();
    
    /**
     * Cache bucket for a specific type T. Contains session-keyed metadata map and ReadWriteLock 
     * for write serialization.
     *
     * @param <T> Type of cached objects
     */
    class CacheObject<T> {
        /**
         * Session ID to metadata mapping using ConcurrentHashMap for thread-safe concurrent access.
         */
        Map<String, RequestSessionContextMetadata<T>> cacheMap = new ConcurrentHashMap<>();
        
        /**
         * ReentrantReadWriteLock serializing write operations (cache population). Prevents concurrent 
         * modifications to cache entries.
         */
        ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    }        
    
    /**
     * Attempts to retrieve cached result or executes supplier function if cache miss or caching disabled.
     * <p>
     * This method implements memoization logic for expensive operations. When caching is enabled, 
     * it checks the cache bucket for the given class type. If a cached entry exists with matching 
     * timestamp, returns the cached object. Otherwise, executes the producer supplier, stores the 
     * result, and returns it.
     * </p>
     * <p>
     * Behavior when no servlet request context is available (e.g., background job): supplier executes 
     * directly without caching, returning immediately.
     * </p>
     * <p>
     * Timestamp resolution: Reads timestamp from request parameter 'timestamp' or retrieves generated 
     * timestamp from request attribute 'rtimestamp' with SCOPE_REQUEST(0). If neither exists, generates 
     * new timestamp via System.currentTimeMillis() and stores in request attribute.
     * </p>
     * <p>
     * Locking behavior: Acquires write lock before cache access, releases in finally block. Logs error 
     * on unlock failure but continues execution.
     * </p>
     *
     * @param <T> Type of cached object
     * @param clazz Class token identifying cache bucket for type safety
     * @param producer Supplier function providing object if cache miss. Executed only when needed
     * @return Cached object if timestamp matches, otherwise newly computed object from producer
     * @see RequestSessionContextMetadata
     */
    public <T> T tryGet(Class<T> clazz, Supplier<T> producer) {
        if(!requestSessionCacheEnabled) {
            return producer.get();
        }
        
        @SuppressWarnings("unchecked")
        CacheObject<T> cache = objectCache.get(clazz);
        if(cache == null) {
            objectCache.put(clazz, cache = new CacheObject<T>());
        }
        
        Lock writeLock = cache.cacheLock.writeLock();
        writeLock.lock();
        T object = null;
        try {
            RequestSessionContextMetadata<T> requestSessionMeta = getRequestSessionMetadata();
            if(requestSessionMeta == null) {
                debug(">>> [preHandle] no request context, getting object");
                object = producer.get();
                return object;
            }
            
            RequestSessionContextMetadata<T> cachedRequestSessionMeta = cache.cacheMap.get(requestSessionMeta.getSessionId());
            if(cachedRequestSessionMeta != null && cachedRequestSessionMeta.getTimestamp() == requestSessionMeta.getTimestamp()) {
                debug("=== [preHandle] using cached object {}", clazz);
                object = cachedRequestSessionMeta.getCached();
            } else {
                debug(">>> [preHandle] getting object {}", clazz);
                object = producer.get();
                debug("<<<[preHandle] got object {}", clazz);            
                
                requestSessionMeta.setCached(object);
                cache.cacheMap.put(requestSessionMeta.getSessionId(), requestSessionMeta);
            }
        } finally {
            try {
                writeLock.unlock();
            }catch (Throwable th) {
                error("Could not unlock {}", th.getMessage());
            }
        }
        
        return object;
    }
    
    /**
     * Extracts request session metadata from current RequestContextHolder.
     * <p>
     * Retrieves the current servlet request from Spring's RequestContextHolder and delegates to 
     * the overloaded method for metadata construction. Returns null if no request context is 
     * available, which occurs in background jobs or non-web contexts.
     * </p>
     *
     * @param <T> Type of cached payload
     * @return Metadata for current HTTP request, or null if no request context available
     */
    public <T> RequestSessionContextMetadata<T> getRequestSessionMetadata() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if(attributes != null) {
            HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
            return getRequestSessionMetadata(request);
        } else {
            return null;
        }
    }
    
    /**
     * Constructs request session metadata from HttpServletRequest parameters.
     * <p>
     * Reads 'timestamp' parameter from request, falls back to 'rtimestamp' request attribute if parameter 
     * is blank. If both are missing, generates new timestamp via System.currentTimeMillis() and stores 
     * in 'rtimestamp' request attribute with scope SCOPE_REQUEST(0).
     * </p>
     * <p>
     * Reads 'widget' parameter with case-insensitive TRUE value check to set isWidget flag. Reads 
     * EXTERNAL_SESSION_ID parameter for external session tracking. Retrieves session ID from 
     * HttpSession.getId().
     * </p>
     *
     * @param <T> Type of cached payload
     * @param request Current HTTP servlet request containing parameters and session
     * @return Metadata containing sessionId, externalSessionId, timestamp, isWidget flag, and requestURI
     */
    public <T> RequestSessionContextMetadata<T> getRequestSessionMetadata(HttpServletRequest request) {
        final boolean isWidget = "TRUE".equalsIgnoreCase(request.getParameter("widget"));
        long timestamp = -1;
        String timestampString = request.getParameter("timestamp");
        if(StringUtils.isBlank(timestampString)) {
            timestampString = (String)request.getAttribute("rtimestamp");
        }
        
        if(StringUtils.isBlank(timestampString)) {
            RequestContextHolder.getRequestAttributes().setAttribute("rtimestamp", String.valueOf(System.currentTimeMillis()), 0);
            timestampString = (String)request.getAttribute("rtimestamp");
        }
        
        if(StringUtils.isNotBlank(timestampString)) { 
            timestamp = Long.parseLong(timestampString.trim());
        }
        
        final String sessionId = request.getSession().getId();
        String externalSession = request.getParameter(EXTERNAL_SESSION_ID);
        return new RequestSessionContextMetadata<>(sessionId, externalSession, timestamp, isWidget, request.getRequestURI());
    }
}

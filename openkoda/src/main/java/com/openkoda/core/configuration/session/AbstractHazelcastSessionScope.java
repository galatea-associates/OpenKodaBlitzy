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

package com.openkoda.core.configuration.session;

import com.openkoda.core.helper.ClusterHelper;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.SessionScope;

import java.util.concurrent.TimeUnit;


/**
 * Abstract base class for Hazelcast-aware custom session scope enabling distributed session-scoped beans 
 * in clustered environments. Extends Spring {@link SessionScope} to add cross-node replication via 
 * Hazelcast {@code ReplicatedMap} when cluster is detected. In standalone mode delegates to standard 
 * {@code HttpSession} storage.
 * <p>
 * Uses {@link ClusterHelper} to detect cluster mode and access {@code HazelcastInstance}. Composes 
 * per-session keys from {@code keyPrefix + name + session ID}. Supports configurable TTL for cached entries.
 * 
 * <p>
 * The general contract is following:
 * If the application works in cluster (ie. {@code ClusterHelper.isCluster() == true}),
 * then add the bean to the replicated Hazelcast cache.
 * If the application is standalone (ie. {@code ClusterHelper.isCluster() == false}),
 * then use standard http session instead.
 * 
 * <p>
 * Optionally a custom bean factory method (objectFactory) can be provided for creation of specialized beans.
 * Optionally a eviction time in seconds for hazelcast entries can be specified.
 * 
 * <p>
 * Thread-safe when used with {@code RequestContextHolder} for session ID access. {@code ReplicatedMap} 
 * operations are thread-safe.
 * 
 *
 * @param <T> the type of session-scoped bean
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * See {@code ClusterHelper}
 * See {@code SessionScope}
 * See {@code com.hazelcast.core.ReplicatedMap}
 */
public abstract class AbstractHazelcastSessionScope<T> extends SessionScope {

    /**
     * Name of the Hazelcast cache storing sessions
     */
    public static final String CACHE_NAME = "HazelcastScope";

    /**
     * Custom ObjectFactory for specialized bean creation. Optional - when not set, the default 
     * Spring-provided factory is used.
     */
    private ObjectFactory<T> customObjectFactory;

    /**
     * Key prefix for cache entries, used to compose unique keys and avoid collisions between different 
     * scope implementations.
     */
    private final String keyPrefix;

    /**
     * TTL for hazelcast entries in seconds (0 == no eviction). Must be tuned for memory vs stale-data 
     * trade-offs based on application requirements.
     */
    private final long entryTTLInSeconds;

    /**
     * Constructs a new AbstractHazelcastSessionScope with specified key prefix and TTL. Both parameters 
     * are immutable after construction.
     *
     * @param keyPrefix prefix for keys in hazelcast cache
     * @param entryTTLInSeconds optional entry eviction time in seconds (0 == no eviction)
     */
    public AbstractHazelcastSessionScope(String keyPrefix, long entryTTLInSeconds) {
        this.keyPrefix = keyPrefix;
        this.entryTTLInSeconds = entryTTLInSeconds;
    }

    /**
     * Retrieves or creates session-scoped bean from Hazelcast {@code ReplicatedMap} in cluster mode, 
     * {@code HttpSession} in standalone mode. Checks {@code ClusterHelper.isCluster()} to determine 
     * storage strategy. In cluster mode composes key from {@code keyPrefix + name + session ID}, 
     * queries {@code ReplicatedMap}, creates via {@code customObjectFactory} if set otherwise uses 
     * provided factory, stores with TTL if configured.
     *
     * @param s the bean name
     * @param objectFactory the Spring ObjectFactory for creating new instances
     * @return the session-scoped bean instance
     * @see ClusterHelper#isCluster()
     */
    @Override
    public Object get(String s, ObjectFactory<?> objectFactory) {

        //if not cluster, use standard session scope
        if (!ClusterHelper.isCluster()) {
            Object result = super.get(s, objectFactory);
            return result;
        }

        String key = getEntryKey(s);
        Object existingEntry = ClusterHelper.getHazelcastInstance().getReplicatedMap(CACHE_NAME).get(key);

        //if entry already exists, return
        if (existingEntry != null) {
            return existingEntry;
        }

        //otherwise, create new entry and insert it into hazelcast
        Object newEntry = customObjectFactory == null ? objectFactory.getObject() : customObjectFactory.getObject();
        if (entryTTLInSeconds > 0) {
            ClusterHelper.getHazelcastInstance().getReplicatedMap(CACHE_NAME).put(key, newEntry, entryTTLInSeconds, TimeUnit.SECONDS);
        } else {
            ClusterHelper.getHazelcastInstance().getReplicatedMap(CACHE_NAME).put(key, newEntry);
        }
        return newEntry;
    }

    /**
     * Composes cache key from keyPrefix + bean name + session ID.
     *
     * @param s the bean name
     * @return the composed cache key
     */
    private String getEntryKey(String s) {
        return keyPrefix + s + RequestContextHolder.currentRequestAttributes().getSessionId();
    }

    /**
     * Removes session-scoped bean from storage (Hazelcast or HttpSession based on cluster mode). 
     * In cluster mode removes from {@code ReplicatedMap} using composed key. In standalone mode 
     * delegates to superclass HttpSession removal.
     *
     * @param s the bean name to remove
     * @return the removed bean instance or null
     */
    @Override
    public Object remove(String s) {
        //if not cluster, use standard session scope
        if (!ClusterHelper.isCluster()) {
            return super.remove(s);
        }
        String key = getEntryKey(s);
        Object result = ClusterHelper.getHazelcastInstance().getReplicatedMap(CACHE_NAME).remove(key);

        return result;
    }

    /**
     * Registers destruction callback. In cluster mode this is a no-op as Hazelcast-stored objects 
     * do not support destruction callbacks. In standalone mode delegates to superclass.
     *
     * @param s the bean name
     * @param runnable the callback to execute on destruction
     */
    @Override
    public void registerDestructionCallback(String s, Runnable runnable) {
        //if not cluster, use standard session scope
        if (!ClusterHelper.isCluster()) {
            super.registerDestructionCallback(s, runnable);
        }
    }

    /**
     * Resolves contextual objects. In cluster mode returns null. In standalone mode delegates to superclass.
     *
     * @param s the contextual object key
     * @return contextual object or null
     */
    @Override
    public Object resolveContextualObject(String s) {
        //if not cluster, use standard session scope
        if (!ClusterHelper.isCluster()) {
            return super.resolveContextualObject(s);
        }
        return null;
    }

    /**
     * Gets the custom ObjectFactory.
     *
     * @return the custom ObjectFactory or null if not set
     */
    protected ObjectFactory<T> getCustomObjectFactory() {
        return customObjectFactory;
    }

    /**
     * Sets the custom ObjectFactory for specialized bean creation.
     *
     * @param customObjectFactory the custom ObjectFactory for specialized bean creation
     */
    protected void setCustomObjectFactory(ObjectFactory<T> customObjectFactory) {
        this.customObjectFactory = customObjectFactory;
    }

}
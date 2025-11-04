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

import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Provides cluster configuration metadata and node detection using Hazelcast.
 * <p>
 * This helper computes master and member status from injected properties and exposes
 * a static API for cluster information access. The static instance is initialized via
 * {@code @PostConstruct} during Spring bean lifecycle.
 * <p>
 * Hazelcast integration enables multi-node deployments with automatic cluster discovery.
 * When Hazelcast is not configured, the helper operates in single-node mode.
 * <p>
 * Example usage:
 * <pre>{@code
 * if (ClusterHelper.isMaster()) {
 *     // Schedule cluster-wide job
 * }
 * }</pre>
 * <p>
 * <b>Warning:</b> Static methods return null or default values before the Spring
 * {@code @PostConstruct} initialization completes. Ensure Spring context is fully
 * loaded before accessing cluster information.
 * <p>
 * <b>Thread Safety:</b> Static methods read instance fields that are set once during
 * startup. Safe for concurrent access after initialization.
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
@Component("cluster")
public class ClusterHelper implements LoggingComponentWithRequestId {

    /**
     * Hazelcast topic name for cluster-wide event broadcasting.
     * <p>
     * Use this constant to publish and subscribe to events that should be
     * distributed across all cluster nodes. The topic enables inter-node
     * communication for cache invalidation, configuration updates, and
     * application-level notifications.
     * 
     */
    public static final String CLUSTER_EVENT_TOPIC = "clusterEvent";

    private static ClusterHelper instance;

    @Value("${hazelcast.members.commaSeparated:127.0.0.1}")
    private String hazelcastMembersCommaSeparated;
    @Value("${master.node:127.0.0.1:8080}")
    private String masterNode;

    private String thisNode;

    private boolean isMaster;

    private String[] hazelcastMembers;

    @Autowired(required = false)
    private HazelcastInstance hazelcastInstance;

    /**
     * Checks if Hazelcast is configured and cluster mode is enabled.
     * <p>
     * Detection occurs by verifying the presence of a {@link HazelcastInstance} bean.
     * When Hazelcast is not configured (instance is null), the application runs in
     * single-node mode without cluster coordination.
     * 
     *
     * @return true if Hazelcast is configured and cluster mode is active, false otherwise
     */
    public static boolean isCluster() {
        return instance.hazelcastInstance != null;
    }

    @PostConstruct
    private void init() {
        instance = this;
        thisNode = detectThisNode();
        isMaster =  StringUtils.equals(masterNode, thisNode);
        hazelcastMembers = hazelcastMembersCommaSeparated.split(",");
    }

    /**
     * Checks if the current node is the designated master node.
     * <p>
     * Master status is determined by comparing this node's address with the
     * {@code master.node} property. The master node typically handles cluster-wide
     * responsibilities such as scheduled jobs, cache coordination, and administrative
     * tasks.
     * 
     * <p>
     * In single-node deployments, the sole node is always the master.
     * 
     *
     * @return true if this node matches the configured master.node property, false otherwise
     */
    public static boolean isMaster() {
        return instance.isMaster;
    }

    /**
     * Checks if the cluster consists of a single node or is not clustered.
     * <p>
     * Returns true in two scenarios: when Hazelcast is configured with exactly one
     * member, or when clustering is disabled entirely. This method is useful for
     * scheduling decisions where certain tasks should run only once across the cluster.
     * 
     *
     * @return true if the cluster has exactly one member or clustering is disabled, false otherwise
     */
    public static boolean isSingleNodeCluster() {
        return isCluster() ? instance.hazelcastMembers.length == 1 : true;
    }


    /**
     * @return members nodes separated by comma
     */
    public static String getMembersCommaSeparated() {
        return instance.hazelcastMembersCommaSeparated;
    }

    /**
     * @return master node
     */
    public static String getMasterNode() {
        return instance.masterNode;
    }

    /**
     * @return this node
     */
    public static String getThisNode() {
        return instance.thisNode;
    }

    private static String detectThisNode() {
        if (!isCluster()) { return instance.masterNode; }
        Member m = instance.hazelcastInstance.getCluster().getLocalMember();
        return m.getSocketAddress().getHostString() + ":" + m.getAttribute("server.port");
    }

    /**
     * Retrieves all cluster member addresses in "host:port" format.
     * <p>
     * Queries the Hazelcast cluster for active members and returns their network
     * addresses. Each member address includes the hostname and server port attribute.
     * Use this method to iterate over cluster nodes for distributed operations or
     * monitoring.
     * 
     *
     * @return array of member addresses in "host:port" format from the Hazelcast cluster
     */
    public static String[] getMembers() {
        return instance.hazelcastInstance.getCluster().getMembers().stream().map(
                m -> m.getSocketAddress().getHostString() + ":" + m.getAttribute("server.port")).toArray(String[]::new);
    }

    /**
     * Provides direct access to the Hazelcast instance for advanced cluster operations.
     * <p>
     * Returns the injected {@link HazelcastInstance} bean, or null if Hazelcast is not
     * configured. Use this for advanced operations such as distributed data structures,
     * topic publishing, or custom cluster event handling.
     * 
     *
     * @return the Hazelcast instance if clustering is enabled, null otherwise
     */
    public static HazelcastInstance getHazelcastInstance() {
        return instance.hazelcastInstance;
    }
}

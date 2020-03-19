/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

/**
 * Options related to remote code execution in the cluster.
 */
@SuppressWarnings("WeakerAccess")
public class ClusterExecutionOptions {

    /**
     * Whether to try connecting to nodes in "transition" states (not checking in, starting). I.e. nodes that are not considered
     * dead but are not 100% up either. This option is typically used to convey information that is quite relevant.
     */
    private boolean tryNodesInTransition;

    /**
     * Whether to try connecting to nodes in all cases (i.e. also to nodes that are declared dead).
     *
     * It is typically used when connecting to specific (user-chosen) node. If it's down, we can expect relevant exception.
     */
    private boolean tryAllNodes;

    /**
     * If true, default "Accept" header values (XML, JSON, YAML) are not applied.
     */
    private boolean skipDefaultAccept;

    public boolean isTryNodesInTransition() {
        return tryNodesInTransition;
    }

    public void setTryNodesInTransition(boolean tryNodesInTransition) {
        this.tryNodesInTransition = tryNodesInTransition;
    }

    public ClusterExecutionOptions tryNodesInTransition() {
        setTryNodesInTransition(true);
        return this;
    }

    public static boolean isTryNodesInTransition(ClusterExecutionOptions options) {
        return options != null && options.isTryNodesInTransition();
    }

    public boolean isTryAllNodes() {
        return tryAllNodes;
    }

    public void setTryAllNodes(boolean tryAllNodes) {
        this.tryAllNodes = tryAllNodes;
    }

    public ClusterExecutionOptions tryAllNodes() {
        setTryAllNodes(true);
        return this;
    }

    public static boolean isTryAllNodes(ClusterExecutionOptions options) {
        return options != null && options.isTryAllNodes();
    }

    public boolean isSkipDefaultAccept() {
        return skipDefaultAccept;
    }

    public void setSkipDefaultAccept(boolean skipDefaultAccept) {
        this.skipDefaultAccept = skipDefaultAccept;
    }

    public ClusterExecutionOptions skipDefaultAccept() {
        setSkipDefaultAccept(true);
        return this;
    }

    public static boolean isSkipDefaultAccept(ClusterExecutionOptions options) {
        return options != null && options.isSkipDefaultAccept();
    }
}

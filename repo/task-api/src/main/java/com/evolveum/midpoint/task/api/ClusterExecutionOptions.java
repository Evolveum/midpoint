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
     * Whether to try connecting to "not checking in" nodes. I.e. nodes that are not declared dead (running=false) but
     * are not up either (last check-in was more than "nodeTimeout" ago).
     */
    private boolean tryNodesNotCheckingIn;

    /**
     * Whether to try connecting to nodes in all cases (i.e. also to nodes that are declared dead).
     */
    private boolean tryAllNodes;

    public boolean isTryNodesNotCheckingIn() {
        return tryNodesNotCheckingIn;
    }

    public void setTryNodesNotCheckingIn(boolean tryNodesNotCheckingIn) {
        this.tryNodesNotCheckingIn = tryNodesNotCheckingIn;
    }

    public ClusterExecutionOptions tryNodesNotCheckingIn() {
        setTryNodesNotCheckingIn(true);
        return this;
    }

    public static boolean isTryNodesNotCheckingIn(ClusterExecutionOptions options) {
        return options != null && options.isTryNodesNotCheckingIn();
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
}

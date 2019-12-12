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
     * Whether to try connecting to "almost dead" nodes. I.e. nodes that are not declared dead (running=false) but
     * are not up either (last check-in was more than "nodeTimeout" ago).
     */
    private boolean tryAlmostDeadNodes;

    /**
     * Whether to try connecting to nodes in all cases (i.e. also to nodes that are declared dead).
     */
    private boolean tryDeadNodes;

    public boolean isTryAlmostDeadNodes() {
        return tryAlmostDeadNodes;
    }

    public void setTryAlmostDeadNodes(boolean tryAlmostDeadNodes) {
        this.tryAlmostDeadNodes = tryAlmostDeadNodes;
    }

    public ClusterExecutionOptions tryAlmostDeadNodes() {
        setTryAlmostDeadNodes(true);
        return this;
    }

    public static boolean isTryAlmostDeadNodes(ClusterExecutionOptions options) {
        return options != null && options.isTryAlmostDeadNodes();
    }

    public boolean isTryDeadNodes() {
        return tryDeadNodes;
    }

    public void setTryDeadNodes(boolean tryDeadNodes) {
        this.tryDeadNodes = tryDeadNodes;
    }

    public ClusterExecutionOptions tryDeadNodes() {
        setTryDeadNodes(true);
        return this;
    }

    public static boolean isTryDeadNodes(ClusterExecutionOptions options) {
        return options != null && options.isTryDeadNodes();
    }
}

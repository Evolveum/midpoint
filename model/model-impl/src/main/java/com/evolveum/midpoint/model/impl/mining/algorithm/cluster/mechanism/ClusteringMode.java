/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

/**
 * Enumeration representing different clustering modes.
 */
public enum ClusteringMode {

    /**
     * Represents a balanced clustering mode.
     */
    BALANCED,

    /**
     * Represents an unbalanced clustering mode.
     */
    UNBALANCED,

    /**
     * Represents a balanced clustering mode with rules.
     */
    BALANCED_RULES,

    /**
     * Represents an unbalanced clustering mode with rules.
     */
    UNBALANCED_RULES,

    /**
     * Represents a balanced clustering mode with rules for outlier detection.
     */
    BALANCED_RULES_OUTLIER
}

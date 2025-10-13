/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

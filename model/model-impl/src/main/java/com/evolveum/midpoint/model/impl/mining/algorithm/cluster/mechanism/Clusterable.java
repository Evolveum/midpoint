
/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.util.Set;

/**
 * An interface representing data points that can be used in clustering algorithms.
 */
public interface Clusterable {
    Set<String> getPoint();
    int getMembersCount();
}

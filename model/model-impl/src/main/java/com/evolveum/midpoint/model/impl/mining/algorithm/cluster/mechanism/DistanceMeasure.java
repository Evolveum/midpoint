/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.io.Serializable;
import java.util.Set;


public interface DistanceMeasure extends Serializable {
    double compute(Set<String> valueA, Set<String> valueB);
}

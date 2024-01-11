/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.detection;

import java.io.Serializable;
import java.util.Set;

/**
 * The `CandidatePattern` class represents a candidate pattern in role analysis. It contains information about the roles,
 * users, and the cluster metric associated with the candidate pattern.
 */
public class CandidatePattern extends BasePattern implements Serializable {

    public static final String F_METRIC = "metric";

    public static final String F_TYPE = "searchMode";

    public CandidatePattern(Set<String> roles, Set<String> users, Double metric, Long id, String identifier, String associatedColor) {
        super(roles, users, metric, id, identifier, associatedColor);
    }
}

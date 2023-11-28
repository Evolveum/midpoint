
/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.io.Serializable;
import java.util.Set;

/**
 * Represents a data point for clustering, containing members and properties.
 * This class is used in the clustering mechanism for role analysis.
 */
public class DataPoint implements Clusterable, Serializable {

    Set<String> members;
    Set<String> properties;
    int membersCount;

    /**
     * Constructs a DataPoint with the given members and properties.
     *
     * @param members    The set of members associated with the data point.
     * @param properties The set of properties associated with the data point.
     */
    public DataPoint(Set<String> members, Set<String> properties) {
        this.members = members;
        this.properties = properties;
        this.membersCount = members.size();
    }

    @Override
    public Set<String> getPoint() {
        return properties;
    }

    @Override
    public int getMembersCount() {
        return membersCount;
    }

    public Set<String> getMembers() {
        return members;
    }

    public Set<String> getProperties() {
        return properties;
    }

}

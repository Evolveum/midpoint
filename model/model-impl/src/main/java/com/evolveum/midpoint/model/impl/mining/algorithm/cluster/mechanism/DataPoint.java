
/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.ExtensionProperties;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OutlierNoiseCategoryType;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a data point for clustering, containing members and properties.
 * This class is used in the clustering mechanism for role analysis.
 */
public class DataPoint implements Clusterable, Serializable {

    Set<String> members;
    Set<String> properties;
    Set<String> closeNeighbors = new HashSet<>();
    int membersCount;
    ExtensionProperties extensionProperties;
    OutlierNoiseCategoryType pointStatus;

    /**
     * Constructs a DataPoint with the given members and properties.
     *
     * @param members The set of members associated with the data point.
     * @param properties The set of properties associated with the data point.
     */
    public DataPoint(Set<String> members, Set<String> properties) {
        this.members = members;
        this.properties = properties;
        this.membersCount = members.size();
    }

    public DataPoint(Set<String> members, Set<String> properties, ExtensionProperties extensionProperties) {
        this.members = members;
        this.properties = properties;
        this.membersCount = members.size();
        this.extensionProperties = extensionProperties;
    }

    @Override
    public Set<String> getPoint() {
        return properties;
    }

    @Override
    public Set<String> getCloseNeighbors() {
        return null;
    }

    @Override
    public void addCloseNeighbor(String neighbor) {
        this.closeNeighbors.add(neighbor);
    }

    @Override
    public ExtensionProperties getExtensionProperties() {
        return extensionProperties;
    }

    @Override
    public int getMembersCount() {
        return membersCount;
    }

    @Override
    public Set<String> getMembers() {
        return members;
    }

    public Set<String> getProperties() {
        return properties;
    }

    public OutlierNoiseCategoryType getPointStatus() {
        return pointStatus;
    }

    public String getPointStatusIdentificator() {
        if(pointStatus == null){
            return "unknown";
        }
        return pointStatus.value();
    }

    public void setPointStatus(OutlierNoiseCategoryType pointStatus) {
        this.pointStatus = pointStatus;
    }

}

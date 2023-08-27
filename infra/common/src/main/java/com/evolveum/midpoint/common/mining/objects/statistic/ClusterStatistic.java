/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.statistic;

import java.io.Serializable;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ClusterStatistic implements Serializable {

    Set<ObjectReferenceType> membersRef;
    int propertiesCount;
    int membersCount;
    int minVectorPoint;
    int maxVectorPoint;
    double propertiesMean;
    double propertiesDensity;
    PolyStringType name;
    Set<ObjectReferenceType> propertiesRef;

    public ClusterStatistic(PolyStringType name, Set<ObjectReferenceType> membersRef, int membersCount,
            int propertiesCount, int minVectorPoint, int maxVectorPoint, double propertiesMean, double propertiesDensity) {
        this.membersRef = membersRef;
        this.propertiesCount = propertiesCount;
        this.minVectorPoint = minVectorPoint;
        this.maxVectorPoint = maxVectorPoint;
        this.propertiesMean = propertiesMean;
        this.propertiesRef = null;
        this.propertiesDensity = propertiesDensity;
        this.name = name;
        this.membersCount = membersCount;
    }

    public ClusterStatistic(PolyStringType name, Set<ObjectReferenceType> propertiesRef, Set<ObjectReferenceType> membersRef,
            int membersCount, int propertiesCount, int minVectorPoint, int maxVectorPoint, double propertiesMean,
            double propertiesDensity) {
        this.membersRef = membersRef;
        this.propertiesCount = propertiesCount;
        this.minVectorPoint = minVectorPoint;
        this.maxVectorPoint = maxVectorPoint;
        this.propertiesMean = propertiesMean;
        this.propertiesRef = propertiesRef;
        this.propertiesDensity = propertiesDensity;
        this.name = name;
        this.membersCount = membersCount;
    }

    public int getMembersCount() {
        return membersCount;
    }

    public Set<ObjectReferenceType> getPropertiesRef() {
        return propertiesRef;
    }

    public Set<ObjectReferenceType> getMembersRef() {
        return membersRef;
    }

    public int getPropertiesCount() {
        return propertiesCount;
    }

    public int getMinVectorPoint() {
        return minVectorPoint;
    }

    public int getMaxVectorPoint() {
        return maxVectorPoint;
    }

    public double getPropertiesMean() {
        return propertiesMean;
    }

    public double getPropertiesDensity() {
        return propertiesDensity;
    }

    public PolyStringType getName() {
        return name;
    }

}

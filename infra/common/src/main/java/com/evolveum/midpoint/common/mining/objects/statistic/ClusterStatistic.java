/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.statistic;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * The `ClusterStatistic` class provides statistics for a clustering operation, such as the number of members,
 * properties, and various measures related to the clustered data. It contains information about the members,
 * properties, and other clustering-related metrics.
 */
public class ClusterStatistic implements Serializable {

    private final Set<ObjectReferenceType> membersRef;
    private final int propertiesCount;
    private final int membersCount;
    private final int minVectorPoint;
    private final int maxVectorPoint;
    private final double propertiesMean;
    private final double propertiesDensity;
    private final PolyStringType name;
    private final Set<ObjectReferenceType> propertiesRef;
    List<AttributeAnalysisStructure> userAttributeAnalysisStructures;
    List<AttributeAnalysisStructure> roleAttributeAnalysisStructures;

    public ClusterStatistic(){
        membersRef = null;
        propertiesCount = 0;
        membersCount = 0;
        minVectorPoint = 0;
        maxVectorPoint = 0;
        propertiesMean = 0;
        propertiesDensity = 0;
        name = null;
        propertiesRef = null;
    }
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

    public List<AttributeAnalysisStructure> getUserAttributeAnalysisStructures() {
        return userAttributeAnalysisStructures;
    }

    public void setUserAttributeAnalysisStructures(List<AttributeAnalysisStructure> userAttributeAnalysisStructures) {
        this.userAttributeAnalysisStructures = userAttributeAnalysisStructures;
    }

    public List<AttributeAnalysisStructure> getRoleAttributeAnalysisStructures() {
        return roleAttributeAnalysisStructures;
    }

    public void setRoleAttributeAnalysisStructures(List<AttributeAnalysisStructure> roleAttributeAnalysisStructures) {
        this.roleAttributeAnalysisStructures = roleAttributeAnalysisStructures;
    }

}

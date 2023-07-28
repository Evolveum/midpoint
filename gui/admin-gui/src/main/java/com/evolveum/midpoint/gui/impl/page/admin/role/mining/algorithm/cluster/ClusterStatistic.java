/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import java.io.Serializable;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ClusterStatistic implements Serializable {

    Set<ObjectReferenceType> processedObjectRef;
    int totalPoints;
    int totalElements;
    int minVectorPoint;
    int maxVectorPoint;
    int clusterSize;
    double meanPoints;
    double density;
    PolyStringType name;
    Set<ObjectReferenceType> propertiesRef;

    public ClusterStatistic(PolyStringType name, Set<ObjectReferenceType> processedObjectRef, int totalElements,
            int totalPoints, int minVectorPoint, int maxVectorPoint, int clusterSize, double meanPoints, double density) {
        this.processedObjectRef = processedObjectRef;
        this.totalPoints = totalPoints;
        this.minVectorPoint = minVectorPoint;
        this.maxVectorPoint = maxVectorPoint;
        this.clusterSize = clusterSize;
        this.meanPoints = meanPoints;
//        this.frequencyMap = frequencyMap;
        this.propertiesRef = null;
        this.density = density;
        this.name = name;
        this.totalElements = totalElements;
    }

    public ClusterStatistic(PolyStringType name, Set<ObjectReferenceType> propertiesRef, Set<ObjectReferenceType> processedObjectRef, int totalElements,
            int totalPoints, int minVectorPoint, int maxVectorPoint, int clusterSize, double meanPoints, double density) {
        this.processedObjectRef = processedObjectRef;
        this.totalPoints = totalPoints;
        this.minVectorPoint = minVectorPoint;
        this.maxVectorPoint = maxVectorPoint;
        this.clusterSize = clusterSize;
        this.meanPoints = meanPoints;
//        this.frequencyMap = frequencyMap;
        this.propertiesRef = propertiesRef;
        this.density = density;
        this.name = name;
        this.totalElements = totalElements;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public Set<ObjectReferenceType> getPropertiesRef() {
        return propertiesRef;
    }

    public Set<ObjectReferenceType> getProcessedObjectRef() {
        return processedObjectRef;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public int getMinVectorPoint() {
        return minVectorPoint;
    }

    public int getMaxVectorPoint() {
        return maxVectorPoint;
    }

    public int getClusterSize() {
        return clusterSize;
    }

    public double getMeanPoints() {
        return meanPoints;
    }

//    public HashMap<String, Double> getFrequencyMap() {
//        return frequencyMap;
//    }

    public double getDensity() {
        return density;
    }

    public PolyStringType getName() {
        return name;
    }

}

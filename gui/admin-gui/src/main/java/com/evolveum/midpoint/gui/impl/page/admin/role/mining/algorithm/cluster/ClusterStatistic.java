/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import java.io.Serializable;
import java.util.Set;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ClusterStatistic implements Serializable {

    Set<String> elementsOid;
    int totalPoints;
    int totalElements;
    int minVectorPoint;
    int maxVectorPoint;
    int clusterSize;
    double meanPoints;
    double density;
    PolyStringType name;
    String identifier;

    public ClusterStatistic(PolyStringType name, String identifier, Set<String> elementsOid, int totalElements,
            int totalPoints, int minVectorPoint, int maxVectorPoint, int clusterSize, double meanPoints, double density) {
        this.elementsOid = elementsOid;
        this.totalPoints = totalPoints;
        this.minVectorPoint = minVectorPoint;
        this.maxVectorPoint = maxVectorPoint;
        this.clusterSize = clusterSize;
        this.meanPoints = meanPoints;
//        this.frequencyMap = frequencyMap;
        this.density = density;
        this.name = name;
        this.identifier = identifier;
        this.totalElements = totalElements;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Set<String> getElementsOid() {
        return elementsOid;
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

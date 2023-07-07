/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ClusterStatistic implements Serializable {

    List<String> occupiedRoles;
    Set<String> occupiedUsers;
    int totalPoints;
    int minVectorPoint;
    int maxVectorPoint;
    int clusterSize;
    double meanPoints;
    HashMap<String, Double> frequencyMap;
    double density;
    PolyStringType name;
    String identifier;
    List<String> defaultIntersection;

    public ClusterStatistic(PolyStringType name, String identifier, List<String> occupiedRoles, Set<String> occupiedUsers,
            int totalPoints, int minVectorPoint, int maxVectorPoint, int clusterSize, double meanPoints, double density,
            HashMap<String, Double> frequencyMap,List<String> defaultIntersection) {
        this.occupiedRoles = occupiedRoles;
        this.occupiedUsers = occupiedUsers;
        this.totalPoints = totalPoints;
        this.minVectorPoint = minVectorPoint;
        this.maxVectorPoint = maxVectorPoint;
        this.clusterSize = clusterSize;
        this.meanPoints = meanPoints;
        this.frequencyMap = frequencyMap;
        this.density = density;
        this.name = name;
        this.identifier = identifier;
        this.defaultIntersection = defaultIntersection;
    }

    public List<String> getDefaultIntersection() {
        return defaultIntersection;
    }
    public String getIdentifier() {
        return identifier;
    }

    public List<String> getOccupiedRoles() {
        return occupiedRoles;
    }

    public Set<String> getOccupiedUsers() {
        return occupiedUsers;
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

    public HashMap<String, Double> getFrequencyMap() {
        return frequencyMap;
    }

    public double getDensity() {
        return density;
    }

    public PolyStringType getName() {
        return name;
    }

}

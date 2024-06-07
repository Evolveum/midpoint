package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

public class SimpleHeatPattern implements Serializable {

    List<String> propertiesOids;
    int propertiesCount = 0;
    int totalRelations = 0;
    int key;

    public SimpleHeatPattern(List<String> propertiesOids, int key) {
        this.propertiesOids = propertiesOids;
        this.propertiesCount = propertiesOids.size();
        this.key = key;
    }

    public List<String> getPropertiesOids() {
        return propertiesOids;
    }

    public int getKey() {
        return key;
    }

    public boolean isPartOf(@NotNull Set<String> properties) {
        return properties.containsAll(propertiesOids);
    }

    public int getPropertiesCount() {
        return propertiesCount;
    }

    public int getTotalRelations() {
        return totalRelations;
    }

    public void incrementTotalRelations(int incrementNumber) {
        totalRelations += incrementNumber;
    }

    public void removeTotalRelations() {
        totalRelations = 0;
    }

}

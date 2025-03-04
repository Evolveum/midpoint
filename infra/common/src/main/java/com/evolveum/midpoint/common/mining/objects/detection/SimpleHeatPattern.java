package com.evolveum.midpoint.common.mining.objects.detection;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

public class SimpleHeatPattern implements Serializable {

    transient List<String> members;
    int propertiesCount = 0;
    int totalRelations = 0;
    int key;

    public SimpleHeatPattern(@NotNull List<String> members, int key) {
        this.members = members;
        this.propertiesCount = members.size();
        this.key = key;
    }

    public List<String> getMembers() {
        return members;
    }

    public int getKey() {
        return key;
    }

    public boolean isPartOf(@NotNull Set<String> properties) {
        return properties.containsAll(members);
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

package com.evolveum.midpoint.model.impl.mining.analysis;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AttributePathResult implements Serializable {

    Map<String, Integer> frequencyMap;
    Set<String> targetIdentifierValue = new HashSet<>();
    int totalRelation = 0;
    int maximumFrequency = 0;
    boolean isMultiValue;
    public AttributePathResult(Map<String, Integer> frequencyMap, int totalRelation) {
        this.frequencyMap = frequencyMap;
        this.totalRelation = totalRelation;
    }

    public Map<String, Integer> getFrequencyMap() {
        return frequencyMap;
    }

    public int getTotalRelation() {
        return totalRelation;
    }

    public void setFrequencyMap(Map<String, Integer> frequencyMap) {
        this.frequencyMap = frequencyMap;
    }

    public void setTotalRelation(int totalRelation) {
        this.totalRelation = totalRelation;
    }

    public void incrementFrequency(String key) {
        Integer put = frequencyMap.put(key, frequencyMap.getOrDefault(key, 0) + 1);
        if (put != null) {
            maximumFrequency = Math.max(maximumFrequency, put);
        }
    }

    public void putIdentifier(String value) {
        targetIdentifierValue.add(value);
    }

    public void incrementTotalRelation() {
        totalRelation++;
    }

    public int getMaximumFrequency() {
        return maximumFrequency;
    }

    public void setMaximumFrequency(int maximumFrequency) {
        this.maximumFrequency = maximumFrequency;
    }

    public boolean isMultiValue() {
        return isMultiValue;
    }

    public void setMultiValue(boolean multiValue) {
        isMultiValue = multiValue;
    }

    public Set<String> getTargetIdentifierValue() {
        return targetIdentifierValue;
    }

    public void setTargetIdentifierValue(Set<String> targetIdentifierPaths) {
        this.targetIdentifierValue = targetIdentifierPaths;
    }
}

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a role analysis cluster of data points of a specific type.
 */
public class Cluster<T extends Clusterable> implements Serializable {

    private final List<T> points = new ArrayList<>();
    Set<ClusterExplanation> explanations;

    public Cluster() {
    }

    public void addPoint(T point) {
        this.points.add(point);
    }

    public List<T> getPoints() {
        return this.points;
    }

    public Set<ClusterExplanation> getExplanations() {
        return explanations;
    }

    public void setExplanations(Set<ClusterExplanation> explanations) {
        this.explanations = explanations;
    }

    public int getMembersCount() {
        int memberCount = 0;
        for (T point : points) {
            memberCount += point.getMembersCount();
        }

        return memberCount;
    }

}

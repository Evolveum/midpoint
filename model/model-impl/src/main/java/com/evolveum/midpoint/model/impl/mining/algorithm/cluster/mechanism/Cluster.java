package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a role analysis cluster of data points of a specific type.
 */
public class Cluster<T extends Clusterable> implements Serializable {

    private final List<T> points = new ArrayList<>();

    public Cluster() {
    }

    public void addPoint(T point) {
        this.points.add(point);
    }

    public List<T> getPoints() {
        return this.points;
    }
}

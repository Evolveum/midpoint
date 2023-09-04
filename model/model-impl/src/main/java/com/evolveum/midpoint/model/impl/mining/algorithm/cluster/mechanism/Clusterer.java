package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;

import java.util.Collection;
import java.util.List;



public abstract class Clusterer<T extends Clusterable> {
    private final DistanceMeasure measure;

    protected Clusterer(DistanceMeasure measure) {
        this.measure = measure;
    }

    public abstract List<? extends Cluster<T>> cluster(Collection<T> var1, RoleAnalysisProgressIncrement handler);

    protected double distance(Clusterable p1, Clusterable p2) {
        return this.measure.compute(p1.getPoint(), p2.getPoint());
    }
}

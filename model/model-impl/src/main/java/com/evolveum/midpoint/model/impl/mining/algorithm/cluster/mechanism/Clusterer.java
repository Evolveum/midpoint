package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.ExtensionProperties;

import org.jetbrains.annotations.NotNull;

/**
 * An abstract base class for role analysis clustering data points of a specific type using a distance measure.
 * Subclasses are responsible for implementing the actual clustering logic for a given data type.
 */
public abstract class Clusterer<T extends Clusterable> {
    private final DistanceMeasure measure;

    protected Clusterer(DistanceMeasure measure) {
        this.measure = measure;
    }

    public abstract List<? extends Cluster<T>> cluster(Collection<T> var1, RoleAnalysisProgressIncrement handler);

    protected double accessDistance(
            @NotNull Clusterable p1,
            @NotNull Clusterable p2) {
        return this.measure.compute(p1.getPoint(), p2.getPoint());
    }

    protected boolean outlierAccessDistance(
            @NotNull Clusterable p1,
            @NotNull Clusterable p2,
            double minSimilarity,
            double maxOffset) {
        double compute = this.measure.compute(p1.getPoint(), p2.getPoint());

        //TODO
        if (compute < minSimilarity) {
            return true;
        } else if (compute < maxOffset) {
            p1.addCloseNeighbor(p2.getPoint().toString());
            p2.addCloseNeighbor(p1.getPoint().toString());
            return false;
        }

        return false;
    }

    protected double rulesDistance(
            @NotNull ExtensionProperties p1,
            @NotNull ExtensionProperties p2,
            @NotNull Set<ClusterExplanation> explanation) {
        return this.measure.compute(p1, p2, explanation);
    }

}

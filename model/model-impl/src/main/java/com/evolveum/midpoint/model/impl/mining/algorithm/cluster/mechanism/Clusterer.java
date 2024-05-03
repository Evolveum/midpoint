package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.util.ArrayList;
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
    private final ClusteringMode clusteringMode;

    protected Clusterer(@NotNull DistanceMeasure measure, @NotNull ClusteringMode clusteringMode) {
        this.measure = measure;
        this.clusteringMode = clusteringMode;
    }

    public abstract List<? extends Cluster<T>> cluster(Collection<T> var1, RoleAnalysisProgressIncrement handler);

    public List<T> getNeighbors(T point, Collection<T> points, Set<ClusterExplanation> explanation, double eps) {
        List<T> neighbors = new ArrayList<>();

        return switch (clusteringMode) {
            case BALANCED -> {
                for (T neighbor : points) {
                    if (point != neighbor && this.balancedAccessDistance(neighbor, point) <= eps) {
                        neighbors.add(neighbor);
                    }
                }
                yield neighbors;
            }
            case UNBALANCED -> {
                for (T neighbor : points) {
                    if (point != neighbor && this.unbalancedAccessDistance(neighbor, point) <= eps) {
                        neighbors.add(neighbor);
                    }
                }
                yield neighbors;
            }
            case BALANCED_RULES -> {
                for (T neighbor : points) {
                    if (point != neighbor && this.balancedAccessDistance(neighbor, point) <= eps
                            && this.rulesDistance(
                            neighbor.getExtensionProperties(),
                            point.getExtensionProperties(),
                            explanation) == 0) {
                        neighbors.add(neighbor);
                    }
                }
                yield neighbors;
            }
            case UNBALANCED_RULES -> {
                for (T neighbor : points) {
                    if (point != neighbor && this.unbalancedAccessDistance(neighbor, point) <= eps
                            && this.rulesDistance(
                            neighbor.getExtensionProperties(),
                            point.getExtensionProperties(),
                            explanation) == 0) {
                        neighbors.add(neighbor);
                    }
                }
                yield neighbors;
            }
        };
    }

    protected double unbalancedAccessDistance(
            @NotNull Clusterable p1,
            @NotNull Clusterable p2) {
        return this.measure.computeSimpleDistance(p1.getPoint(), p2.getPoint());
    }

    protected double balancedAccessDistance(
            @NotNull Clusterable p1,
            @NotNull Clusterable p2) {
        return this.measure.computeBalancedDistance(p1.getPoint(), p2.getPoint());
    }

    protected double rulesDistance(
            @NotNull ExtensionProperties p1,
            @NotNull ExtensionProperties p2,
            @NotNull Set<ClusterExplanation> explanation) {
        return this.measure.computeRuleDistance(p1, p2, explanation);
    }

    protected boolean outlierAccessDistance(
            @NotNull Clusterable p1,
            @NotNull Clusterable p2,
            double minSimilarity,
            double maxOffset) {
        double compute = this.measure.computeBalancedDistance(p1.getPoint(), p2.getPoint());

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
}

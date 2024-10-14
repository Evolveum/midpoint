package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.util.*;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OutlierNoiseCategoryType;

import org.jetbrains.annotations.NotNull;

/**
 * Performs density-based clustering of data points based on specified parameters and distance measure.
 * This class implements the Density-Based Spatial Clustering of Applications with Noise (DBSCAN) algorithm.
 */
public class DensityBasedClustering<T extends Clusterable> extends Clusterer<T> {
    private double eps;
    private int minPts;
    int minPropertiesOverlap;
    private static final Trace LOGGER = TraceManager.getTrace(DensityBasedClustering.class);

    /**
     * Constructs a DensityBasedClustering instance with the specified parameters.
     *
     * @param eps The epsilon parameter for distance-based clustering.
     * @param minPts The minimum number of points required to form a dense cluster.
     * @param measure The distance measure for clustering.
     * @param minRolesOverlap The minimum properties overlap required for adding a point to a cluster.
     */
    public DensityBasedClustering(double eps, int minPts, @NotNull DistanceMeasure measure, int minRolesOverlap, @NotNull ClusteringMode clusteringMode) {
        super(measure, clusteringMode);

        if (eps < 0.0) {
            LOGGER.warn("Invalid parameter: eps={} is less than 0.0. Parameters not updated.", eps);
        } else if (eps > 1.0) {
            LOGGER.warn("Invalid parameter: eps={} is greater than 1.0. Parameters not updated.", eps);
        } else if ((minPts - 1) < 0) {
            LOGGER.warn("Invalid parameter: minPts={} results in minPts  being less than 1. Parameters not updated.", minPts);
        } else {
            this.eps = eps;
            this.minPts = (minPts - 1);
            LOGGER.debug("Updated parameters: eps={} and minPts={}. New values: eps={} and minPts={}.",
                    eps, minPts, this.eps, this.minPts);
        }

        this.minPropertiesOverlap = minRolesOverlap;
    }

    /**
     * Performs density-based clustering on the provided collection of data points.
     *
     * @param points The collection of data points to cluster.
     * @param handler The progress increment handler for tracking the execution progress.
     * @return A list of clusters containing the clustered data points.
     */
    public List<Cluster<T>> cluster(Collection<T> points, RoleAnalysisProgressIncrement handler) {
        List<Cluster<T>> clusters = new ArrayList<>();
        Map<Clusterable, OutlierNoiseCategoryType> visited = new HashMap<>();

        Set<ClusterExplanation> explanation = new HashSet<>();

        handler.setActive(true);
        handler.enterNewStep("Clustering");
        handler.setOperationCountToProcess(points.size());
        for (T point : points) {
            handler.iterateActualStatus();

            if (visited.get(point) == null) {
                PointStatusWrapper pStatusWrapper = new PointStatusWrapper(null);

                List<T> neighbors = this.getNeighbors(point, points, explanation, this.eps, this.minPts, this.minPropertiesOverlap, pStatusWrapper);

                if (pStatusWrapper.pStatus == OutlierNoiseCategoryType.SUITABLE) {
                    Cluster<T> cluster = new Cluster<>();
                    Cluster<T> tCluster = this.expandCluster(cluster, point, neighbors, points, visited, explanation);
                    tCluster.setExplanations(explanation);
                    clusters.add(tCluster);
                } else {
                    point.setPointStatus(pStatusWrapper.pStatus);
                    visited.put(point, pStatusWrapper.pStatus);
                }

                explanation = new HashSet<>();

//                int neighborsSize = getNeightborsSize(neighbors);
//                if (neighborsSize >= this.minPts
//                        || (point.getMembersCount() >= this.minPts && point.getPoint().size() >= minPropertiesOverlap)) {
//                    Cluster<T> cluster = new Cluster<>();
//                    Cluster<T> tCluster = this.expandCluster(cluster, point, neighbors, points, visited, explanation);
//                    tCluster.setExplanations(explanation);
//                    clusters.add(tCluster);
//                    explanation = new HashSet<>();
//                } else {
//                    visited.put(point, pStatusWrapper.pStatus);
//                    explanation = new HashSet<>();
//                }
            }
        }

        return clusters;
    }

    private Cluster<T> expandCluster(Cluster<T> cluster, T point, List<T> neighbors, Collection<T> points,
            Map<Clusterable, OutlierNoiseCategoryType> visited, Set<ClusterExplanation> explanation) {
        cluster.addPoint(point);
        visited.put(point, OutlierNoiseCategoryType.SUITABLE);
        List<T> seeds = new ArrayList<>(neighbors);

        for (int index = 0; index < seeds.size(); ++index) {
            T current = (T) ((List) seeds).get(index);
            OutlierNoiseCategoryType pStatus = visited.get(current);
            if (pStatus == null) {
                PointStatusWrapper pStatusWrapper = new PointStatusWrapper(null);
                List<T> currentNeighbors = this.getNeighbors(current, points, explanation,
                        this.eps, this.minPts, this.minPropertiesOverlap,
                        pStatusWrapper);
                int currentNeighborsCount = getNeightborsSize(currentNeighbors);
                if (currentNeighborsCount >= this.minPts) {
                    this.merge(seeds, currentNeighbors);
                }
            }

            if (pStatus != OutlierNoiseCategoryType.SUITABLE) {
                visited.put(current, OutlierNoiseCategoryType.SUITABLE);
                cluster.addPoint(current);
            }
        }

        return cluster;
    }

    private int getNeightborsSize(List<T> neighbors) {
        int count = 0;
        for (T neighbor : neighbors) {
            count += neighbor.getMembersCount();
        }
        return count;
    }

    private void merge(List<T> one, List<T> two) {
        Set<T> oneSet = new HashSet<>(one);

        for (T item : two) {
            if (!oneSet.contains(item)) {
                one.add(item);
            }
        }

    }

    static class PointStatusWrapper {
        public OutlierNoiseCategoryType pStatus;

        public PointStatusWrapper(OutlierNoiseCategoryType pStatus) {
            this.pStatus = pStatus;
        }
    }
}

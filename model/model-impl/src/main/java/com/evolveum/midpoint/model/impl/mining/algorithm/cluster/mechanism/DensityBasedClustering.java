package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import com.evolveum.midpoint.common.mining.objects.handler.Handler;

import java.util.*;

import static com.evolveum.midpoint.model.api.expr.MidpointFunctions.LOGGER;

public class DensityBasedClustering<T extends Clusterable> extends Clusterer<T> {
    private double eps;
    private int minPts;

    public DensityBasedClustering(double eps, int minPts, DistanceMeasure measure) {
        super(measure);

        if (eps < 0.0) {
            LOGGER.warn("Invalid parameter: eps={} is less than 0.0. Parameters not updated.", eps);
        } else if (eps > 1.0) {
            LOGGER.warn("Invalid parameter: eps={} is greater than 1.0. Parameters not updated.", eps);
        } else if ((minPts - 1) < 0) {
            LOGGER.warn("Invalid parameter: minPts={} results in minPts  being less than 1. Parameters not updated.", minPts);
        } else {
            this.eps = eps;
            this.minPts = (minPts - 1);
            LOGGER.info("Updated parameters: eps={} and minPts={}. New values: eps={} and minPts={}.",
                    eps, minPts, this.eps, this.minPts);
        }
    }

    public List<Cluster<T>> cluster(Collection<T> points, Handler handler) {
        List<Cluster<T>> clusters = new ArrayList<>();
        Map<Clusterable, PointStatus> visited = new HashMap<>();

        handler.setActive(true);
        handler.setSubTitle("Clustering");
        handler.setOperationCountToProcess(points.size());
        for (T point : points) {
            handler.iterateActualStatus();

            if (visited.get(point) == null) {
                List<T> neighbors = this.getNeighbors(point, points);
                int neighborsSize = getNeightborsSize(neighbors);
                if (neighborsSize >= this.minPts || point.getMembersCount() >= this.minPts) {
                    Cluster<T> cluster = new Cluster<>();
                    clusters.add(this.expandCluster(cluster, point, neighbors, points, visited));
                } else {
                    visited.put(point, PointStatus.NOISE);
                }
            }
        }

        return clusters;
    }

    private Cluster<T> expandCluster(Cluster<T> cluster, T point, List<T> neighbors, Collection<T> points,
            Map<Clusterable, PointStatus> visited) {
        cluster.addPoint(point);
        visited.put(point, PointStatus.PART_OF_CLUSTER);
        List<T> seeds = new ArrayList<>(neighbors);

        for (int index = 0; index < seeds.size(); ++index) {
            T current = (T) ((List) seeds).get(index);
            PointStatus pStatus = visited.get(current);
            if (pStatus == null) {
                List<T> currentNeighbors = this.getNeighbors(current, points);
                int currentNeighborsCount = getNeightborsSize(currentNeighbors);
                if (currentNeighborsCount >= this.minPts) {
                    this.merge(seeds, currentNeighbors);
                }
            }

            if (pStatus != PointStatus.PART_OF_CLUSTER) {
                visited.put(current, PointStatus.PART_OF_CLUSTER);
                cluster.addPoint(current);
            }
        }

        return cluster;
    }

    private List<T> getNeighbors(T point, Collection<T> points) {
        List<T> neighbors = new ArrayList<>();

        for (T neighbor : points) {
            if (point != neighbor && this.distance(neighbor, point) <= this.eps) {
                neighbors.add(neighbor);
            }
        }

        return neighbors;
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

    private enum PointStatus {
        NOISE,
        PART_OF_CLUSTER;

        PointStatus() {
        }
    }
}

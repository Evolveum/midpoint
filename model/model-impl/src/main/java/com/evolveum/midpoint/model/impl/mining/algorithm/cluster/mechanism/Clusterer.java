package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.ExtensionProperties;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OutlierNoiseCategoryType;

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

    //TODO make better structure (high cognitive complexity)
    @SuppressWarnings({ "ClassEscapesDefinedScope" })
    public List<T> getNeighbors(@NotNull T point, Collection<T> points, Set<ClusterExplanation> explanation, double eps, int minPts,
            int minPropertiesOverlap, DensityBasedClustering.PointStatusWrapper pStatusWrapper) {
        List<T> neighbors = new ArrayList<>();

        return switch (clusteringMode) {
            case BALANCED -> {
                boolean conditionMeets = checkPropertiesRequirement(point, minPropertiesOverlap, pStatusWrapper);
                if (!conditionMeets) {yield neighbors;}

                int numberOfOveralRuleNeighbors = point.getMembersCount();
                for (T neighbor : points) {
                    boolean notNeighbor = point != neighbor;
                    boolean accessDistance = this.balancedAccessDistance(neighbor, point) <= eps;

                    if (notNeighbor && accessDistance) {
                        neighbors.add(neighbor);
                        numberOfOveralRuleNeighbors += neighbor.getMembersCount();
                    }
                }

                if (numberOfOveralRuleNeighbors > minPts) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.SUITABLE;
                } else {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_NOISE;
                }

                yield neighbors;
            }
            case UNBALANCED -> {
                boolean conditionMeets = checkPropertiesRequirement(point, minPropertiesOverlap, pStatusWrapper);
                if (!conditionMeets) {yield neighbors;}

                int numberOfOveralRuleNeighbors = point.getMembersCount();
                for (T neighbor : points) {
                    boolean notNeighbor = point != neighbor;
                    boolean accessDistance = this.unbalancedAccessDistance(neighbor, point) <= eps;

                    if (notNeighbor && accessDistance) {
                        neighbors.add(neighbor);
                        numberOfOveralRuleNeighbors += neighbor.getMembersCount();
                    }
                }

                if (numberOfOveralRuleNeighbors > minPts) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.SUITABLE;
                } else {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_NOISE;
                }

                yield neighbors;
            }
            case BALANCED_RULES -> {
                int numberOfOveralRuleNeighbors = point.getMembersCount();
                int numberOfAccessNeighbors = point.getMembersCount();
                int numberOfRulesNeighbors = point.getMembersCount();
                for (T neighbor : points) {
                    boolean notNeighbor = point != neighbor;
                    boolean accessDistance = this.balancedAccessDistance(neighbor, point) <= eps;
                    boolean rulesDistance = this.rulesDistance(
                            neighbor.getExtensionProperties(),
                            point.getExtensionProperties(),
                            explanation) == 0;

                    if (notNeighbor && accessDistance && rulesDistance) {
                        neighbors.add(neighbor);
                        numberOfOveralRuleNeighbors += neighbor.getMembersCount();
                    }

                    if (accessDistance) {
                        numberOfAccessNeighbors += neighbor.getMembersCount();
                    }

                    if (rulesDistance) {
                        numberOfRulesNeighbors += neighbor.getMembersCount();
                    }

                }

                boolean conditionMeets = checkPropertiesRequirementIncludeRule(point, minPropertiesOverlap,
                        numberOfRulesNeighbors, minPts, pStatusWrapper);
                if (!conditionMeets) {yield neighbors;}

                OutlierNoiseCategoryType pStatus = pStatusWrapper.pStatus;
                if (numberOfOveralRuleNeighbors > minPts) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.SUITABLE;
                } else if (numberOfAccessNeighbors < minPts && numberOfRulesNeighbors < minPts && pStatus == null) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.OVERAL_NOISE;
                } else if (pStatus == OutlierNoiseCategoryType.ACCESS_NOISE
                        || pStatus == OutlierNoiseCategoryType.RULE_NOISE
                        || pStatus == OutlierNoiseCategoryType.ACCESS_OR_RULE_NOISE) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_OR_RULE_NOISE;
                } else if (numberOfAccessNeighbors < minPts) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_NOISE;
                } else if (numberOfRulesNeighbors < minPts) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.RULE_NOISE;
                } else {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_OR_RULE_NOISE;
                }

                yield neighbors;
            }
            case UNBALANCED_RULES -> {
                int numberOfOveralRuleNeighbors = point.getMembersCount();
                int numberOfAccessNeighbors = point.getMembersCount();
                int numberOfRulesNeighbors = point.getMembersCount();
                for (T neighbor : points) {
                    boolean notNeighbor = point != neighbor;
                    boolean accessDistance = this.unbalancedAccessDistance(neighbor, point) <= eps;
                    boolean rulesDistance = this.rulesDistance(
                            neighbor.getExtensionProperties(),
                            point.getExtensionProperties(),
                            explanation) == 0;

                    if (notNeighbor && accessDistance && rulesDistance) {
                        neighbors.add(neighbor);
                        numberOfOveralRuleNeighbors += neighbor.getMembersCount();
                    }

                    if (accessDistance) {
                        numberOfAccessNeighbors += neighbor.getMembersCount();
                    }

                    if (rulesDistance) {
                        numberOfRulesNeighbors += neighbor.getMembersCount();
                    }
                }

                boolean conditionMeets = checkPropertiesRequirementIncludeRule(point, minPropertiesOverlap,
                        numberOfRulesNeighbors, minPts, pStatusWrapper);
                if (!conditionMeets) {yield neighbors;}

                OutlierNoiseCategoryType pStatus = pStatusWrapper.pStatus;
                if (numberOfOveralRuleNeighbors > minPts) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.SUITABLE;
                } else if (numberOfAccessNeighbors < minPts && numberOfRulesNeighbors < minPts && pStatus == null) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.OVERAL_NOISE;
                } else if (pStatus == OutlierNoiseCategoryType.ACCESS_NOISE
                        || pStatus == OutlierNoiseCategoryType.RULE_NOISE
                        || pStatus == OutlierNoiseCategoryType.ACCESS_OR_RULE_NOISE) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_OR_RULE_NOISE;
                } else if (numberOfAccessNeighbors < minPts) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_NOISE;
                } else if (numberOfRulesNeighbors < minPts) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.RULE_NOISE;
                } else {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_OR_RULE_NOISE;
                }

                yield neighbors;
            }
            case BALANCED_RULES_OUTLIER -> {

                int numberOfOveralRuleNeighbors = point.getMembersCount();
                int numberOfAccessNeighbors = point.getMembersCount();
                int numberOfRulesNeighbors = point.getMembersCount();

                for (T neighbor : points) {
                    ExtensionProperties neighborExtensionProperties = neighbor.getExtensionProperties();
                    ExtensionProperties pointExtensionProperties = point.getExtensionProperties();

                    boolean notNeighbor = point != neighbor;
                    boolean accessDistance = this.balancedAccessDistance(neighbor, point) <= eps;
                    boolean rulesDistance = this.rulesDistance(
                            neighborExtensionProperties,
                            pointExtensionProperties,
                            explanation) == 0;

                    if (accessDistance) {
                        numberOfAccessNeighbors += neighbor.getMembersCount();
                    }

                    if (rulesDistance) {
                        numberOfRulesNeighbors += neighbor.getMembersCount();
                    }

                    if (notNeighbor && accessDistance && rulesDistance) {
                        neighbors.add(neighbor);
                        numberOfOveralRuleNeighbors += neighbor.getMembersCount();
                    }
                }

                boolean conditionMeets = checkPropertiesRequirementIncludeRule(point, minPropertiesOverlap,
                        numberOfRulesNeighbors, minPts, pStatusWrapper);
                if (!conditionMeets) {yield neighbors;}

                OutlierNoiseCategoryType pStatus = pStatusWrapper.pStatus;
                if (numberOfOveralRuleNeighbors > minPts) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.SUITABLE;
                } else if (numberOfAccessNeighbors < minPts && numberOfRulesNeighbors < minPts && pStatus == null) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.OVERAL_NOISE;
                } else if (pStatus == OutlierNoiseCategoryType.ACCESS_NOISE
                        || pStatus == OutlierNoiseCategoryType.RULE_NOISE
                        || pStatus == OutlierNoiseCategoryType.ACCESS_OR_RULE_NOISE) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_OR_RULE_NOISE;
                } else if (numberOfRulesNeighbors < minPts) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.RULE_NOISE;
                } else if (numberOfAccessNeighbors < minPts) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_NOISE;
                } else if (numberOfAccessNeighbors > minPts && numberOfRulesNeighbors > minPts) {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_OR_RULE_NOISE;
                } else {
                    pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_OR_RULE_NOISE;
                }

                yield neighbors;
            }
        };
    }

    private static <T extends Clusterable> boolean checkPropertiesRequirement(@NotNull T point,
            int minPropertiesOverlap,
            DensityBasedClustering.PointStatusWrapper pStatusWrapper) {
        // Retrieve the properties of the given point
        Set<String> pointProperties = point.getPoint();
 /*        Check if the number of properties is less than the minimum required overlap
         If the number of properties is less than the minimum required overlap,
         set the point status to ACCESS_NOISE and return an empty list of neighbors
         because the point is not suitable for minIntersection
         conditions (there are not enough properties to meet the minimum overlap).*/
        if (pointProperties.size() < minPropertiesOverlap) {
            pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_NOISE;
            return false;
        }
        return true;
    }

    private static <T extends Clusterable> boolean checkPropertiesRequirementIncludeRule(@NotNull T point,
            int minPropertiesOverlap,
            int numberOfRulesNeighbors,
            int minPts,
            DensityBasedClustering.PointStatusWrapper pStatusWrapper) {
        boolean propertiesCountMeet = checkPropertiesRequirement(point, minPropertiesOverlap, pStatusWrapper);
        if (!propertiesCountMeet && numberOfRulesNeighbors < minPts) {
            pStatusWrapper.pStatus = OutlierNoiseCategoryType.OVERAL_NOISE;
            return false;
        } else if (!propertiesCountMeet) {
            pStatusWrapper.pStatus = OutlierNoiseCategoryType.ACCESS_NOISE;
            return false;
        }
        return true;
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

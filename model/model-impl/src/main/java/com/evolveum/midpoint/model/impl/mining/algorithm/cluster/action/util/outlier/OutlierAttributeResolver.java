package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatistics;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import java.util.List;
import java.util.Objects;

/**
 * Responsibility of an attribute resolver is to decide whether user attribute value is unusual comparing to the similar users (in group stats).
 * Algorithm:
 * 1. find the most common (mode) value of an attribute
 * 2. use its frequency within group as a baseline (100%) to compute relative frequencies
 * 3. find out relative frequency of user's attribute value
 * 4. if it is below threshold, mark it as unusual (confidence 1)
 * Limitations:
 * - it does not support missing/undefined user attributes
 */
public class OutlierAttributeResolver {

    private final double minRelativeFrequencyThreshold;

    public OutlierAttributeResolver(double minRelativeFrequencyThreshold) {
        this.minRelativeFrequencyThreshold = minRelativeFrequencyThreshold;
    }

    public record UnusualSingleValueDetail(String value, double confidence, double relativeFrequency) {}
    public record UnusualAttributeValueConfidence(ItemPathType path, double confidence, List<UnusualSingleValueDetail> partialConfidences) {}

    private RoleAnalysisAttributeStatistics findMedianAttributeValueStats(List<RoleAnalysisAttributeStatistics> stats) {
        var usersWithAttributeCount = stats.stream().mapToInt(RoleAnalysisAttributeStatistics::getInGroup).sum();
        var halfUsersStack = usersWithAttributeCount / 2;
        var sortedDescStats = stats.stream().sorted((a, b) -> a.getInGroup().compareTo(b.getInGroup())).toList();
        for (var stat: sortedDescStats) {
            halfUsersStack -= stat.getInGroup();
            if (halfUsersStack <= 0) {
                return stat;
            }
        }
        throw new RuntimeException("Invariant violation: there always have to be median value");
    }

    private UnusualSingleValueDetail analyzeAttributeValue(RoleAnalysisAttributeAnalysis clusterAttributeDetail, String userAttributeValue ) {
        var allStats = clusterAttributeDetail.getAttributeStatistics();
        var medianValueStats = findMedianAttributeValueStats(allStats);
        var userValueInGroup = allStats.stream()
                .filter(s -> s.getAttributeValue().equals(userAttributeValue))
                .map(a -> a.getInGroup().doubleValue())
                .findFirst()
                .orElse(0d);
        var medianValueInGroup = medianValueStats.getInGroup().doubleValue();
        var relativeUserValueFrequency = userValueInGroup / medianValueInGroup;
        var confidence = relativeUserValueFrequency <= minRelativeFrequencyThreshold ? 1 : 0;
        return new UnusualSingleValueDetail(userAttributeValue, confidence, relativeUserValueFrequency);
    }

    private UnusualAttributeValueConfidence analyzeAttribute(RoleAnalysisAttributeAnalysis clusterAttributeDetail, RoleAnalysisAttributeAnalysis userAttributeDetail ) {
        var userValues = userAttributeDetail.getAttributeStatistics().stream()
                .map(RoleAnalysisAttributeStatistics::getAttributeValue)
                .toList();

        var partialConfidences = userValues.stream()
                .map(userValue -> analyzeAttributeValue(clusterAttributeDetail, userValue))
                .toList();

        var confidence = partialConfidences.stream()
                .mapToDouble(UnusualSingleValueDetail::confidence)
                .max()
                .orElse(0d);
        return new UnusualAttributeValueConfidence(clusterAttributeDetail.getItemPath(), confidence, partialConfidences);
    }

    public List<UnusualAttributeValueConfidence> resolveUnusualAttributes(
            List<RoleAnalysisAttributeAnalysis> clusterAttributeDetails,
            List<RoleAnalysisAttributeAnalysis> userAttributeDetails
    ) {
        return clusterAttributeDetails.stream()
                .map(clusterAttributeDetail -> {
                    var path = clusterAttributeDetail.getItemPath();
                    var userAttributeDetail = userAttributeDetails.stream()
                            .filter(a -> a.getItemPath().equals(path))
                            .findFirst()
                            .orElse(null);

                    if (clusterAttributeDetail.getAttributeStatistics().isEmpty()) {
                        // invariant violation: defensive, but should not happen
                        return null;
                    }

                    if (userAttributeDetail == null) {
                        // NOTE: limitation, doesn't support missing attributes
                        return null;
                    }

                    return analyzeAttribute(clusterAttributeDetail, userAttributeDetail);
                })
                .filter(Objects::nonNull)
                .toList();
    }

}

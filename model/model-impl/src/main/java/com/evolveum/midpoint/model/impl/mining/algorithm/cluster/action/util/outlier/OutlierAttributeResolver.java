package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatistics;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import java.util.List;
import java.util.Objects;

/**
 * Responsibility of an attribute resolver is to decide whether user attribute value is unusual comparing to the provided users (in repo stats).
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
        var usersWithAttributeCount = stats.stream().mapToInt(RoleAnalysisAttributeStatistics::getInRepo).sum();
        var halfUsersStack = usersWithAttributeCount / 2;
        var sortedDescStats = stats.stream().sorted((a, b) -> a.getInRepo().compareTo(b.getInRepo())).toList();
        for (var stat: sortedDescStats) {
            halfUsersStack -= stat.getInRepo();
            if (halfUsersStack <= 0) {
                return stat;
            }
        }
        throw new RuntimeException("Invariant violation: there always have to be median value");
    }

    private UnusualSingleValueDetail analyzeAttributeValue(RoleAnalysisAttributeAnalysis attributeDetail, String userAttributeValue ) {
        var allStats = attributeDetail.getAttributeStatistics();
        var medianValueStats = findMedianAttributeValueStats(allStats);
        var userValueInRepo = allStats.stream()
                .filter(s -> s.getAttributeValue().equals(userAttributeValue))
                .map(a -> a.getInRepo().doubleValue())
                .findFirst()
                .orElse(0d);
        var medianValueInRepo = medianValueStats.getInRepo().doubleValue();
        var relativeUserValueFrequency = userValueInRepo / medianValueInRepo;
        var confidence = relativeUserValueFrequency <= minRelativeFrequencyThreshold ? 1 : 0;
        return new UnusualSingleValueDetail(userAttributeValue, confidence, relativeUserValueFrequency);
    }

    private UnusualAttributeValueConfidence analyzeAttribute(RoleAnalysisAttributeAnalysis attributeDetail, RoleAnalysisAttributeAnalysis userAttributeDetail ) {
        var userValues = userAttributeDetail.getAttributeStatistics().stream()
                .map(RoleAnalysisAttributeStatistics::getAttributeValue)
                .toList();

        var partialConfidences = userValues.stream()
                .map(userValue -> analyzeAttributeValue(attributeDetail, userValue))
                .toList();

        var confidence = partialConfidences.stream()
                .mapToDouble(UnusualSingleValueDetail::confidence)
                .max()
                .orElse(0d);
        return new UnusualAttributeValueConfidence(attributeDetail.getItemPath(), confidence, partialConfidences);
    }

    public List<UnusualAttributeValueConfidence> resolveUnusualAttributes(
            List<RoleAnalysisAttributeAnalysis> attributeDetails,
            List<RoleAnalysisAttributeAnalysis> userAttributeDetails
    ) {
        return attributeDetails.stream()
                .map(attributeDetail -> {
                    var path = attributeDetail.getItemPath();
                    var userAttributeDetail = userAttributeDetails.stream()
                            .filter(a -> a.getItemPath().equals(path))
                            .findFirst()
                            .orElse(null);

                    if (attributeDetail.getAttributeStatistics().isEmpty()) {
                        // invariant violation: defensive, but should not happen
                        return null;
                    }

                    if (userAttributeDetail == null) {
                        // NOTE: limitation, doesn't support missing attributes
                        return null;
                    }

                    return analyzeAttribute(attributeDetail, userAttributeDetail);
                })
                .filter(Objects::nonNull)
                .toList();
    }

}

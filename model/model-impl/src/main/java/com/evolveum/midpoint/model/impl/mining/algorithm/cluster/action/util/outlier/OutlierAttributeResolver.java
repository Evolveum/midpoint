package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatisticsType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import java.util.List;
import java.util.Objects;

/**
 * Responsibility of an attribute resolver is to decide whether user attribute value is unusual comparing to the provided users (attribute members - in group stats).
 * Algorithm:
 * 1. find the most common (mode) value of an attribute
 * 2. use its frequency within group as a baseline (100%) to compute relative frequencies
 * 3. find out relative frequency of user's attribute value
 * 4. if it is below threshold, mark it as unusual
 * Limitations:
 * - it does not support missing/undefined user attributes
 */
public class OutlierAttributeResolver {

    private final double minRelativeFrequencyThreshold;

    public OutlierAttributeResolver(double minRelativeFrequencyThreshold) {
        this.minRelativeFrequencyThreshold = minRelativeFrequencyThreshold;
    }

    public record UnusualSingleValueDetail(String value, boolean isUnusual, double relativeFrequency) {}
    public record UnusualAttributeValueResult(ItemPathType path, boolean isUnusual, List<UnusualSingleValueDetail> partialResults) {}

    private RoleAnalysisAttributeStatisticsType findMedianAttributeValueStats(List<RoleAnalysisAttributeStatisticsType> stats) {
        var usersWithAttributeCount = stats.stream().mapToInt(RoleAnalysisAttributeStatisticsType::getInGroup).sum();
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

    private UnusualSingleValueDetail analyzeAttributeValue(RoleAnalysisAttributeAnalysisType attributeDetail, String userAttributeValue ) {
        var allStats = attributeDetail.getAttributeStatistics();
        var medianValueStats = findMedianAttributeValueStats(allStats);
        var userValueInGroup = allStats.stream()
                .filter(s -> s.getAttributeValue().equals(userAttributeValue))
                .map(a -> a.getInGroup().doubleValue())
                .findFirst()
                .orElse(0d);
        var medianValueInGroup = medianValueStats.getInGroup().doubleValue();
        var relativeUserValueFrequency = userValueInGroup / medianValueInGroup;
        var isUnusual = relativeUserValueFrequency <= minRelativeFrequencyThreshold;
        return new UnusualSingleValueDetail(userAttributeValue, isUnusual, relativeUserValueFrequency);
    }

    private UnusualAttributeValueResult analyzeAttribute(RoleAnalysisAttributeAnalysisType attributeDetail, RoleAnalysisAttributeAnalysisType userAttributeDetail ) {
        var userValues = userAttributeDetail.getAttributeStatistics().stream()
                .map(RoleAnalysisAttributeStatisticsType::getAttributeValue)
                .toList();

        var partialResults = userValues.stream()
                .map(userValue -> analyzeAttributeValue(attributeDetail, userValue))
                .toList();

        var isUnusual = partialResults.stream()
                .anyMatch(UnusualSingleValueDetail::isUnusual);
        return new UnusualAttributeValueResult(attributeDetail.getItemPath(), isUnusual, partialResults);
    }

    public List<UnusualAttributeValueResult> resolveUnusualAttributes(
            List<RoleAnalysisAttributeAnalysisType> attributeDetails,
            List<RoleAnalysisAttributeAnalysisType> userAttributeDetails
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

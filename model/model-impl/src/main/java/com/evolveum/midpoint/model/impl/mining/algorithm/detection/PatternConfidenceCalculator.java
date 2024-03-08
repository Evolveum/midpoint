package com.evolveum.midpoint.model.impl.mining.algorithm.detection;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionPatternType;

import java.util.List;

/**
 * Experimental class for calculating confidence values based on detection patterns. (Part of RoleAnalysis)
 * <p> This class calculates confidence values for reduction factor and item density.
 * <p> The confidence values are calculated based on the analysis results provided by detection patterns.
 */
public class PatternConfidenceCalculator {

    double itemsConfidence = 0.0;
    double reductionFactorConfidence = 0.0;

    RoleAnalysisDetectionPatternType pattern;

    double maxReduction;

    public PatternConfidenceCalculator(RoleAnalysisDetectionPatternType pattern, double maxReduction) {
        this.pattern = pattern;
        this.maxReduction = maxReduction;
    }

    public double calculateReductionFactorConfidence() {
        Double clusterMetric = pattern.getClusterMetric();

        if (clusterMetric == null) {
            return this.reductionFactorConfidence = 0.0;
        } else {
            return this.reductionFactorConfidence = (clusterMetric / maxReduction) * 100;
        }
    }

    public double calculateItemConfidence() {
        double averageDensity = 0.0;
        RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = pattern.getRoleAttributeAnalysisResult();
        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = pattern.getUserAttributeAnalysisResult();

        if (roleAttributeAnalysisResult == null || userAttributeAnalysisResult == null) {
            return this.itemsConfidence = 0.0;
        }

        List<RoleAnalysisAttributeAnalysis> roleAttributeAnalysis = roleAttributeAnalysisResult.getAttributeAnalysis();
        List<RoleAnalysisAttributeAnalysis> userAttributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();

        double totalDensity = 0.0;
        int totalCount = 0;

        for (RoleAnalysisAttributeAnalysis attributeAnalysis : roleAttributeAnalysis) {
            Double density = attributeAnalysis.getDensity();
            if (density != null) {
                totalDensity += density;
                totalCount++;
            }
        }

        for (RoleAnalysisAttributeAnalysis attributeAnalysis : userAttributeAnalysis) {
            Double density = attributeAnalysis.getDensity();
            if (density != null) {
                totalDensity += density;
                totalCount++;
            }
        }

        if (totalCount > 0) {
            averageDensity = totalDensity / totalCount;
        }

        return this.itemsConfidence = averageDensity;
    }

}

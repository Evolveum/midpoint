package com.evolveum.midpoint.model.impl.mining.algorithm.detection;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

/**
 * Experimental class for calculating confidence values based on detection patterns. (Part of RoleAnalysis)
 * <p> This class calculates confidence values for reduction factor and item density.
 * <p> The confidence values are calculated based on the analysis results provided by detection patterns.
 */
public class PatternConfidenceCalculator implements Serializable {

    protected double itemsConfidence = 0.0;
    protected double reductionFactorConfidence = 0.0;

    protected RoleAnalysisDetectionPatternType pattern;
    protected RoleAnalysisSessionType session;
    protected double maxReduction;
    protected int itemCount;

    public PatternConfidenceCalculator(
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisDetectionPatternType pattern,
            double maxReduction) {
        this.pattern = pattern;
        this.maxReduction = maxReduction;
        this.session = session;
        initializeItemCount();
    }

    private void initializeItemCount() {
        AbstractAnalysisSessionOptionType sessionOptions = getSessionOptions();
        AnalysisAttributeSettingType analysisAttributeSetting = getAnalysisAttributeSetting(sessionOptions);
        //TODO check (role attributes are missing)
        List<ItemPathType> analysisAttributeRule = getAnalysisAttributeRule(analysisAttributeSetting);
        itemCount = analysisAttributeRule == null ? 0 : analysisAttributeRule.size();
    }

    private AbstractAnalysisSessionOptionType getSessionOptions() {
        return session.getAnalysisOption().getProcessMode().equals(RoleAnalysisProcessModeType.USER) ?
                session.getUserModeOptions() : session.getRoleModeOptions();
    }

    private AnalysisAttributeSettingType getAnalysisAttributeSetting(AbstractAnalysisSessionOptionType sessionOptions) {
        if (sessionOptions == null || sessionOptions.getUserAnalysisAttributeSetting() == null) {
            return null;
        }
        return sessionOptions.getUserAnalysisAttributeSetting();
    }

    //TODO check (role attributes are missing)
    private List<ItemPathType> getAnalysisAttributeRule(
            @Nullable AnalysisAttributeSettingType analysisAttributeSetting) {
        if (analysisAttributeSetting == null) {
            return null;
        }
        return analysisAttributeSetting.getPath();
    }

    public double calculateReductionFactorConfidence() {
        Double clusterMetric = pattern.getClusterMetric();
        return reductionFactorConfidence = (clusterMetric == null ? 0.0 : (clusterMetric / maxReduction) * 100);
    }

    public double calculateItemConfidence() {
        double totalDensity = 0.0;
        int totalCount = 0;
        RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = pattern.getRoleAttributeAnalysisResult();
        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = pattern.getUserAttributeAnalysisResult();

        if (roleAttributeAnalysisResult != null) {
            totalDensity += calculateDensity(roleAttributeAnalysisResult.getAttributeAnalysis());
            totalCount += roleAttributeAnalysisResult.getAttributeAnalysis().size();
        }
        if (userAttributeAnalysisResult != null) {
            totalDensity += calculateDensity(userAttributeAnalysisResult.getAttributeAnalysis());
            totalCount += userAttributeAnalysisResult.getAttributeAnalysis().size();
        }
        //TODO check it. It is not clear how to calculate item confidence. In the item count missing role attributes.
//        double itemsConfidence = (totalCount > 0 && totalDensity > 0.0 && itemCount > 0) ? totalDensity / itemCount : 0.0;

        return (totalCount > 0 && totalDensity > 0.0 && itemCount > 0) ? totalDensity / totalCount : 0.0;
    }

    private double calculateDensity(@NotNull List<RoleAnalysisAttributeAnalysis> attributeAnalysisList) {
        double totalDensity = 0.0;
        for (RoleAnalysisAttributeAnalysis attributeAnalysis : attributeAnalysisList) {
            Double density = attributeAnalysis.getDensity();
            if (density != null) {
                totalDensity += density;
            }
        }
        return totalDensity;
    }
}

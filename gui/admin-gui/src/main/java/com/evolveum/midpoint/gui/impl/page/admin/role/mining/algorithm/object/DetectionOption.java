package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object;

import java.io.Serializable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSearchModeType;

public class DetectionOption implements Serializable {



    double minFrequencyThreshold;
    Integer minOccupancy;
    double maxFrequencyThreshold;
    Integer minPropertiesOverlap;
    RoleAnalysisSearchModeType searchMode;

    public double getMinFrequencyThreshold() {
        return minFrequencyThreshold;
    }

    public void setMinFrequencyThreshold(double minFrequencyThreshold) {
        this.minFrequencyThreshold = minFrequencyThreshold;
    }

    public Integer getMinOccupancy() {
        return minOccupancy;
    }

    public void setMinOccupancy(Integer minOccupancy) {
        this.minOccupancy = minOccupancy;
    }

    public double getMaxFrequencyThreshold() {
        return maxFrequencyThreshold;
    }

    public void setMaxFrequencyThreshold(double maxFrequencyThreshold) {
        this.maxFrequencyThreshold = maxFrequencyThreshold;
    }

    public Integer getMinPropertiesOverlap() {
        return minPropertiesOverlap;
    }

    public void setMinPropertiesOverlap(Integer minPropertiesOverlap) {
        this.minPropertiesOverlap = minPropertiesOverlap;
    }

    public RoleAnalysisSearchModeType getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(RoleAnalysisSearchModeType searchMode) {
        this.searchMode = searchMode;
    }

    public Double getJaccardSimilarityThreshold() {
        return jaccardSimilarityThreshold;
    }

    public void setJaccardSimilarityThreshold(Double jaccardSimilarityThreshold) {
        this.jaccardSimilarityThreshold = jaccardSimilarityThreshold;
    }

    Double jaccardSimilarityThreshold;

    public DetectionOption(double minFrequencyThreshold, double maxFrequencyThreshold, Integer minOccupancy, Integer minPropertiesOverlap, RoleAnalysisSearchModeType searchMode, Double jaccardSimilarityThreshold) {
        this.minFrequencyThreshold = minFrequencyThreshold;
        this.minOccupancy = minOccupancy;
        this.maxFrequencyThreshold = maxFrequencyThreshold;
        this.minPropertiesOverlap = minPropertiesOverlap;
        this.searchMode = searchMode;
        this.jaccardSimilarityThreshold = jaccardSimilarityThreshold;
    }
}

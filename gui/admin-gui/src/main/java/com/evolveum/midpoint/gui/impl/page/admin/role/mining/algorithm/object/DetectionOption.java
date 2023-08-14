package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object;

import java.io.Serializable;


public class DetectionOption implements Serializable {



    double minFrequencyThreshold;
    Integer minOccupancy;
    double maxFrequencyThreshold;
    Integer minPropertiesOverlap;

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




    public DetectionOption(double minFrequencyThreshold, double maxFrequencyThreshold, Integer minOccupancy, Integer minPropertiesOverlap) {
        this.minFrequencyThreshold = minFrequencyThreshold;
        this.minOccupancy = minOccupancy;
        this.maxFrequencyThreshold = maxFrequencyThreshold;
        this.minPropertiesOverlap = minPropertiesOverlap;
    }
}

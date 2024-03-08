package com.evolveum.midpoint.common.mining.objects.analysis;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;

import java.io.Serializable;

/**
 * Represents an attribute analysis structure.
 * Used for storing statistical data about the role analysis cluster.
 * Represent information about the attribute similarity and density.
 */
// TODO - this class is just fast experiment
public class AttributeAnalysisStructure implements Serializable {

    int uniqueValues;
    int totalValues;
    String itemPath;
    double density;
    String description;
    String jsonDescription;

    public AttributeAnalysisStructure(int uniqueValues, int objectCount, int totalValues, String itemPath) {
        this.uniqueValues = uniqueValues;
        this.totalValues = totalValues;
        int possibleRelations = uniqueValues * objectCount;
        this.density = calculateDensity(totalValues, possibleRelations);
        this.itemPath = itemPath;
    }

    public AttributeAnalysisStructure(double density, String itemPath) {
        this.density = density;
        this.itemPath = itemPath;
    }

    public AttributeAnalysisStructure(RoleAnalysisAttributeAnalysis attributeAnalysis) {
        if (attributeAnalysis == null) {
            return;
        }
        this.itemPath = attributeAnalysis.getItemPath();
        this.density = attributeAnalysis.getDensity();
        this.description = attributeAnalysis.getDescription();
    }

    public void addUniqueValues(int uniqueValues) {
        this.uniqueValues += uniqueValues;
    }

    public void addTotalValues(int totalValues) {
        this.totalValues += totalValues;
    }

    protected double calculateDensity(int relations, int possibleRelations) {
        if (relations == 0 || possibleRelations == 0) {
            return 0;
        }
        return Math.min((relations / (double) possibleRelations) * 100, 100);
    }

    public int getUniqueValues() {
        return uniqueValues;
    }

    public int getTotalValues() {
        return totalValues;
    }

    public String getItemPath() {
        return itemPath;
    }

    public double getDensity() {
        return density;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJsonDescription() {
        return jsonDescription;
    }

    public void setJsonDescription(String jsonDescription) {
        this.jsonDescription = jsonDescription;
    }
}

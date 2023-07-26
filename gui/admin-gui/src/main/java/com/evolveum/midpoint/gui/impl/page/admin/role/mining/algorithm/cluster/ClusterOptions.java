/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessMode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSearchMode;

public class ClusterOptions implements Serializable {
    private PageBase pageBase;
    private double similarity;
    private int minGroupSize;
    private int minIntersections;
    private String identifier;
    private ObjectFilter query;
    private int assignThreshold;
    RoleAnalysisProcessMode mode;
    RoleAnalysisSearchMode searchMode;
    String name;
    int defaultIntersectionSearch = 10;
    int defaultOccupancySearch = 10;
    double defaultMinFrequency = 0.4;
    double defaultMaxFrequency = 1;
    double defaultJaccardThreshold = 0.8;

    public ClusterOptions(PageBase pageBase, RoleAnalysisProcessMode mode,RoleAnalysisSearchMode searchMode) {
        this.pageBase = pageBase;
        this.mode = mode;
        this.searchMode = searchMode;
        setDefaultOptions(mode);
    }

    private void setDefaultOptions(RoleAnalysisProcessMode mode) {
        if (mode.equals(RoleAnalysisProcessMode.ROLE)) {
            this.similarity = 0.6;
            this.assignThreshold = 300;
            this.minIntersections = 10;
            this.minGroupSize = 2;
        } else if (mode.equals(RoleAnalysisProcessMode.USER)) {
            this.similarity = 0.8;
            this.assignThreshold = 10;
            this.minIntersections = 10;
            this.minGroupSize = 5;
        }
    }

    public PageBase getPageBase() {
        return pageBase;
    }

    public void setPageBase(PageBase pageBase) {
        this.pageBase = pageBase;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public int getMinGroupSize() {
        return minGroupSize;
    }

    public void setMinGroupSize(int minGroupSize) {
        this.minGroupSize = minGroupSize;
    }

    public int getMinIntersections() {
        return minIntersections;
    }

    public void setMinIntersections(int minIntersections) {
        this.minIntersections = minIntersections;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public ObjectFilter getQuery() {
        return query;
    }

    public void setQuery(ObjectFilter query) {
        this.query = query;
    }

    public int getAssignThreshold() {
        return assignThreshold;
    }

    public void setAssignThreshold(int assignThreshold) {
        this.assignThreshold = assignThreshold;
    }

    public RoleAnalysisProcessMode getMode() {
        return mode;
    }

    public void setMode(RoleAnalysisProcessMode mode) {
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDefaultIntersectionSearch() {
        return defaultIntersectionSearch;
    }

    public void setDefaultIntersectionSearch(int defaultIntersectionSearch) {
        this.defaultIntersectionSearch = defaultIntersectionSearch;
    }

    public int getDefaultOccupancySearch() {
        return defaultOccupancySearch;
    }

    public void setDefaultOccupancySearch(int defaultOccupancySearch) {
        this.defaultOccupancySearch = defaultOccupancySearch;
    }

    public double getDefaultMinFrequency() {
        return defaultMinFrequency;
    }

    public void setDefaultMinFrequency(double defaultMinFrequency) {
        this.defaultMinFrequency = defaultMinFrequency;
    }

    public double getDefaultMaxFrequency() {
        return defaultMaxFrequency;
    }

    public void setDefaultMaxFrequency(double defaultMaxFrequency) {
        this.defaultMaxFrequency = defaultMaxFrequency;
    }

    public RoleAnalysisSearchMode getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(RoleAnalysisSearchMode searchMode) {
        this.searchMode = searchMode;
    }

    public double getDefaultJaccardThreshold() {
        return defaultJaccardThreshold;
    }

    public void setDefaultJaccardThreshold(double defaultJaccardThreshold) {
        this.defaultJaccardThreshold = defaultJaccardThreshold;
    }
}

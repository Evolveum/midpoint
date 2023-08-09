/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

public class ClusterOptions implements Serializable {
    private PageBase pageBase;
    private double similarity;
    private int minGroupSize;
    private int minMembers;
    private int minIntersections;
    private ObjectFilter query;
    private int minProperties;
    private int maxProperties;
    RoleAnalysisProcessModeType mode;
    RoleAnalysisDetectionModeType searchMode;
    String name;
    ClusterObjectUtils.DETECT detect;

    int defaultIntersectionSearch = 10;
    int defaultOccupancySearch = 5;
    double defaultMinFrequency = 0.3;
    double defaultMaxFrequency = 1;
    double defaultJaccardThreshold = 0.8;

    public ClusterOptions(PageBase pageBase, RoleAnalysisProcessModeType mode, RoleAnalysisDetectionModeType searchMode) {
        this.pageBase = pageBase;
        this.mode = mode;
        this.searchMode = searchMode;
        setDefaultOptions(mode);
    }

    private void setDefaultOptions(RoleAnalysisProcessModeType mode) {
        if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            this.similarity = 0.6;
            this.minProperties = 300;
            this.minIntersections = 10;
            this.minGroupSize = 2;
            this.minMembers = 10;
        } else if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            this.similarity = 0.8;
            this.minProperties = 10;
            this.minIntersections = 10;
            this.minGroupSize = 5;
            this.minMembers = 10;
        }
    }

    public int getMinMembers() {
        return minMembers;
    }

    public void setMinMembers(int minMembers) {
        this.minMembers = minMembers;
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


    public ObjectFilter getQuery() {
        return query;
    }

    public void setQuery(ObjectFilter query) {
        this.query = query;
    }

    public int getMinProperties() {
        return minProperties;
    }

    public void setMinProperties(int minProperties) {
        this.minProperties = minProperties;
    }

    public RoleAnalysisProcessModeType getMode() {
        return mode;
    }

    public void setMode(RoleAnalysisProcessModeType mode) {
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

    public RoleAnalysisDetectionModeType getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(RoleAnalysisDetectionModeType searchMode) {
        this.searchMode = searchMode;
    }

    public double getDefaultJaccardThreshold() {
        return defaultJaccardThreshold;
    }

    public void setDefaultJaccardThreshold(double defaultJaccardThreshold) {
        this.defaultJaccardThreshold = defaultJaccardThreshold;
    }

    public int getMaxProperties() {
        return maxProperties;
    }

    public void setMaxProperties(int maxProperties) {
        this.maxProperties = maxProperties;
    }

    public ClusterObjectUtils.DETECT getDetect() {
        return detect;
    }

    public void setDetect(ClusterObjectUtils.DETECT detect) {
        this.detect = detect;
    }

}



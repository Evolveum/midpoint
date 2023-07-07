/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.prism.query.ObjectFilter;

public class ClusterOptions implements Serializable {
    private PageBase pageBase;
    private double similarity;
    private int minGroupSize;
    private int minIntersections;
    private String identifier;
    private ObjectFilter query;
    private int assignThreshold;
    ClusterObjectUtils.Mode mode;
    String name;

    public ClusterOptions(PageBase pageBase, ClusterObjectUtils.Mode mode) {
        this.pageBase = pageBase;
        this.mode = mode;
        setDefaultOptions(mode);
    }

    private void setDefaultOptions(ClusterObjectUtils.Mode mode) {
        if (mode.equals(ClusterObjectUtils.Mode.ROLE)) {
            this.similarity = 0.6;
            this.assignThreshold = 300;
            this.minIntersections = 10;
            this.minGroupSize = 2;
        } else if (mode.equals(ClusterObjectUtils.Mode.USER)) {
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

    public ClusterObjectUtils.Mode getMode() {
        return mode;
    }

    public void setMode(ClusterObjectUtils.Mode mode) {
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

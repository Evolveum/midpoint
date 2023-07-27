/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSearchModeType;

import java.io.Serializable;
import java.util.Set;

public class IntersectionObject implements Serializable {

    public static final String F_METRIC = "metric";

    public static final String F_TYPE = "type";

    Set<String> points;
    String type;
    int currentElements;
    Integer totalElements;
    double metric;
    Set<String> elements;
    RoleAnalysisSearchModeType searchMode;

    public IntersectionObject(Set<String> points, double metric, String type, int currentElements,
            Integer totalElements, Set<String> elements, RoleAnalysisSearchModeType searchMode) {
        this.elements = elements;
        this.points = points;
        this.metric = metric;
        this.type = type;
        this.currentElements = currentElements;
        this.totalElements = totalElements;
        this.searchMode = searchMode;
    }

    public Set<String> getElements() {
        return elements;
    }

    public Set<String> getPoints() {
        return points;
    }

    public double getMetric() {
        return metric;
    }

    public String getType() {
        return type;
    }

    public int getCurrentElements() {
        return currentElements;
    }

    public Integer getTotalElements() {
        return totalElements;
    }

    public RoleAnalysisSearchModeType getSearchMode() {
        return searchMode;
    }
}

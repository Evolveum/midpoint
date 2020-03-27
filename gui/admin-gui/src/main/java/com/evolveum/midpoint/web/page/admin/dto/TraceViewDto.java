/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.dto;

import java.io.Serializable;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericTraceVisualizationType;

@Experimental
@SuppressWarnings("unused")
public class TraceViewDto implements Serializable {

    public static final String F_CLOCKWORK_EXECUTION = "clockworkExecution";
    public static final String F_CLOCKWORK_CLICK = "clockworkClick";
    public static final String F_MAPPING_EVALUATION = "mappingEvaluation";
    public static final String F_FOCUS_LOAD = "focusLoad";
    public static final String F_PROJECTION_LOAD = "projectionLoad";
    public static final String F_FOCUS_CHANGE = "focusChange";
    public static final String F_PROJECTION_CHANGE = "projectionChange";
    public static final String F_OTHERS = "others";

    public static final String F_SHOW_INVOCATION_ID = "showInvocationId";
    public static final String F_SHOW_DURATION_BEFORE = "showDurationBefore";
    public static final String F_SHOW_DURATION_AFTER = "showDurationAfter";
    public static final String F_SHOW_REPO_OP_COUNT = "showRepoOpCount";
    public static final String F_SHOW_CONN_ID_OP_COUNT = "showConnIdOpCount";
    public static final String F_SHOW_REPO_OP_TIME = "showRepoOpTime";
    public static final String F_SHOW_CONN_ID_OP_TIME = "showConnIdOpTime";

    public static final String F_VISUALIZED_TRACE = "visualizedTrace";

    private GenericTraceVisualizationType clockworkExecution = GenericTraceVisualizationType.ONE_LINE;
    private GenericTraceVisualizationType clockworkClick = GenericTraceVisualizationType.ONE_LINE;
    private GenericTraceVisualizationType mappingEvaluation = GenericTraceVisualizationType.ONE_LINE;
    private GenericTraceVisualizationType focusLoad = GenericTraceVisualizationType.ONE_LINE;
    private GenericTraceVisualizationType projectionLoad = GenericTraceVisualizationType.ONE_LINE;
    private GenericTraceVisualizationType focusChange = GenericTraceVisualizationType.ONE_LINE;
    private GenericTraceVisualizationType projectionChange = GenericTraceVisualizationType.ONE_LINE;
    private GenericTraceVisualizationType others = GenericTraceVisualizationType.HIDE;

    private boolean showInvocationId;
    private boolean showDurationBefore;
    private boolean showDurationAfter;
    private boolean showRepoOpCount;
    private boolean showConnIdOpCount;
    private boolean showRepoOpTime;
    private boolean showConnIdOpTime;

    private transient String visualizedTrace;

    public TraceViewDto() {
    }

    public GenericTraceVisualizationType getClockworkExecution() {
        return clockworkExecution;
    }

    public void setClockworkExecution(GenericTraceVisualizationType clockworkExecution) {
        this.clockworkExecution = clockworkExecution;
    }

    public GenericTraceVisualizationType getClockworkClick() {
        return clockworkClick;
    }

    public void setClockworkClick(GenericTraceVisualizationType clockworkClick) {
        this.clockworkClick = clockworkClick;
    }

    public GenericTraceVisualizationType getMappingEvaluation() {
        return mappingEvaluation;
    }

    public void setMappingEvaluation(GenericTraceVisualizationType mappingEvaluation) {
        this.mappingEvaluation = mappingEvaluation;
    }

    public GenericTraceVisualizationType getFocusLoad() {
        return focusLoad;
    }

    public void setFocusLoad(GenericTraceVisualizationType focusLoad) {
        this.focusLoad = focusLoad;
    }

    public GenericTraceVisualizationType getProjectionLoad() {
        return projectionLoad;
    }

    public void setProjectionLoad(GenericTraceVisualizationType projectionLoad) {
        this.projectionLoad = projectionLoad;
    }

    public GenericTraceVisualizationType getFocusChange() {
        return focusChange;
    }

    public void setFocusChange(GenericTraceVisualizationType focusChange) {
        this.focusChange = focusChange;
    }

    public GenericTraceVisualizationType getProjectionChange() {
        return projectionChange;
    }

    public void setProjectionChange(GenericTraceVisualizationType projectionChange) {
        this.projectionChange = projectionChange;
    }

    public GenericTraceVisualizationType getOthers() {
        return others;
    }

    public void setOthers(GenericTraceVisualizationType others) {
        this.others = others;
    }

    public boolean isShowInvocationId() {
        return showInvocationId;
    }

    public void setShowInvocationId(boolean showInvocationId) {
        this.showInvocationId = showInvocationId;
    }

    public boolean isShowDurationBefore() {
        return showDurationBefore;
    }

    public void setShowDurationBefore(boolean showDurationBefore) {
        this.showDurationBefore = showDurationBefore;
    }

    public boolean isShowDurationAfter() {
        return showDurationAfter;
    }

    public void setShowDurationAfter(boolean showDurationAfter) {
        this.showDurationAfter = showDurationAfter;
    }

    public boolean isShowRepoOpCount() {
        return showRepoOpCount;
    }

    public void setShowRepoOpCount(boolean showRepoOpCount) {
        this.showRepoOpCount = showRepoOpCount;
    }

    public boolean isShowConnIdOpCount() {
        return showConnIdOpCount;
    }

    public void setShowConnIdOpCount(boolean showConnIdOpCount) {
        this.showConnIdOpCount = showConnIdOpCount;
    }

    public boolean isShowRepoOpTime() {
        return showRepoOpTime;
    }

    public void setShowRepoOpTime(boolean showRepoOpTime) {
        this.showRepoOpTime = showRepoOpTime;
    }

    public boolean isShowConnIdOpTime() {
        return showConnIdOpTime;
    }

    public void setShowConnIdOpTime(boolean showConnIdOpTime) {
        this.showConnIdOpTime = showConnIdOpTime;
    }

    public void setVisualizedTrace(String visualizedTrace) {
        this.visualizedTrace = visualizedTrace;
    }

    public String getVisualizedTrace() {
        return visualizedTrace;
    }

    public boolean isVisualized() {
        return visualizedTrace != null;
    }
}

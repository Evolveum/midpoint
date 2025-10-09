/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.progress;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ProgressDto implements Serializable {

    private List<ProgressReportActivityDto> progressReportActivities = new ArrayList<>();
    private List<String> logItems = new ArrayList<>();

    public List<ProgressReportActivityDto> getProgressReportActivities() {
        return progressReportActivities;
    }

    public void setProgressReportActivities(List<ProgressReportActivityDto> progressReportActivities) {
        this.progressReportActivities = progressReportActivities;
    }

    public List<String> getLogItems() {
        return logItems;
    }

    public void setLogItems(List<String> logItems) {
        this.logItems = logItems;
    }

    public void log(String message) {
        logItems.add(message);
    }

    public void add(ProgressReportActivityDto item) {
        progressReportActivities.add(item);
    }

    public void clear() {
        progressReportActivities.clear();
        logItems.clear();
    }

    public boolean allSuccess() {
        for (ProgressReportActivityDto si : progressReportActivities) {
            if (!si.isSuccess()) {
                return false;
            }
        }
        return true;
    }

}

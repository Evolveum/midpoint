/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.progress;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
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

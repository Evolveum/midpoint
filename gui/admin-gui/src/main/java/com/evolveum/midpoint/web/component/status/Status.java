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

package com.evolveum.midpoint.web.component.status;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class Status implements Serializable {

    public static class StatusItem implements Serializable {
        private String description;
        private String state;

        public StatusItem(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    private List<StatusItem> statusItems = new ArrayList<>();
    private List<String> logItems = new ArrayList<>();

    public List<StatusItem> getStatusItems() {
        return statusItems;
    }

    public void setStatusItems(List<StatusItem> statusItems) {
        this.statusItems = statusItems;
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

    public void add(StatusItem si) {
        statusItems.add(si);
    }

    public void clear() {
        statusItems.clear();
        logItems.clear();
    }
}

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

import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.OperationStatus;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugConfDialogDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.model.api.OperationStatus.EventType;
import static com.evolveum.midpoint.model.api.OperationStatus.EventType.RESOURCE_OBJECT_OPERATION;
import static com.evolveum.midpoint.web.page.PageBase.createStringResourceStatic;

/**
 * @author mederly
 */
public class StatusDto implements Serializable {

    public static class StatusItem implements Serializable {

        private EventType eventType;
        private ResourceShadowDiscriminator resourceShadowDiscriminator;        // if applicable w.r.t. eventType
        private String resourceName;
        private OperationResultStatusType state;

        public EventType getEventType() {
            return eventType;
        }

        public void setEventType(EventType eventType) {
            this.eventType = eventType;
        }

        public ResourceShadowDiscriminator getResourceShadowDiscriminator() {
            return resourceShadowDiscriminator;
        }

        public void setResourceShadowDiscriminator(ResourceShadowDiscriminator resourceShadowDiscriminator) {
            this.resourceShadowDiscriminator = resourceShadowDiscriminator;
        }

        public void setResourceName(String resourceName) {
            this.resourceName = resourceName;
        }

        public String getResourceName() {
            return resourceName;
        }

        public OperationResultStatusType getState() {
            return state;
        }

        public void setState(OperationResultStatusType state) {
            this.state = state;
        }

        public boolean correspondsTo(OperationStatus newStatus) {
            if (newStatus == null) {
                return false;           // should not occur anyway
            }
            if (eventType != newStatus.getEventType()) {
                return false;
            }
            if (eventType == RESOURCE_OBJECT_OPERATION
                    && (resourceShadowDiscriminator == null ||
                        !resourceShadowDiscriminator.equals(newStatus.getResourceShadowDiscriminator()))) {
                return false;
            }
            return true;
        }

        public boolean isSuccess() {
            return state == null || state == OperationResultStatusType.SUCCESS;
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

    public boolean allSuccess() {
        for (StatusItem si : statusItems) {
            if (!si.isSuccess()) {
                return false;
            }
        }
        return true;
    }
}

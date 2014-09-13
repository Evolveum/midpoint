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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.model.api.OperationStatus.EventType.RESOURCE_OBJECT_OPERATION;

/**
 * @author mederly
 */
public class StatusDto implements Serializable {

    public static class StatusItem implements Serializable {

        private OperationStatus operationStatus;            // to be able to check correspondence
        private ResourceShadowDiscriminator awaitingRsd;    // TODO fixme
        private String description;
        private String state;
        private boolean isSuccess;

        public StatusItem(OperationStatus operationStatus) {
            this.operationStatus = operationStatus;
        }

        public OperationStatus getOperationStatus() {
            return operationStatus;
        }

        public void setOperationStatus(OperationStatus operationStatus) {
            this.operationStatus = operationStatus;
        }

        public StatusItem(OperationStatus operationStatus, String description) {
            this.operationStatus = operationStatus;
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

        public boolean isSuccess() {
            return isSuccess;
        }

        public void setSuccess(boolean isSuccess) {
            this.isSuccess = isSuccess;
        }

        public ResourceShadowDiscriminator getAwaitingRsd() {
            return awaitingRsd;
        }

        public void setAwaitingRsd(ResourceShadowDiscriminator awaitingRsd) {
            this.awaitingRsd = awaitingRsd;
        }

        public boolean correspondsTo(OperationStatus newStatus) {
            if (newStatus == null) {
                return false;           // should not occur too often
            }

            if (operationStatus == null) {          // "awaiting" entry
                if (newStatus.getEventType() != RESOURCE_OBJECT_OPERATION || newStatus.getResourceShadowDiscriminator() == null) {
                    return false;
                }
                if (!newStatus.getResourceShadowDiscriminator().equals(awaitingRsd)) {
                    return false;
                }
                return true;
            }

            if (operationStatus.getEventType() != newStatus.getEventType()) {
                return false;
            }
            if (operationStatus.getEventType() == RESOURCE_OBJECT_OPERATION
                    && (operationStatus.getResourceShadowDiscriminator() == null ||
                        !operationStatus.getResourceShadowDiscriminator().equals(newStatus.getResourceShadowDiscriminator()))) {
                return false;
            }
            return true;
        }

        public boolean correspondsTo(ResourceShadowDiscriminator discriminator) {
            if (discriminator == null) {
                return false;
            }
            if (awaitingRsd != null && awaitingRsd.equals(discriminator)) {
                return true;
            }
            if (operationStatus != null && operationStatus.getEventType() == RESOURCE_OBJECT_OPERATION && discriminator.equals(operationStatus.getResourceShadowDiscriminator())) {
                return true;
            }
            return false;
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

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
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import java.io.Serializable;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.RESOURCE_OBJECT_OPERATION;

/**
* @author Pavol
*/
public class ProgressReportActivityDto implements Serializable {

    private ProgressInformation.ActivityType activityType;
    private ResourceShadowDiscriminator resourceShadowDiscriminator;        // if applicable w.r.t. activityType
    private String resourceName;                                            // pre-resolved resource name, if applicable
    private OperationResultStatusType status;

    public ProgressInformation.ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ProgressInformation.ActivityType activityType) {
        this.activityType = activityType;
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

    public OperationResultStatusType getStatus() {
        return status;
    }

    public void setStatus(OperationResultStatusType status) {
        this.status = status;
    }

    public boolean correspondsTo(ProgressInformation newStatus) {
        if (newStatus == null) {
            return false;           // should not occur anyway
        }
        if (activityType != newStatus.getActivityType()) {
            return false;
        }
        if (activityType == RESOURCE_OBJECT_OPERATION
                && (resourceShadowDiscriminator == null ||
                    !resourceShadowDiscriminator.equals(newStatus.getResourceShadowDiscriminator()))) {
            return false;
        }
        return true;
    }

    public boolean isSuccess() {
        return status == null || status == OperationResultStatusType.SUCCESS;
    }
}

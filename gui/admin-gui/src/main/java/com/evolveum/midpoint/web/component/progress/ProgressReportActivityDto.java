/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import java.io.Serializable;
import java.util.List;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.RESOURCE_OBJECT_OPERATION;

/**
* @author Pavol
*/
public class ProgressReportActivityDto implements Serializable {

    private ProgressInformation.ActivityType activityType;
    private ResourceShadowDiscriminator resourceShadowDiscriminator;        // if applicable w.r.t. activityType
    private String resourceName;                                            // pre-resolved resource name, if applicable
    private OperationResultStatusType status;
    private OperationResult operationResult;
    // additional information on resource-related operation
    private String resourceObjectName;
    private List<ResourceOperationResult> resourceOperationResultList;

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

    public OperationResult getOperationResult() {
        return operationResult;
    }

    public void setOperationResult(OperationResult operationResult) {
        this.operationResult = operationResult;
    }

    public String getResourceObjectName() {
        return resourceObjectName;
    }

    public void setResourceObjectName(String resourceObjectName) {
        this.resourceObjectName = resourceObjectName;
    }

    public List<ResourceOperationResult> getResourceOperationResultList() {
        return resourceOperationResultList;
    }

    public void setResourceOperationResultList(List<ResourceOperationResult> resourceOperationResultList) {
        this.resourceOperationResultList = resourceOperationResultList;
    }

    public boolean correspondsTo(ProgressInformation newStatus) {
        if (newStatus == null) {
            return false;           // should not occur anyway
        }
        if (activityType != newStatus.getActivityType()) {
            return false;
        }
        if (activityType == RESOURCE_OBJECT_OPERATION) {
            if (resourceShadowDiscriminator != null &&
                    !resourceShadowDiscriminator.equals(newStatus.getResourceShadowDiscriminator())) {
                return false;
            }
            if (resourceShadowDiscriminator == null && newStatus.getResourceShadowDiscriminator() != null) {
                // actually, we consider all resource-related records with null RSD to be equal (even if they deal with different resources)
                return false;
            }
        }
        return true;
    }

    public boolean isSuccess() {
        return status == null || status == OperationResultStatusType.SUCCESS;
    }

    public static class ResourceOperationResult implements Serializable {
        private ChangeType changeType;
        private OperationResultStatus resultStatus;

        public ResourceOperationResult(ChangeType changeType, OperationResultStatus resultStatus) {
            this.changeType = changeType;
            this.resultStatus = resultStatus;
        }

        public ChangeType getChangeType() {
            return changeType;
        }

        public void setChangeType(ChangeType changeType) {
            this.changeType = changeType;
        }

        public OperationResultStatus getResultStatus() {
            return resultStatus;
        }

        public void setResultStatus(OperationResultStatus resultStatus) {
            this.resultStatus = resultStatus;
        }
    }
}

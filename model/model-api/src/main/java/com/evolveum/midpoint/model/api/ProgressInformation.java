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

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;

import java.io.Serializable;

/**
 * Describes a state of the operation. Although theoretically everything relevant should be in the model context,
 * as a convenience for clients interpreting this structure we offer explicit denotation of specific events (see ActivityType).
 *
 * HIGHLY EXPERIMENTAL. Probably should be refactored (e.g. by providing strong typing via class hierarchy).
 *
 * @author mederly
 */
public class ProgressInformation implements Serializable, DebugDumpable {

    /**
     * There are some basic kinds of activities relevant for progress reporting.
     *
     * These are activities that could take a considerably long time (because of communicating
     * with external systems, except focus operation, which is important from the users' point
     * of view).
     */
    public enum ActivityType {
        NOTIFICATIONS,
        WORKFLOWS,
        PROJECTOR,
        RESOURCE_OBJECT_OPERATION,
        FOCUS_OPERATION,
	    CLOCKWORK,
	    WAITING
    }

    /**
     * We usually report on entering and exiting a particular activity.
     */
    public enum StateType {
        ENTERING, EXITING
    }

    private ActivityType activityType;
    private StateType stateType;
    private ResourceShadowDiscriminator resourceShadowDiscriminator;              // if relevant
    private OperationResult operationResult;                                      // if relevant (mostly on "_EXITING" events)
    private String message;                                                       // A custom message for cases not covered by the progress info (should be only rarely used).

    public ProgressInformation(ActivityType activityType, StateType stateType) {
        this.activityType = activityType;
        this.stateType = stateType;
    }

    public ProgressInformation(ActivityType activityType, StateType stateType, String message) {
        this.activityType = activityType;
        this.stateType = stateType;
        this.message = message;
    }

    public ProgressInformation(ActivityType activityType, OperationResult operationResult) {
        this.activityType = activityType;
        this.stateType = StateType.EXITING;
        this.operationResult = operationResult;
    }

    public ProgressInformation(ActivityType activityType, ResourceShadowDiscriminator resourceShadowDiscriminator, StateType stateType) {
        this(activityType, stateType);
        this.resourceShadowDiscriminator = resourceShadowDiscriminator;
    }

    public ProgressInformation(ActivityType activityType, ResourceShadowDiscriminator resourceShadowDiscriminator, OperationResult operationResult) {
        this(activityType, operationResult);
        this.resourceShadowDiscriminator = resourceShadowDiscriminator;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public StateType getStateType() {
        return stateType;
    }

    public OperationResult getOperationResult() {
        return operationResult;
    }

    public void setOperationResult(OperationResult operationResult) {
        this.operationResult = operationResult;
    }

    public void setStateType(StateType stateType) {
        this.stateType = stateType;
    }

    public ResourceShadowDiscriminator getResourceShadowDiscriminator() {
        return resourceShadowDiscriminator;
    }

    public void setResourceShadowDiscriminator(ResourceShadowDiscriminator resourceShadowDiscriminator) {
        this.resourceShadowDiscriminator = resourceShadowDiscriminator;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(ProgressInformation.class.getSimpleName()).append(" ");
        sb.append("activityType=").append(activityType);
        sb.append(", state=").append(stateType);
        if (resourceShadowDiscriminator != null) {
            sb.append(", resourceShadowDiscriminator=").append(resourceShadowDiscriminator.toHumanReadableDescription());
        }
        if (message != null) {
            sb.append("\n");
            for (int i = 0; i < indent; i++) {
                sb.append(INDENT_STRING);
            }
            sb.append("message=").append(message);
        }
        if (operationResult != null) {
            sb.append("\n");
            sb.append(operationResult.debugDump(indent));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return activityType + ":" + stateType
                + (resourceShadowDiscriminator != null ? ", resource=" + resourceShadowDiscriminator.getResourceOid() : "")
                + (operationResult != null ? ", result=" + operationResult.getStatus() : "")
                + (message != null ? ": " + message : "");
    }
}

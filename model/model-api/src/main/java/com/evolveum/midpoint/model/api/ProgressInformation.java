/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;

import java.io.Serializable;

/**
 * Describes a state of the operation. Although theoretically everything relevant should be in the model context,
 * as a convenience for clients interpreting this structure we offer explicit denotation of specific events (see ActivityType).
 *
 * HIGHLY EXPERIMENTAL. Probably should be refactored (e.g. by providing strong typing via class hierarchy).
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
    private ProjectionContextKey projectionContextKey; // if relevant
    private OperationResult operationResult; // if relevant (mostly on "_EXITING" events)
    private String message; // A custom message for cases not covered by the progress info (should be only rarely used).

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

    public ProgressInformation(ActivityType activityType, ProjectionContextKey projectionContextKey, StateType stateType) {
        this(activityType, stateType);
        this.projectionContextKey = projectionContextKey;
    }

    public ProgressInformation(ActivityType activityType, ProjectionContextKey projectionContextKey, OperationResult operationResult) {
        this(activityType, operationResult);
        this.projectionContextKey = projectionContextKey;
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

    public ProjectionContextKey getProjectionContextKey() {
        return projectionContextKey;
    }

    public void setProjectionContextKey(ProjectionContextKey projectionContextKey) {
        this.projectionContextKey = projectionContextKey;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(INDENT_STRING.repeat(Math.max(0, indent)));
        sb.append(ProgressInformation.class.getSimpleName()).append(" ");
        sb.append("activityType=").append(activityType);
        sb.append(", state=").append(stateType);
        if (projectionContextKey != null) {
            sb.append(", resourceShadowDiscriminator=").append(projectionContextKey.toHumanReadableDescription());
        }
        if (message != null) {
            sb.append("\n");
            sb.append(INDENT_STRING.repeat(Math.max(0, indent)));
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
                + (projectionContextKey != null ? ", resource=" + projectionContextKey.getResourceOid() : "")
                + (operationResult != null ? ", result=" + operationResult.getStatus() : "")
                + (message != null ? ": " + message : "");
    }
}

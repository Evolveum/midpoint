/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.wf.api.WorkItemOperationInfo;
import com.evolveum.midpoint.wf.api.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;

/**
 * @author mederly
 */
public class WorkItemEvent extends WorkflowEvent {

    @NotNull protected final CaseWorkItemType workItem;
    // (Currently) Each work item event is related to at most one assignee. So, if a work item has more assignees,
    // more events will be generated. This might change in a future.
    protected final SimpleObjectRef assignee;
    /**
     * User who "pressed the button". I.e. the one that really approved, rejected or delegated/escalated a work item.
     * In case of automated actions (completion, delegation/escalation) this is not filled-in.
     */
    protected final SimpleObjectRef initiator;
    protected final WorkItemOperationInfo operationInfo;
    protected final WorkItemOperationSourceInfo sourceInfo;
    protected final Duration timeBefore;

    WorkItemEvent(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator, @NotNull ChangeType changeType,
            @NotNull CaseWorkItemType workItem,
            @Nullable SimpleObjectRef assignee, @Nullable SimpleObjectRef initiator,
            @Nullable WorkItemOperationInfo operationInfo, @Nullable WorkItemOperationSourceInfo sourceInfo,
            @Nullable ApprovalContextType approvalContext,
            @NotNull CaseType aCase,
            @Nullable EventHandlerType handler, @Nullable Duration timeBefore) {
        super(lightweightIdentifierGenerator, changeType, approvalContext, aCase, handler);
        Validate.notNull(workItem);
        this.workItem = workItem;
        this.assignee = assignee;
        this.initiator = initiator;
        this.operationInfo = operationInfo;
        this.sourceInfo = sourceInfo;
        this.timeBefore = timeBefore;
    }

    public String getWorkItemName() {
        return PolyString.getOrig(workItem.getName());  // todo MID-5916
    }

    @NotNull
    public CaseWorkItemType getWorkItem() {
        return workItem;
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.WORK_ITEM_EVENT || eventCategoryType == EventCategoryType.WORKFLOW_EVENT;
    }

    public SimpleObjectRef getAssignee() {
        return assignee;
    }

    public SimpleObjectRef getInitiator() {
        return initiator;
    }

    public WorkItemOperationKindType getOperationKind() {
        return operationInfo != null ? operationInfo.getOperationKind() : null;
    }

    public AbstractWorkItemActionType getSource() {
        return sourceInfo != null ? sourceInfo.getSource() : null;
    }

    public WorkItemEventCauseInformationType getCause() {
        return sourceInfo != null ? sourceInfo.getCause() : null;
    }

    public Duration getTimeBefore() {
        return timeBefore;
    }

    public WorkItemOperationInfo getOperationInfo() {
        return operationInfo;
    }

    public WorkItemOperationSourceInfo getSourceInfo() {
        return sourceInfo;
    }

    @Override
    public void createExpressionVariables(VariablesMap variables, OperationResult result) {
        super.createExpressionVariables(variables, result);
        variables.put(ExpressionConstants.VAR_ASSIGNEE, resolveTypedObject(assignee, false, result));
        variables.put(ExpressionConstants.VAR_WORK_ITEM, workItem, CaseWorkItemType.class);
    }

    public AbstractWorkItemOutputType getOutput() {
        return workItem.getOutput();
    }

    @Override
    public String getOutcome() {
        AbstractWorkItemOutputType output = getOutput();
        return output != null ? output.getOutcome() : null;
    }

    @Override
    public String toString() {
        return "WorkflowProcessEvent{" +
                "workflowEvent=" + super.toString() +
                ", workItemName=" + getWorkItemName() +
                ", assignee=" + assignee +
                '}';

    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "workItemName", getWorkItemName(), indent + 1);
        DebugUtil.debugDumpWithLabelToString(sb, "assignee", assignee, indent + 1);
        return sb.toString();
    }
}

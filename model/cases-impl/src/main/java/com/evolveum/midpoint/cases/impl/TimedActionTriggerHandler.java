/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.cases.impl;

import static java.util.Objects.requireNonNullElse;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.getExtensionItemRealValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jakarta.annotation.PostConstruct;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.api.events.WorkItemAllocationChangeOperationInfo;
import com.evolveum.midpoint.cases.api.events.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.cases.api.request.CompleteWorkItemsRequest;
import com.evolveum.midpoint.cases.api.request.CompleteWorkItemsRequest.SingleCompletion;
import com.evolveum.midpoint.cases.impl.helpers.CaseExpressionEvaluationHelper;
import com.evolveum.midpoint.cases.impl.helpers.CaseMiscHelper;
import com.evolveum.midpoint.cases.impl.helpers.NotificationHelper;
import com.evolveum.midpoint.model.api.trigger.MultipleTriggersHandler;
import com.evolveum.midpoint.model.api.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Processes trigger(s) on case objects that implement timed actions (e.g. escalation or auto-completion).
 *
 * Limitation: Currently supported only for approval cases.
 */
@Component
public class TimedActionTriggerHandler implements MultipleTriggersHandler {

    public static final String HANDLER_URI = SchemaConstants.NS_WORKFLOW + "/trigger/timed-action/handler-3";

    private static final Trace LOGGER = TraceManager.getTrace(TimedActionTriggerHandler.class);

    private static final String OP_HANDLE = TimedActionTriggerHandler.class.getName() + ".handle";
    private static final String OP_HANDLE_TRIGGER = TimedActionTriggerHandler.class.getName() + ".handleTrigger";
    private static final String OP_HANDLE_COMPLETIONS = TimedActionTriggerHandler.class.getName() + ".handleCompletions";

    @Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
    @Autowired private WorkItemManager workItemManager;
    @Autowired private NotificationHelper notificationHelper;
    @Autowired private CaseExpressionEvaluationHelper evaluationHelper;
    @Autowired private CaseMiscHelper miscHelper;

    @PostConstruct
    private void initialize() {
        triggerHandlerRegistry.register(HANDLER_URI, this);
    }

    @Override
    public <O extends ObjectType> Collection<TriggerType> handle(
            @NotNull PrismObject<O> object,
            @NotNull Collection<TriggerType> triggers,
            @NotNull RunningTask opTask,
            @NotNull OperationResult parentResult) {
        if (!(object.asObjectable() instanceof CaseType)) {
            throw new IllegalArgumentException("Unexpected object type: should be CaseType: " + object);
        }
        OperationResult result = parentResult.createSubresult(OP_HANDLE);
        try {
            return new TriggersExecution((CaseType) object.asObjectable(), triggers, opTask)
                    .execute(result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't process triggers: " + t.getMessage(), t);
            throw t;
        } finally {
            result.close();
        }
    }

    private class TriggersExecution {

        @NotNull private final CaseType aCase;
        @NotNull private final Collection<TriggerType> triggers;
        @NotNull private final RunningTask task;
        @NotNull private final XMLGregorianCalendar now;

        /**
         * "Completion" action has to be executed for all the work items at once.
         * Otherwise the first work item will be regularly completed and the other ones cancelled
         * because the stage is being closed. (Before midPoint 4.0 there has to be a special code to treat this.)
         */
        @NotNull private final List<SingleCompletion> completionActions = new ArrayList<>();

        private WorkItemEventCauseInformationType cause;

        TriggersExecution(@NotNull CaseType aCase, @NotNull Collection<TriggerType> triggers, @NotNull RunningTask task) {
            this.aCase = aCase;
            this.triggers = triggers;
            this.task = task;
            this.now = XmlTypeConverter.createXMLGregorianCalendar();
        }

        Collection<TriggerType> execute(OperationResult result) {
            LOGGER.trace("Going to process {} trigger(s) on {}", triggers.size(), aCase);

            List<TriggerType> processedTriggers = new ArrayList<>();
            for (TriggerType trigger : triggers) {
                CaseWorkItemType workItem = getWorkItem(trigger);
                boolean canForgetTrigger = workItem == null || processTrigger(trigger, workItem, result);
                if (canForgetTrigger) {
                    processedTriggers.add(trigger);
                }
            }
            if (!completionActions.isEmpty()) {
                processCompletionActions(result);
            }
            return processedTriggers;
        }

        private CaseWorkItemType getWorkItem(TriggerType trigger) {
            Long workItemId = getExtensionItemRealValue(trigger.getExtension(), SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ID);
            if (workItemId == null) {
                LOGGER.warn("Trigger without workItemId; ignoring it: {}", trigger);
                return null;
            }
            CaseWorkItemType workItem = CaseTypeUtil.getWorkItem(aCase, workItemId);
            if (workItem == null) {
                LOGGER.warn("Work item {} couldn't be found; ignoring the trigger: {}", workItemId, trigger);
            }
            LOGGER.trace("Found work item {}", workItem);
            return workItem;
        }

        private boolean processTrigger(TriggerType trigger, CaseWorkItemType workItem, OperationResult result) {
            return new TriggerExecution(trigger, workItem)
                    .process(result);
        }

        private class TriggerExecution {

            @NotNull private final TriggerType trigger;
            @NotNull private final CaseWorkItemType workItem;

            TriggerExecution(@NotNull TriggerType trigger, @NotNull CaseWorkItemType workItem) {
                this.trigger = trigger;
                this.workItem = workItem;
            }

            private boolean process(OperationResult parentResult) {
                LOGGER.trace("Going to process trigger {}", trigger);

                OperationResult result = parentResult.createSubresult(OP_HANDLE_TRIGGER);
                try {
                    Duration timeBeforeAction = getExtValue(SchemaConstants.MODEL_EXTENSION_TIME_BEFORE_ACTION);
                    if (timeBeforeAction != null) {
                        AbstractWorkItemActionType action = getExtValue(SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTION);
                        sendNotifications(timeBeforeAction, action, result);
                    } else {
                        WorkItemActionsType actions = getExtValue(SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTIONS);
                        executeActions(actions, result);
                    }
                    return true;
                } catch (Exception e) {
                    String message =
                            "Exception while handling work item trigger for ID " + workItem.getId() + ": " + e.getMessage();
                    result.recordPartialError(message, e);
                    // Not propagating the exception
                    return false;
                } catch (Throwable t) {
                    result.recordFatalError(t);
                    throw t;
                } finally {
                    result.close();
                }
            }

            /**
             * Here we send notifications about future actions (delegation, escalation, completion).
             */
            private void sendNotifications(
                    @NotNull Duration timeBeforeAction,
                    AbstractWorkItemActionType action, // normally not null
                    OperationResult result) throws SchemaException {
                if (action == null) {
                    LOGGER.warn("Notification trigger without workItemAction; ignoring it. In:\n{}", aCase.debugDump(1));
                    return;
                }
                WorkItemOperationKindType operationKind = ApprovalContextUtil.getOperationKind(action);
                LOGGER.trace("Processing notification about '{}' with time before action: {}", operationKind, timeBeforeAction);

                WorkItemEventCauseInformationType cause = ApprovalContextUtil.createCause(action);
                List<ObjectReferenceType> assigneesAndDeputies = miscHelper.getAssigneesAndDeputies(workItem, task, result);
                WorkItemAllocationChangeOperationInfo operationInfo =
                        new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputies, null);
                WorkItemOperationSourceInfo sourceInfo = new WorkItemOperationSourceInfo(null, cause, action);
                notificationHelper.notifyWorkItemAllocationChangeCurrentActors(
                        workItem, operationInfo, sourceInfo, timeBeforeAction, aCase, task, result);
            }

            /**
             * Here we execute the actual actions (delegation, escalation, completion, notification).
             */
            private void executeActions(
                    WorkItemActionsType actions, // normally not null
                    OperationResult result)
                    throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
                    CommunicationException, ConfigurationException {
                if (actions == null) {
                    LOGGER.warn("Trigger without workItemActions; ignoring it. In:\n{}", aCase.debugDump(1));
                    return;
                }
                for (WorkItemNotificationActionType notificationAction : actions.getNotify()) {
                    executeNotificationAction(notificationAction, result);
                }
                if (actions.getDelegate() != null) {
                    executeDelegateAction(actions.getDelegate(), false, result);
                }
                if (actions.getEscalate() != null) {
                    executeDelegateAction(actions.getEscalate(), true, result);
                }
                CompleteWorkItemActionType complete = actions.getComplete();
                if (complete != null) {
                    SingleCompletion completion = new SingleCompletion(
                            workItem.getId(),
                            new AbstractWorkItemOutputType()
                                    .outcome( // TODO remove dependency on approvals
                                            requireNonNullElse(complete.getOutcome(), MODEL_APPROVAL_OUTCOME_REJECT)));

                    LOGGER.trace("Postponing completion: {}", completion);
                    completionActions.add(completion);
                    cause = ApprovalContextUtil.createCause(complete);
                }
            }

            private void executeNotificationAction(
                    @NotNull WorkItemNotificationActionType notificationAction,
                    @NotNull OperationResult result) throws SchemaException {
                LOGGER.trace("Executing notification action: {}", notificationAction);
                WorkItemTypeUtil.assertHasCaseOid(workItem);
                WorkItemEventCauseInformationType cause = ApprovalContextUtil.createCause(notificationAction);
                if (Boolean.FALSE.equals(notificationAction.isPerAssignee())) {
                    notificationHelper.notifyWorkItemCustom(
                            null, workItem, cause, aCase, notificationAction, task, result);
                } else {
                    List<ObjectReferenceType> assigneesAndDeputies = miscHelper.getAssigneesAndDeputies(workItem, task, result);
                    for (ObjectReferenceType assigneeOrDeputy : assigneesAndDeputies) {
                        notificationHelper.notifyWorkItemCustom(
                                assigneeOrDeputy, workItem, cause, aCase, notificationAction, task, result);
                    }
                }
            }

            private void executeDelegateAction(
                    @NotNull DelegateWorkItemActionType delegateAction,
                    boolean escalate,
                    @NotNull OperationResult result)
                    throws SecurityViolationException, ObjectNotFoundException, SchemaException,
                    ExpressionEvaluationException, CommunicationException, ConfigurationException {
                LOGGER.trace("Executing delegation/escalation action: {}", delegateAction);
                WorkItemDelegationRequestType request = new WorkItemDelegationRequestType();
                request.getDelegate().addAll(
                        computeDelegateTo(delegateAction, result));
                request.setMethod(delegateAction.getDelegationMethod());
                workItemManager.delegateWorkItem(
                        WorkItemId.of(workItem),
                        request,
                        escalate ?
                                ApprovalContextUtil.createEscalationLevelInformation(delegateAction) : null,
                        delegateAction.getDuration(),
                        ApprovalContextUtil.createCause(delegateAction),
                        now,
                        task,
                        result);
            }

            private List<ObjectReferenceType> computeDelegateTo(DelegateWorkItemActionType delegateAction, OperationResult result)
                    throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                    ConfigurationException, SecurityViolationException {
                List<ObjectReferenceType> rv = CloneUtil.cloneCollectionMembers(delegateAction.getApproverRef());
                if (!delegateAction.getApproverExpression().isEmpty()) {
                    VariablesMap variables = miscHelper.getDefaultVariables(
                            aCase, aCase.getApprovalContext(), getChannel(task), result);
                    variables.put(ExpressionConstants.VAR_WORK_ITEM, workItem, CaseWorkItemType.class);
                    rv.addAll(evaluationHelper.evaluateRefExpressions(delegateAction.getApproverExpression(),
                            variables, "computing delegates", task, result));
                }
                return rv;
            }

            private <T> T getExtValue(ItemName name) {
                return getExtensionItemRealValue(trigger.getExtension(), name);
            }
        }

        private void processCompletionActions(OperationResult parentResult) {
            OperationResult result = parentResult.createSubresult(OP_HANDLE_COMPLETIONS);
            try {
                CompleteWorkItemsRequest request = new CompleteWorkItemsRequest(aCase.getOid(), cause);
                request.getCompletions().addAll(completionActions);

                LOGGER.trace("Going to process {} completion action(s) in a request:\n{}", completionActions.size(), request);
                workItemManager.completeWorkItems(request, task, result);
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't handler work item completion", t);
                result.recordFatalError("Couldn't handle work item completion", t);
                // but let's ignore this fact and mark all triggers as processed
            } finally {
                result.close();
            }
        }
    }

    private String getChannel(Task opTask) {
        // TODO TODO TODO here we should put the original channel (determined from the root model context!)
        return opTask.getChannel();
    }
}

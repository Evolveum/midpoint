/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.actions;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.api.events.FutureNotificationEvent.AllocationChangeNew;
import com.evolveum.midpoint.cases.api.extensions.EngineExtension;
import com.evolveum.midpoint.cases.api.extensions.StageOpeningResult;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.cases.api.events.WorkItemAllocationChangeOperationInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static com.evolveum.midpoint.cases.api.events.FutureNotificationEvent.*;

/**
 * Opens a default unnumbered stage (if stages are not used), or a regular - i.e. numbered - one.
 *
 * Responsibilities:
 *
 * - auditing and notifying about work item(s) creation
 * - creating timed-actions triggers (e.g. for auto-escalation) for new work items
 *
 * Work items creation itself is delegated to {@link EngineExtension#processStageOpening(CaseEngineOperation, OperationResult)}.
 */
class OpenStageAction extends InternalAction {

    private static final Trace LOGGER = TraceManager.getTrace(OpenStageAction.class);

    OpenStageAction(CaseEngineOperationImpl ctx) {
        super(ctx, LOGGER);
    }

    @Override
    public @Nullable Action executeInternal(OperationResult result) throws SchemaException {
        int currentStage = operation.getCurrentStageNumber();
        int expectedStages = operation.getExpectedNumberOfStages();
        LOGGER.trace("Current stage: {}, expected number of stages: {}", currentStage, expectedStages);

        if (currentStage >= expectedStages) {
            // This can occur e.g. if there are zero stages. Normally, though,
            // the number of stages check fires when the last stage is closed.
            return new CloseCaseAction(operation, SchemaConstants.NS_MODEL_CASES_OUTCOME_DEFAULT);
        }

        CaseType currentCase = operation.getCurrentCase();

        int stageToBe = currentStage + 1;
        currentCase.setStageNumber(stageToBe);
        LOGGER.trace("Stage number set to {}", stageToBe);

        EngineExtension engineExtension = operation.getEngineExtension();
        StageOpeningResult openingInformation = engineExtension.processStageOpening(operation, result);

        LOGGER.trace("Engine extension {} returned opening result:\n{}",
                engineExtension.getClass().getName(), openingInformation.debugDumpLazily(1));

        if (openingInformation.getAutoClosingInformation() != null) {
            LOGGER.trace("Auto-closing information present, going to close the stage immediately.");
            return new CloseStageAction(operation, openingInformation.getAutoClosingInformation());
        }

        AtomicLong idCounter = new AtomicLong(
                Objects.requireNonNullElse(currentCase.asPrismObject().getHighestId(), 0L) + 1);

        LOGGER.trace("Got {} work items to create", openingInformation.getNewWorkItems().size());
        for (CaseWorkItemType newWorkItem : openingInformation.getNewWorkItems()) {
            if (newWorkItem.getId() == null) {
                newWorkItem.setId(idCounter.getAndIncrement());
            }
            currentCase.getWorkItem().add(newWorkItem);
            prepareAuditAndNotifications(newWorkItem, result);
            createCaseTriggers(newWorkItem, openingInformation.getTimedActionsCollection());
        }

        // Now we are waiting for the work items to be completed (typically)
        return null;
    }

    private void createCaseTriggers(CaseWorkItemType workItem, Collection<WorkItemTimedActionsType> timedActions) {
        beans.triggerHelper.createTriggersForTimedActions(
                operation.getCurrentCase(),
                workItem.getId(),
                0,
                XmlTypeConverter.toDate(workItem.getCreateTimestamp()),
                XmlTypeConverter.toDate(workItem.getDeadline()),
                timedActions);
    }

    private void prepareAuditAndNotifications(CaseWorkItemType workItem, OperationResult result) {
        auditRecords.addWorkItemCreation(workItem, result);
        prepareNotifications(workItem, result);
    }

    private void prepareNotifications(CaseWorkItemType workItem, OperationResult result) {
        CaseType currentCase = getCurrentCase();
        Task task = getTask();
        try {
            List<ObjectReferenceType> assigneesAndDeputies = beans.miscHelper.getAssigneesAndDeputies(workItem, task, result);
            prepareIndividualNotifications(workItem, currentCase, assigneesAndDeputies);
            prepareCommonNotification(workItem, currentCase, assigneesAndDeputies);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't prepare notification about work item create event", e);
        }
    }

    /**
     * Notifications to individual assignees that a work item has been created for them.
     */
    private void prepareIndividualNotifications(
            CaseWorkItemType workItem, CaseType currentCase, List<ObjectReferenceType> assigneesAndDeputies) {
        for (ObjectReferenceType assigneesOrDeputy : assigneesAndDeputies) {
            // we assume originalAssigneeRef == assigneeRef in this case
            notificationEvents.add(
                    new ItemCreation(currentCase, workItem, null, null, assigneesOrDeputy));
        }
    }

    /**
     * Common notification saying that allocation of the work item has changed.
     */
    private void prepareCommonNotification(
            CaseWorkItemType workItem, CaseType currentCase, List<ObjectReferenceType> assigneesAndDeputies) {
        WorkItemAllocationChangeOperationInfo operationInfo =
                new WorkItemAllocationChangeOperationInfo(null, List.of(), assigneesAndDeputies);
        notificationEvents.add(
                new AllocationChangeNew(currentCase, workItem, operationInfo, null));
    }
}

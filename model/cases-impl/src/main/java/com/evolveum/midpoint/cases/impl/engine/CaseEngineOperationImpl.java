/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine;

import java.util.Collection;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.api.events.FutureNotificationEvent.CaseClosing;
import com.evolveum.midpoint.cases.api.extensions.EngineExtension;
import com.evolveum.midpoint.cases.api.request.Request;
import com.evolveum.midpoint.cases.impl.engine.actions.Action;
import com.evolveum.midpoint.cases.impl.engine.events.PendingAuditRecords;
import com.evolveum.midpoint.cases.impl.engine.events.PendingNotificationEvents;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.VersionPrecondition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.CaseState;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Single case engine operation: an attempt to execute a {@link Request} against a case.
 *
 * The request is converted to an {@link Action}, potentially followed by other actions stemming from it.
 *
 * The operation can be retried if there is a collision with another modification of the case in question.
 * See the note of concurrency handling in {@link CaseEngineImpl}.
 *
 * The overall principle is a bit different from the rest of midPoint: we gradually modify
 * the case, and when committing, we compute the difference from the original case, and apply to repository.
 * So no list of item deltas is maintained. See {@link #commit(OperationResult)} method.
 *
 * We maintain {@link #auditRecords} and {@link #notificationEvents}.
 * They are processed after successful commit: records are written, and notification events are produced and sent.
 */
public class CaseEngineOperationImpl implements DebugDumpable, CaseEngineOperation {

    private static final Trace LOGGER = TraceManager.getTrace(CaseEngineOperationImpl.class);

    private static final String OP_COMMIT = CaseEngineOperation.class.getName() + ".commit";

    /** The case as it was on the beginning of the operation. OID is null if the object is not yet in repo. */
    @NotNull private final CaseType originalCase;

    /** The case that is being modified by the operation. It is a clone of the original case. */
    @NotNull private final CaseType currentCase;

    /** Engine extension that provides archetype-specific functionality for this operation. */
    @NotNull private final EngineExtension engineExtension;

    /** The task under which we execute the operation. */
    @NotNull private final Task task;

    /** Useful beans. */
    @NotNull private final CaseBeans beans;

    /** Logged-in user. */
    @NotNull private final MidPointPrincipal principal;

    /** Pending audit records for the current operation. */
    @NotNull private final PendingAuditRecords auditRecords;

    /** Pending notification events for the current operation. */
    @NotNull private final PendingNotificationEvents notificationEvents;

    public CaseEngineOperationImpl(
            @NotNull CaseType originalCase,
            @NotNull EngineExtension engineExtension,
            @NotNull Task task,
            @NotNull CaseBeans beans,
            @NotNull MidPointPrincipal principal) {
        this.originalCase = originalCase;
        this.currentCase = originalCase.clone();
        this.engineExtension = engineExtension;
        this.task = task;
        this.beans = beans;
        this.principal = principal;
        this.auditRecords = new PendingAuditRecords(this);
        this.notificationEvents = new PendingNotificationEvents(this);
    }

    /**
     * Main entry point: executes the request by creating and executing the respective action.
     */
    void executeRequest(@NotNull Request request, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, ObjectAlreadyExistsException, PreconditionViolationException {
        Action action = beans.actionFactory.create(request, this);
        while (action != null) {
            action = action.execute(result);
        }
        commit(result);
    }

    private void commit(OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, PreconditionViolationException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException, SecurityViolationException {
        OperationResult result = parentResult.subresult(OP_COMMIT)
                .setMinor()
                .build();
        try {

            if (getCaseOid() == null) {
                addCaseToRepo(result);
            } else {
                modifyCaseInRepo(result); // Throws PreconditionViolationException if there's a race condition
            }

            if (CaseState.of(currentCase).isClosing()) {
                closeTheCase(result);
            }

            auditRecords.flush(result);
            notificationEvents.flush(result);
        } catch (PreconditionViolationException e) {
            result.recordNotApplicable("Concurrent repository access");
            throw e;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private void addCaseToRepo(OperationResult result) throws ObjectAlreadyExistsException, SchemaException {
        LOGGER.trace("Case is not in repo yet. Adding it there.");
        String newOid = beans.repositoryService.addObject(currentCase.asPrismObject(), null, result);
        originalCase.setOid(newOid);
    }

    private void modifyCaseInRepo(OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException {
        LOGGER.trace("Case is already in repo. Computing modifications to be applied to it.");
        ObjectDelta<CaseType> diff = originalCase.asPrismObject()
                .diff(currentCase.asPrismObject(), ParameterizedEquivalenceStrategy.DATA);
        assert diff.isModify();
        Collection<? extends ItemDelta<?, ?>> modifications = diff.getModifications();

        String caseOid = getCaseOidRequired();

        LOGGER.trace("Modifications to be applied to case {}:\n{}",
                caseOid, DebugUtil.debugDumpLazily(modifications));

        beans.repositoryService.modifyObject(
                CaseType.class,
                caseOid,
                modifications,
                new VersionPrecondition<>(originalCase.asPrismObject()),
                null,
                result);
    }

    private void closeTheCase(OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException,
            ConfigurationException, CommunicationException, SecurityViolationException {

        // Invoking specific postprocessing of the case, like submitting a task that executes approved deltas.
        engineExtension.finishCaseClosing(this, result);

        // Note that the case may still be in "closing" state. E.g. an approval case is still closing here,
        // when the execution task was immediately started, but (obviously) not finished yet.
        // TODO Do we want to fix this?
        auditRecords.addCaseClosing(result);
        notificationEvents.add(
                new CaseClosing(currentCase));
    }

    @Override
    public void closeCaseInRepository(OperationResult result) throws ObjectNotFoundException {
        beans.miscHelper.closeCaseInRepository(currentCase, result);
    }

    public @NotNull CaseType getCurrentCase() {
        return currentCase;
    }

    public @Nullable String getCaseOid() {
        return currentCase.getOid();
    }

    public @NotNull String getCaseOidRequired() {
        return Objects.requireNonNull(
                currentCase.getOid(), "No case OID");
    }

    public @NotNull Task getTask() {
        return task;
    }

    public String getChannel() {
        return task.getChannel();
    }

    public @NotNull CaseBeans getBeans() {
        return beans;
    }

    public @NotNull CaseWorkItemType getWorkItemById(long id) {
        //noinspection unchecked
        PrismContainerValue<CaseWorkItemType> workItemPcv = (PrismContainerValue<CaseWorkItemType>)
                currentCase.asPrismContainerValue().find(ItemPath.create(CaseType.F_WORK_ITEM, id));
        if (workItemPcv == null) {
            throw new IllegalStateException("No work item " + id + " in " + this);
        } else {
            return workItemPcv.asContainerable();
        }
    }

    /**
     * Returns the number of stages the case is expected to go through.
     *
     * @see EngineExtension#getExpectedNumberOfStages(CaseEngineOperation)
     */
    public int getExpectedNumberOfStages() {
        return engineExtension.getExpectedNumberOfStages(this);
    }

    /**
     * Does this case use stages?
     */
    public boolean doesUseStages() {
        return engineExtension.doesUseStages();
    }

    /**
     * Returns the current stage number, or 0 if no stage has been opened yet.
     * After the last (N-th) stage is closed, the current stage number remain to be N.
     */
    @Override
    public int getCurrentStageNumber() {
        int stage = Objects.requireNonNullElse(currentCase.getStageNumber(), 0);
        checkCurrentStage(stage);
        return stage;
    }

    private void checkCurrentStage(int stageNumber) {
        if (stageNumber < 0 || stageNumber > getExpectedNumberOfStages()) {
            LOGGER.error("Current stage is below 0 or beyond the number of stages: {}\n{}", stageNumber, debugDump());
            throw new IllegalStateException("Current stage is below 0 or beyond the number of stages: " + stageNumber);
        }
    }

    public ApprovalStageDefinitionType getCurrentStageDefinition() {
        return ApprovalContextUtil.getCurrentStageDefinition(currentCase);
    }

    public void addCaseHistoryEvent(CaseEventType event) {
        currentCase.getEvent().add(event);
    }

    // todo remove
    public WorkItemId createWorkItemId(CaseWorkItemType workItem) {
        return WorkItemId.create(getCaseOidRequired(), workItem.getId());
    }

    public @NotNull MidPointPrincipal getPrincipal() {
        return principal;
    }

    public boolean isApprovalCase() {
        return ObjectTypeUtil.hasArchetypeRef(currentCase, SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value());
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Current case", currentCase, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Pending audit records", auditRecords, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Pending notification event suppliers",
                notificationEvents, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "case=" + currentCase +
                ", audit: " + auditRecords.size() +
                ", notifications: " + notificationEvents.size() +
                '}';
    }

    @Deprecated // FIXME
    public ApprovalContextType getApprovalContext() {
        return currentCase.getApprovalContext();
    }

    public @NotNull EngineExtension getEngineExtension() {
        return engineExtension;
    }

    // TODO better name
    public @NotNull PendingAuditRecords getAuditRecords() {
        return auditRecords;
    }

    // TODO better name
    public @NotNull PendingNotificationEvents getNotificationEvents() {
        return notificationEvents;
    }
}

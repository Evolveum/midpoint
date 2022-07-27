/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.VersionPrecondition;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.engine.helpers.DelayedNotification;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 *  A context for single engine invocation attempt.
 *  (I.e. this context is created when invocation attempt starts.)
 *
 *  todo clean this up
 */
public class EngineInvocationContext implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(EngineInvocationContext.class);

    private static final String OP_COMMIT = EngineInvocationContext.class.getName() + ".commit";

    @NotNull private final CaseType originalCase;
    @NotNull private final CaseType currentCase;
    @NotNull private final Task opTask;
    @NotNull private final WorkflowEngine engine;
    @NotNull private final MidPointPrincipal principal;

    @NotNull public final List<AuditEventRecord> pendingAuditRecords = new ArrayList<>();
    @NotNull public final List<DelayedNotification> pendingNotifications = new ArrayList<>();

    private boolean wasClosed;

    public EngineInvocationContext(@NotNull CaseType originalCase, @NotNull Task opTask, @NotNull WorkflowEngine engine,
            @NotNull MidPointPrincipal principal) {
        this.originalCase = originalCase;
        this.currentCase = originalCase.clone();
        this.opTask = opTask;
        this.engine = engine;
        this.principal = principal;
    }

    public ApprovalContextType getWfContext() {
        return currentCase.getApprovalContext();
    }

    @NotNull
    public CaseType getCase() {
        return currentCase;
    }

    // TODO what's the difference between getCase() and getCurrentCase() ?
    @NotNull
    public CaseType getCurrentCase() {
        return currentCase;
    }

    @NotNull
    public Task getTask() {
        return opTask;
    }

    @Override
    public String debugDump(int indent) {
        ApprovalContextType approvalContext = currentCase.getApprovalContext();
        if (approvalContext != null) {
            return approvalContext.asPrismContainerValue().debugDump(indent);
        }
        ManualProvisioningContextType manualProvisioningContext = currentCase.getManualProvisioningContext();
        if (manualProvisioningContext != null) {
            return manualProvisioningContext.asPrismContainerValue().debugDump(indent);
        }
        return currentCase.debugDump(indent);
    }

    public String getChannel() {
        return opTask.getChannel();
    }

    @Override
    public String toString() {
        return "EngineInvocationContext{" +
                "case=" + currentCase +
                '}';
    }

    public String getCaseOid() {
        return currentCase.getOid();
    }

    @NotNull
    public CaseWorkItemType findWorkItemById(long id) {
        //noinspection unchecked
        PrismContainerValue<CaseWorkItemType> workItemPcv = (PrismContainerValue<CaseWorkItemType>)
                currentCase.asPrismContainerValue().find(ItemPath.create(CaseType.F_WORK_ITEM, id));
        if (workItemPcv == null) {
            throw new IllegalStateException("No work item " + id + " in " + this);
        } else {
            return workItemPcv.asContainerable();
        }
    }

    public String getProcessInstanceNameOrig() {
        return currentCase.getName().getOrig();
    }

    public PolyStringType getProcessInstanceName() {
        return currentCase.getName();
    }

    public void addAuditRecord(AuditEventRecord record) {
        pendingAuditRecords.add(record);
    }

    void commit(OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, PreconditionViolationException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException {
        OperationResult result = parentResult.subresult(OP_COMMIT)
                .setMinor()
                .build();
        try {
            boolean approvalCase = isApprovalCase();

            if (currentCase.getOid() == null) {
                String newOid = engine.repositoryService.addObject(currentCase.asPrismObject(), null, result);
                originalCase.setOid(newOid);
            } else {
                ObjectDelta<CaseType> diff = originalCase.asPrismObject()
                        .diff(currentCase.asPrismObject(), ParameterizedEquivalenceStrategy.DATA);
                assert diff.isModify();
                Collection<? extends ItemDelta<?, ?>> modifications = diff.getModifications();

                LOGGER.trace("Modifications to be applied to case {}:\n{}", getCaseOid(),
                        DebugUtil.debugDumpLazily(modifications));

                engine.repositoryService.modifyObject(CaseType.class, getCaseOid(), modifications,
                        new VersionPrecondition<>(originalCase.asPrismObject()), null, result);
            }

            if (wasClosed) {
                try {
                    if (approvalCase) {
                        engine.primaryChangeProcessor.onProcessEnd(this, result);
                    } else {
                        engine.executionHelper.closeCaseInRepository(currentCase, result);
                    }

                    engine.wfAuditHelper.prepareProcessEndRecord(this, result);
                    prepareNotification(new DelayedNotification.ProcessEnd(currentCase));
                } catch (PreconditionViolationException e) {
                    throw new SystemException(e);
                }
            }

            engine.wfAuditHelper.auditPreparedRecords(this, result);
            engine.notificationHelper.sendPreparedNotifications(this, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public int getNumberOfStages() {
        Integer stageCount = ApprovalContextUtil.getStageCount(getWfContext());
        if (stageCount == null) {
            LOGGER.error("Couldn't determine stage count from the workflow context\n{}", debugDump());
            throw new IllegalStateException("Couldn't determine stage count from the workflow context");
        }
        return stageCount;
    }

    public int getCurrentStage() {
        int rv = defaultIfNull(currentCase.getStageNumber(), 0);
        checkCurrentStage(rv);
        return rv;
    }

    private void checkCurrentStage(int rv) {
        if (rv < 0 || rv > getNumberOfStages()) {
            LOGGER.error("Current stage is below 0 or beyond the number of stages: {}\n{}", rv, debugDump());
            throw new IllegalStateException("Current stage is below 0 or beyond the number of stages: " + rv);
        }
    }

    public ApprovalStageDefinitionType getCurrentStageDefinition() {
        return ApprovalContextUtil.getCurrentStageDefinition(currentCase);
    }

    public boolean isAnyCurrentStageWorkItemOpen() {
        int currentStage = getCurrentStage();
        return currentCase.getWorkItem().stream()
                .anyMatch(wi -> wi.getStageNumber() != null && wi.getStageNumber() == currentStage && wi.getCloseTimestamp() == null);
    }

    public void addEvent(CaseEventType event) {
        currentCase.getEvent().add(event);
    }

    private PrismContext getPrismContext() {
        return originalCase.asPrismObject().getPrismContext();
    }

    public void updateDelta(ObjectDeltaType additionalDelta) throws SchemaException {
        PrismContext prismContext = getPrismContext();
        ApprovalContextType actx = getWfContext();
        ObjectTreeDeltasType updatedDelta = ObjectTreeDeltas.mergeDeltas(actx.getDeltasToApprove(), additionalDelta, prismContext);
        actx.setDeltasToApprove(updatedDelta);
    }

//    @NotNull
//    List<CaseWorkItemType> getOpenWorkItemsForStage(EngineInvocationContext ctx, int stage) {
//        return ctx.currentCase.getWorkItem().stream()
//                .filter(wi -> wi.getStageNumber() != null && wi.getStageNumber() == stage)
//                .filter(wi -> wi.getCloseTimestamp() == null)
//                .collect(Collectors.toList());
//    }

    @NotNull
    public List<CaseWorkItemType> getWorkItemsForStage(int stage) {
        return currentCase.getWorkItem().stream()
                .filter(wi -> wi.getStageNumber() != null && wi.getStageNumber() == stage)
                .collect(Collectors.toList());
    }


//    private boolean isClosed() {
//        return SchemaConstants.CASE_STATE_CLOSED.equals(currentCase.getState());
//    }
//
//    private boolean isWaiting() {
//        return currentCase.getWorkItem().stream().anyMatch(wi -> wi.getCloseTimestamp() == null);
//    }

    public void setWasClosed(boolean wasClosed) {
        this.wasClosed = wasClosed;
    }

    public boolean getWasClosed() {
        return wasClosed;
    }

//    public void assertNoOpenWorkItems() {
//        if (currentCase.getWorkItem().stream().anyMatch(wi -> wi.getCloseTimestamp() == null)) {
//            throw new IllegalStateException("Open work item in " + currentCase);
//        }
//    }

    // todo remove
    public WorkItemId createWorkItemId(CaseWorkItemType workItem) {
        return WorkItemId.create(getCaseOid(), workItem.getId());
    }

    @NotNull
    public MidPointPrincipal getPrincipal() {
        return principal;
    }

    public void prepareNotification(DelayedNotification notification) {
        pendingNotifications.add(notification);
    }

    //    private void logCtx(EngineInvocationContext ctx, String message, OperationResult result)
//            throws SchemaException, ObjectNotFoundException {
//        String rootOid = ctx.originalCase.getParentRef() != null ? ctx.originalCase.getParentRef().getOid() : ctx.originalCase.getOid();
//        CaseType rootCase = repositoryService.getObject(CaseType.class, rootOid, null, result).asObjectable();
//        LOGGER.trace("###### [ {} ] ######", message);
//        LOGGER.trace("Root case:\n{}", modelHelper.dumpCase(rootCase));
//        for (CaseType subcase : miscHelper.getSubcases(rootCase, result)) {
//            LOGGER.trace("Subcase:\n{}", modelHelper.dumpCase(subcase));
//        }
//        LOGGER.trace("###### [ END OF {} ] ######", message);
//    }

    @NotNull
    public WorkflowEngine getEngine() {
        return engine;
    }

    public boolean isApprovalCase() {
        return ObjectTypeUtil.hasArchetype(currentCase, SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value());
    }
}

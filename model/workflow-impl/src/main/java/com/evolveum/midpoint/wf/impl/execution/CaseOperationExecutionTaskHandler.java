/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.execution;

import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.primary.ApprovalMetadataHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpGeneralHelper;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

import static java.util.Collections.singletonList;

/**
 * Executes approved changes from a case. Expects task.objectRef to point to the particular (root or partial) case.
 */
@Component
public class CaseOperationExecutionTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(CaseOperationExecutionTaskHandler.class);

    private static final String DOT_CLASS = CaseOperationExecutionTaskHandler.class.getName() + ".";

    public static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/workflow/operation-execution/handler-3";

    @Autowired private TaskManager taskManager;
    @Autowired private ApprovalMetadataHelper metadataHelper;
    @Autowired private Clockwork clockwork;
    @Autowired private MiscHelper miscHelper;
    @Autowired private PcpGeneralHelper pcpGeneralHelper;
    @Autowired private ApprovalMetadataHelper approvalMetadataHelper;
    @Autowired private ExecutionHelper executionHelper;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;
    @Autowired private LensContextHelper lensContextHelper;

    @Override
    public TaskRunResult run(@NotNull RunningTask task) {
        OperationResult result = task.getResult().createSubresult(DOT_CLASS + "run");
        TaskRunResult runResult = new TaskRunResult();
        try {
            PrismObject<CaseType> caseObject = task.getObject(CaseType.class, result);
            if (caseObject == null) {
                throw new IllegalStateException("No case reference in task " + task);
            }
            CaseType aCase = caseObject.asObjectable();
            if (aCase.getParentRef() == null) {
                executeAllChanges(aCase, task, result);
            } else {
                executeLocalChanges(aCase, task, result);
            }
            result.computeStatus();
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
        } catch (RuntimeException | ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException |
                ExpressionEvaluationException | PolicyViolationException | PreconditionViolationException |
                ObjectAlreadyExistsException | SecurityViolationException e) {
            String message = "An exception occurred when trying to execute model operation for a case in " + task;
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            result.recordFatalError(message, e);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR); // let's assume it's temporary
        }
        task.getResult().recomputeStatus();
        runResult.setOperationResultStatus(task.getResult().getStatus());
        return runResult;
    }

    private void executeLocalChanges(CaseType subcase, RunningTask task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException,
            CommunicationException, PolicyViolationException, PreconditionViolationException, ObjectAlreadyExistsException,
            SecurityViolationException {
        CaseType rootCase = repositoryService.getObject(CaseType.class, subcase.getParentRef().getOid(), null, result)
                .asObjectable();
        LensContext<?> modelContext = miscHelper.getModelContext(rootCase, task, result);
        ObjectTreeDeltas<?> deltas = pcpGeneralHelper.retrieveResultingDeltas(subcase);
        if (deltas == null) {
            throw new IllegalStateException("No deltas to be executed in " + subcase);
        }
        ObjectDelta<?> focusChange = deltas.getFocusChange();
        if (focusChange != null) {
            approvalMetadataHelper.addAssignmentApprovalMetadata(focusChange, subcase, task, result);
        }
        lensContextHelper.mergeDeltasToModelContext(modelContext, singletonList(deltas));
        executeModelContext(modelContext, subcase, task, result);
        executionHelper.closeCaseInRepository(subcase, result);
        executionHelper.checkDependentCases(subcase.getParentRef().getOid(), result);
    }

    private void executeAllChanges(CaseType rootCase, RunningTask task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, PolicyViolationException, PreconditionViolationException, ObjectAlreadyExistsException,
            SecurityViolationException {
        List<CaseType> subcases = miscHelper.getSubcases(rootCase, result);
        LensContext<?> modelContext = lensContextHelper.collectApprovedDeltasToModelContext(rootCase, subcases, task, result);
        executeModelContext(modelContext, rootCase, task, result);
        executionHelper.closeCaseInRepository(rootCase, result);
    }

    private void executeModelContext(LensContext<?> modelContext, CaseType aCase, RunningTask task, OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        if (!modelContext.hasAnyPrimaryChange()) {
            LOGGER.trace("No primary changes -- nothing to do here");
            return;
        }
        modelContext.deleteNonTransientComputationResults();
        LOGGER.trace("Context to be executed:\n{}", modelContext.debugDumpLazily(1));

        modelContext.getOperationApprovedBy().addAll(metadataHelper.getAllApprovers(aCase, result));
        modelContext.getOperationApproverComments().addAll(metadataHelper.getAllApproverComments(aCase, task, result));

        // here we request to brutally remove all the projection contexts except for these with primary/sync deltas -- because
        // if we are continuing after rejection of a role/resource assignment that resulted in such projection contexts,
        // we DO NOT want them to appear in the context any more
        modelContext.rot("restart after approvals");
        Iterator<LensProjectionContext> projectionIterator = modelContext.getProjectionContextsIterator();
        while (projectionIterator.hasNext()) {
            LensProjectionContext projectionContext = projectionIterator.next();
            if (!ObjectDelta.isEmpty(projectionContext.getPrimaryDelta()) ||
                    !ObjectDelta.isEmpty(projectionContext.getSyncDelta())) {
                continue; // don't remove client requested or externally triggered actions!
            }
            LOGGER.trace("Removing projection context {}", projectionContext.getHumanReadableName());
            projectionIterator.remove();
        }
        if (task.getChannel() == null) {
            task.setChannel(modelContext.getChannel());
        }
        clockwork.run(modelContext, task, result);
    }

    @Override
    public String getArchetypeOid(@Nullable String handlerUri) {
        return SystemObjectsType.ARCHETYPE_APPROVAL_TASK.value();
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }
}

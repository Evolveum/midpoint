/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.evolveum.midpoint.schema.ObjectTreeDeltas.fromObjectTreeDeltasType;

/**
 * Deals with sorting out changes into categories (see ChangesByState).
 * Should be reworked after changing workflows from tasks to cases.
 */
@Component
public class ChangesSorter {

    private static final Trace LOGGER = TraceManager.getTrace(ChangesSorter.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;

    private static class TaskHolder {
        boolean initialized = false;
        TaskType task;
    }

    public ChangesByState<?> getChangesByStateForChild(CaseType approvalCase, CaseType rootCase, PrismContext prismContext,
            OperationResult result) throws SchemaException {
        ChangesByState<?> rv = new ChangesByState(prismContext);
        TaskHolder rootTaskHolder = new TaskHolder();
        recordChangesFromApprovalCase(rv, approvalCase, rootCase, prismContext, rootTaskHolder, result);
        return rv;
    }

    private void recordChangesFromApprovalCase(ChangesByState<?> rv, CaseType approvalCase, CaseType rootCase,
            PrismContext prismContext, TaskHolder rootTaskHolder, OperationResult result) throws SchemaException {
        ApprovalContextType actx = approvalCase.getApprovalContext();
        if (actx != null) {
            boolean hasApprovalSchema = actx.getApprovalSchema() != null;
            Boolean isApproved = ApprovalUtils.approvalBooleanValueFromUri(approvalCase.getOutcome());
            if (hasApprovalSchema && isApproved == null) {
                if (approvalCase.getCloseTimestamp() == null) {
                    recordChangesWaitingToBeApproved(rv, actx, prismContext);
                } else {
                    recordChangesCanceled(rv, actx, prismContext);
                }
            } else if (!hasApprovalSchema || isApproved) {
                TaskType executionTask;
                if (Boolean.TRUE.equals(approvalCase.getApprovalContext().isImmediateExecution())) {
                    executionTask = getExecutionTask(approvalCase, result);
                } else {
                    if (!rootTaskHolder.initialized) {
                        rootTaskHolder.task = getExecutionTask(rootCase, result);
                        rootTaskHolder.initialized = true;
                    }
                    executionTask = rootTaskHolder.task;
                }
                if (executionTask == null || executionTask.getExecutionStatus() == TaskExecutionStatusType.WAITING) {
                    recordResultingChanges(rv.getWaitingToBeApplied(), actx, prismContext);
                } else if (executionTask.getExecutionStatus() == TaskExecutionStatusType.RUNNABLE) {
                    recordResultingChanges(rv.getBeingApplied(), actx, prismContext);
                } else {
                    // note: the task might be suspended here
                    recordResultingChanges(rv.getApplied(), actx, prismContext);
                }
            } else {
                recordChangesRejected(rv, actx, prismContext);
            }
        } else {
            LOGGER.warn("Approval case with no approval context?\n{}", approvalCase.asPrismObject().debugDump());
        }
    }

    public ChangesByState getChangesByStateForRoot(CaseType rootCase, PrismContext prismContext, OperationResult result)
            throws SchemaException {
        ChangesByState rv = new ChangesByState(prismContext);
        TaskHolder rootTaskHolder = new TaskHolder();
        for (CaseType subcase : getSubcases(rootCase, result)) {
            recordChangesFromApprovalCase(rv, subcase, rootCase, prismContext, rootTaskHolder, result);
        }
        return rv;
    }

    private List<CaseType> getSubcases(CaseType aCase, OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_PARENT_REF).ref(aCase.getOid())
                .build();
        SearchResultList<PrismObject<CaseType>> subcases = repositoryService
                .searchObjects(CaseType.class, query, null, result);
        return ObjectTypeUtil.asObjectables(subcases);
    }

    private TaskType getExecutionTask(CaseType aCase, OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(aCase.getOid())
                .build();
        SearchResultList<PrismObject<TaskType>> tasks = repositoryService
                .searchObjects(TaskType.class, query, null, result);
        if (tasks.isEmpty()) {
            return null;
        }
        if (tasks.size() > 1) {
            LOGGER.warn("More than one task found for case {} ({} in total): taking an arbitrary one", aCase, tasks.size());
        }
        return tasks.get(0).asObjectable();
    }

    private void recordChangesWaitingToBeApproved(ChangesByState rv, ApprovalContextType wfc, PrismContext prismContext)
            throws SchemaException {
        //noinspection unchecked
        rv.getWaitingToBeApproved().mergeUnordered(fromObjectTreeDeltasType(wfc.getDeltasToApprove(), prismContext));
    }

    private void recordChangesCanceled(ChangesByState rv, ApprovalContextType wfc, PrismContext prismContext)
            throws SchemaException {
        //noinspection unchecked
        rv.getCanceled().mergeUnordered(fromObjectTreeDeltasType(wfc.getDeltasToApprove(), prismContext));
    }

    private void recordChangesRejected(ChangesByState rv, ApprovalContextType wfc, PrismContext prismContext) throws SchemaException {
        if (ObjectTreeDeltas.isEmpty(wfc.getResultingDeltas())) {
            //noinspection unchecked
            rv.getRejected().mergeUnordered(fromObjectTreeDeltasType(wfc.getDeltasToApprove(), prismContext));
        } else {
            // it's actually hard to decide what to display as 'rejected' - because the delta was partly approved
            // however, this situation will not currently occur
        }
    }

    private void recordResultingChanges(ObjectTreeDeltas<?> target, ApprovalContextType wfc, PrismContext prismContext) throws SchemaException {
        //noinspection unchecked
        target.mergeUnordered(fromObjectTreeDeltasType(wfc.getResultingDeltas(), prismContext));
    }
}

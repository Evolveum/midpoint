/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.util;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.util.ModelContextUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
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

    private static final transient Trace LOGGER = TraceManager.getTrace(ChangesSorter.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;

	//region ========================================================================== Miscellaneous

	// TODO move somewhere else?
	public ChangesByState<?> getChangesByStateForChild(CaseType approvalCase, CaseType rootCase, PrismContext prismContext,
			OperationResult result) throws SchemaException {
		ChangesByState<?> rv = new ChangesByState(prismContext);

		final ApprovalContextType wfc = approvalCase.getApprovalContext();
		if (wfc != null) {
			Boolean isApproved = ApprovalUtils.approvalBooleanValueFromUri(approvalCase.getOutcome());
			if (isApproved == null) {
				if (approvalCase.getCloseTimestamp() == null) {
					recordChangesWaitingToBeApproved(rv, wfc, prismContext);
				} else {
					recordChangesCanceled(rv, wfc, prismContext);
				}
			} else if (isApproved) {
				TaskType executionTask;
				if (Boolean.TRUE.equals(approvalCase.getApprovalContext().isImmediateExecution())) {
					executionTask = getExecutionTask(approvalCase, result);
				} else {
					executionTask = getExecutionTask(rootCase, result);
				}
				if (executionTask == null || executionTask.getExecutionStatus() == TaskExecutionStatusType.WAITING) {
					recordResultingChanges(rv.getWaitingToBeApplied(), wfc, prismContext);
				} else if (executionTask.getExecutionStatus() == TaskExecutionStatusType.RUNNABLE) {
					recordResultingChanges(rv.getBeingApplied(), wfc, prismContext);
				} else {
					// note: the task might be suspended here
					recordResultingChanges(rv.getApplied(), wfc, prismContext);
				}
			} else {
				recordChangesRejected(rv, wfc, prismContext);
			}
		}
		return rv;
	}

	// TODO move somewhere else?
	public ChangesByState getChangesByStateForRoot(CaseType rootCase, ModelInteractionService modelInteractionService, PrismContext prismContext, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		ChangesByState rv = new ChangesByState(prismContext);
		LensContextType rootModelContext = getModelContext(rootCase, result);
		recordChanges(rv, rootModelContext, modelInteractionService, task, result);
		for (CaseType subcase : getSubcases(rootCase, result)) {
			ApprovalContextType actx = subcase.getApprovalContext();
			if (actx != null) {
				Boolean isApproved = ApprovalUtils.approvalBooleanValueFromUri(subcase.getOutcome());
				if (isApproved == null) {
					if (subcase.getCloseTimestamp() == null) {
						recordChangesWaitingToBeApproved(rv, actx, prismContext);
					} else {
						recordChangesCanceled(rv, actx, prismContext);
					}
				} else if (isApproved) {
					if (Boolean.TRUE.equals(actx.isImmediateExecution())) {
						LensContextType subModelContext = getModelContext(subcase, result);
						recordChanges(rv, subModelContext, modelInteractionService, task, result);
					} else {
						recordResultingChanges(rv.getWaitingToBeApplied(), actx, prismContext);
					}
				} else {
					recordChangesRejected(rv, actx, prismContext);
				}
			}
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

	private LensContextType getModelContext(CaseType aCase, OperationResult result) throws SchemaException {
		TaskType executionTask = getExecutionTask(aCase, result);
		return executionTask != null ? executionTask.getModelOperationContext() : null;
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

	@SuppressWarnings("unchecked")
	private <O extends ObjectType> void recordChanges(ChangesByState rv, LensContextType modelOperationContext, ModelInteractionService modelInteractionService,
			Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (modelOperationContext == null) {
			return;
		}
		ModelContext<O> modelContext = ModelContextUtil.unwrapModelContext(modelOperationContext, modelInteractionService, task, result);
		ObjectTreeDeltas<O> deltas = modelContext.getTreeDeltas();
		ObjectTreeDeltas<O> target;
		switch (modelContext.getState()) {
			case INITIAL:
			case PRIMARY: target = rv.getWaitingToBeApplied(); break;
			case SECONDARY: target = rv.getBeingApplied(); break;
			case EXECUTION:	// TODO reconsider this after EXECUTION and POSTEXECUTION states are really used
			case POSTEXECUTION:
			case FINAL: target = rv.getApplied(); break;
			default: throw new IllegalStateException("Illegal model state: " + modelContext.getState());
		}
		target.merge(deltas);
	}

	private void recordChangesWaitingToBeApproved(ChangesByState rv, ApprovalContextType wfc, PrismContext prismContext)
			throws SchemaException {
		//noinspection unchecked
		rv.getWaitingToBeApproved().merge(fromObjectTreeDeltasType(wfc.getDeltasToApprove(), prismContext));
	}

	private void recordChangesCanceled(ChangesByState rv, ApprovalContextType wfc, PrismContext prismContext)
			throws SchemaException {
		//noinspection unchecked
		rv.getCanceled().merge(fromObjectTreeDeltasType(wfc.getDeltasToApprove(), prismContext));
	}

	private void recordChangesRejected(ChangesByState rv, ApprovalContextType wfc, PrismContext prismContext) throws SchemaException {
		if (ObjectTreeDeltas.isEmpty(wfc.getResultingDeltas())) {
			//noinspection unchecked
			rv.getRejected().merge(fromObjectTreeDeltasType(wfc.getDeltasToApprove(), prismContext));
		} else {
			// it's actually hard to decide what to display as 'rejected' - because the delta was partly approved
			// however, this situation will not currently occur
		}
	}

	private void recordResultingChanges(ObjectTreeDeltas<?> target, ApprovalContextType wfc, PrismContext prismContext) throws SchemaException {
		//noinspection unchecked
		target.merge(fromObjectTreeDeltasType(wfc.getResultingDeltas(), prismContext));
	}
}

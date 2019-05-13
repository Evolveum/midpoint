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
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.ModelHelper;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

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
	@Autowired private ModelHelper modelHelper;

	//region ========================================================================== Miscellaneous

	// TODO move somewhere else?
	public ChangesByState getChangesByStateForChild(TaskType childTask, TaskType rootTask, ModelInteractionService modelInteractionService, PrismContext prismContext, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		ChangesByState rv = new ChangesByState(prismContext);

		// TODO implement this!

//		final WfContextType wfc = childTask.getWorkflowContext();
//		if (wfc != null) {
//			Boolean isApproved = ApprovalUtils.approvalBooleanValueFromUri(wfc.getOutcome());
//			if (isApproved == null) {
//				if (wfc.getEndTimestamp() == null) {
//					recordChangesWaitingToBeApproved(rv, wfc, prismContext);
//				} else {
//					recordChangesCanceled(rv, wfc, prismContext);
//				}
//			} else if (isApproved) {
//				if (rootTask.getModelOperationContext() != null) {
//					// this is "execute after all approvals"
//					if (rootTask.getModelOperationContext().getState() == ModelStateType.FINAL) {
//						recordResultingChanges(rv.getApplied(), wfc, prismContext);
//					} else if (!containsHandler(rootTask, WfPrepareRootOperationTaskHandler.HANDLER_URI)) {
//						recordResultingChanges(rv.getBeingApplied(), wfc, prismContext);
//					} else {
//						recordResultingChanges(rv.getWaitingToBeApplied(), wfc, prismContext);
//					}
//				} else {
//					// "execute immediately"
//					if (childTask.getModelOperationContext() == null || childTask.getModelOperationContext().getState() == ModelStateType.FINAL) {
//						recordResultingChanges(rv.getApplied(), wfc, prismContext);
//					} else if (!containsHandler(childTask, WfPrepareChildOperationTaskHandler.HANDLER_URI)) {
//						recordResultingChanges(rv.getBeingApplied(), wfc, prismContext);
//					} else {
//						recordResultingChanges(rv.getWaitingToBeApplied(), wfc, prismContext);
//					}
//				}
//			} else {
//				recordChangesRejected(rv, wfc, prismContext);
//			}
//		}
		return rv;
	}

	// TODO move somewhere else?
	public ChangesByState getChangesByStateForRoot(TaskType rootTask, ModelInteractionService modelInteractionService, PrismContext prismContext, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		ChangesByState rv = new ChangesByState(prismContext);
		recordChanges(rv, rootTask.getModelOperationContext(), modelInteractionService, task, result);
		for (TaskType subtask : rootTask.getSubtask()) {
			recordChanges(rv, subtask.getModelOperationContext(), modelInteractionService, task, result);
			final WfContextType wfc = subtask.getWorkflowContext();
			if (wfc != null) {
				Boolean isApproved = null; // todo take outcome from CaseType! ApprovalUtils.approvalBooleanValueFromUri(wfc.getOutcome());
				if (isApproved == null) {
					if (true /* TODO wfc.getEndTimestamp() == null*/ ) {
						recordChangesWaitingToBeApproved(rv, wfc, prismContext);
					} else {
						recordChangesCanceled(rv, wfc, prismContext);
					}
				} else if (isApproved) {
					recordChangesApprovedIfNeeded(rv, subtask, rootTask, prismContext);
				} else {
					recordChangesRejected(rv, wfc, prismContext);
				}
			}
		}
		return rv;
	}

	private void recordChangesApprovedIfNeeded(ChangesByState rv, TaskType subtask, TaskType rootTask, PrismContext prismContext) throws SchemaException {
    	// TODO implement this!
//		if (!containsHandler(rootTask, WfPrepareRootOperationTaskHandler.HANDLER_URI) &&
//				!containsHandler(subtask, WfPrepareChildOperationTaskHandler.HANDLER_URI)) {
//			return;			// these changes were already incorporated into one of model contexts
//		}
//		if (subtask.getWorkflowContext().getProcessorSpecificState() instanceof WfPrimaryChangeProcessorStateType) {
//			WfPrimaryChangeProcessorStateType ps = (WfPrimaryChangeProcessorStateType) subtask.getWorkflowContext().getProcessorSpecificState();
//			rv.getWaitingToBeApplied().merge(fromObjectTreeDeltasType(ps.getResultingDeltas(), prismContext));
//		}
	}

	private boolean containsHandler(TaskType taskType, String handlerUri) {
		if (handlerUri.equals(taskType.getHandlerUri())) {
			return true;
		}
		if (taskType.getOtherHandlersUriStack() == null) {
			return false;
		}
		for (UriStackEntry uriStackEntry : taskType.getOtherHandlersUriStack().getUriStackEntry()) {
			if (handlerUri.equals(uriStackEntry.getHandlerUri())) {
				return true;
			}
		}
		return false;
	}

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

	protected void recordChangesWaitingToBeApproved(ChangesByState rv, WfContextType wfc, PrismContext prismContext)
			throws SchemaException {
		if (wfc.getProcessorSpecificState() instanceof WfPrimaryChangeProcessorStateType) {
			WfPrimaryChangeProcessorStateType ps = (WfPrimaryChangeProcessorStateType) wfc.getProcessorSpecificState();
			rv.getWaitingToBeApproved().merge(fromObjectTreeDeltasType(ps.getDeltasToProcess(), prismContext));
		}
	}

	protected void recordChangesCanceled(ChangesByState rv, WfContextType wfc, PrismContext prismContext)
			throws SchemaException {
		if (wfc.getProcessorSpecificState() instanceof WfPrimaryChangeProcessorStateType) {
			WfPrimaryChangeProcessorStateType ps = (WfPrimaryChangeProcessorStateType) wfc.getProcessorSpecificState();
			rv.getCanceled().merge(fromObjectTreeDeltasType(ps.getDeltasToProcess(), prismContext));
		}
	}

	private void recordChangesRejected(ChangesByState rv, WfContextType wfc, PrismContext prismContext) throws SchemaException {
		if (wfc.getProcessorSpecificState() instanceof WfPrimaryChangeProcessorStateType) {
			WfPrimaryChangeProcessorStateType ps = (WfPrimaryChangeProcessorStateType) wfc.getProcessorSpecificState();
			if (ObjectTreeDeltas.isEmpty(ps.getResultingDeltas())) {
				rv.getRejected().merge(fromObjectTreeDeltasType(ps.getDeltasToProcess(), prismContext));
			} else {
				// it's actually hard to decide what to display as 'rejected' - because the delta was partly approved
				// however, this situation will not currently occur
			}
		}
	}

	private void recordResultingChanges(ObjectTreeDeltas<?> target, WfContextType wfc, PrismContext prismContext) throws SchemaException {
		if (wfc.getProcessorSpecificState() instanceof WfPrimaryChangeProcessorStateType) {
			WfPrimaryChangeProcessorStateType ps = (WfPrimaryChangeProcessorStateType) wfc.getProcessorSpecificState();
			target.merge(fromObjectTreeDeltasType(ps.getResultingDeltas(), prismContext));
		}
	}

}

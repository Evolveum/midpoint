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
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.OidUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.processors.BaseModelInvocationProcessingHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.evolveum.midpoint.prism.delta.ChangeType.ADD;
import static com.evolveum.midpoint.schema.ObjectTreeDeltas.fromObjectTreeDeltasType;

/**
 * @author mederly
 */

@Component
public class MiscDataUtil {

    private static final transient Trace LOGGER = TraceManager.getTrace(MiscDataUtil.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
	@Autowired private TaskManager taskManager;
	@Autowired private WfConfiguration wfConfiguration;
	@Autowired private BaseModelInvocationProcessingHelper baseModelInvocationProcessingHelper;
	@Autowired private PrimaryChangeProcessor primaryChangeProcessor;

	//region ========================================================================== Miscellaneous


	public PrismObject resolveObjectReference(ObjectReferenceType ref, OperationResult result) {
		return resolveObjectReference(ref, false, result);
	}

	public PrismObject resolveAndStoreObjectReference(ObjectReferenceType ref, OperationResult result) {
		return resolveObjectReference(ref, true, result);
	}

    private PrismObject resolveObjectReference(ObjectReferenceType ref, boolean storeBack, OperationResult result) {
        if (ref == null) {
            return null;
        }
		if (ref.asReferenceValue().getObject() != null) {
			return ref.asReferenceValue().getObject();
		}
        try {
            PrismObject object = repositoryService.getObject((Class) prismContext.getSchemaRegistry().getCompileTimeClass(ref.getType()), ref.getOid(), null, result);
			if (storeBack) {
				ref.asReferenceValue().setObject(object);
			}
            return object;
        } catch (ObjectNotFoundException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get reference {} details because it couldn't be found", e, ref);
            return null;
        } catch (SchemaException e) {
            // there should be a note in result by now
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get reference {} details due to schema exception", e, ref);
            return null;
        }
    }

    public ObjectReferenceType resolveObjectReferenceName(ObjectReferenceType ref, OperationResult result) {
        if (ref == null || ref.getTargetName() != null) {
            return ref;
        }
        PrismObject<?> object;
        if (ref.asReferenceValue().getObject() != null) {
            object = ref.asReferenceValue().getObject();
        } else {
            object = resolveObjectReference(ref, result);
            if (object == null) {
                return ref;
            }
        }
        ref = ref.clone();
        ref.setTargetName(PolyString.toPolyStringType(object.getName()));
        return ref;
    }

	public void generateFocusOidIfNeeded(ModelContext<?> modelContext, ObjectDelta<? extends ObjectType> change) {
		if (modelContext.getFocusContext().getOid() != null) {
			return;
		}

		String newOid = OidUtil.generateOid();
		LOGGER.trace("This is ADD operation with no focus OID provided. Generated new OID to be used: {}", newOid);
		if (change.getChangeType() != ADD) {
			throw new IllegalStateException("Change type is not ADD for no-oid focus situation: " + change);
		} else if (change.getObjectToAdd() == null) {
			throw new IllegalStateException("Object to add is null for change: " + change);
		} else if (change.getObjectToAdd().getOid() != null) {
			throw new IllegalStateException("Object to add has already an OID present: " + change);
		}
		change.getObjectToAdd().setOid(newOid);
		((LensFocusContext<?>) modelContext.getFocusContext()).setOid(newOid);
	}

	public void generateProjectionOidIfNeeded(ModelContext<?> modelContext, ShadowType shadow, ResourceShadowDiscriminator rsd) {
		if (shadow.getOid() != null) {
			return;
		}
		String newOid = OidUtil.generateOid();
		LOGGER.trace("This is ADD operation with no shadow OID for {} provided. Generated new OID to be used: {}", rsd, newOid);
		shadow.setOid(newOid);
		LensProjectionContext projCtx = ((LensProjectionContext) modelContext.findProjectionContext(rsd));
		if (projCtx == null) {
			throw new IllegalStateException("No projection context for " + rsd + " could be found");
		} else if (projCtx.getOid() != null) {
			throw new IllegalStateException("No projection context for " + rsd + " has already an OID: " + projCtx.getOid());
		}
		projCtx.setOid(newOid);
	}

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
		ObjectTreeDeltas<O> deltas = baseModelInvocationProcessingHelper.extractTreeDeltasFromModelContext(modelContext);
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

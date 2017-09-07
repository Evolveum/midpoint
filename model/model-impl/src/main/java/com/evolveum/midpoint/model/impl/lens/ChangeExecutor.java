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

package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.FOCUS_OPERATION;
import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.RESOURCE_OBJECT_OPERATION;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;
import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.SynchronizationUtils;
import com.evolveum.midpoint.repo.api.ConflictWatcher;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.projector.FocusConstraintsChecker;
import com.evolveum.midpoint.model.impl.lens.projector.PolicyRuleProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.CredentialsProcessor;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * @author semancik
 */
@Component
public class ChangeExecutor {

	private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutor.class);

	private static final String OPERATION_EXECUTE_DELTA = ChangeExecutor.class.getName() + ".executeDelta";
	private static final String OPERATION_EXECUTE = ChangeExecutor.class.getName() + ".execute";
	private static final String OPERATION_EXECUTE_FOCUS = OPERATION_EXECUTE + ".focus";
	private static final String OPERATION_EXECUTE_PROJECTION = OPERATION_EXECUTE + ".projection";
	private static final String OPERATION_LINK_ACCOUNT = ChangeExecutor.class.getName() + ".linkShadow";
	private static final String OPERATION_UNLINK_ACCOUNT = ChangeExecutor.class.getName() + ".unlinkShadow";
	private static final String OPERATION_UPDATE_SITUATION_ACCOUNT = ChangeExecutor.class.getName()
			+ ".updateSituationInShadow";

	@Autowired
	private transient TaskManager taskManager;

	@Autowired
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;

	@Autowired
	private ProvisioningService provisioning;

	@Autowired
	private PrismContext prismContext;

	@Autowired
	private ExpressionFactory expressionFactory;

	@Autowired
	private SecurityEnforcer securityEnforcer;

	@Autowired
	private Clock clock;

	@Autowired
	private ModelObjectResolver objectResolver;

	@Autowired
	private OperationalDataManager metadataManager;

	@Autowired
	private PolicyRuleProcessor policyRuleProcessor;

	@Autowired
	private CredentialsProcessor credentialsProcessor;

	private PrismObjectDefinition<UserType> userDefinition = null;
	private PrismObjectDefinition<ShadowType> shadowDefinition = null;

	@PostConstruct
	private void locateDefinitions() {
		userDefinition = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(UserType.class);
		shadowDefinition = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(ShadowType.class);
	}

	// returns true if current operation has to be restarted, see
	// ObjectAlreadyExistsException handling (TODO specify more exactly)
	public <O extends ObjectType> boolean executeChanges(LensContext<O> context, Task task,
			OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException,
					SchemaException, CommunicationException, ConfigurationException,
					SecurityViolationException, ExpressionEvaluationException {

		OperationResult result = parentResult.createSubresult(OPERATION_EXECUTE);

		// FOCUS

		context.checkAbortRequested();

		LensFocusContext<O> focusContext = context.getFocusContext();
		if (focusContext != null) {
			ObjectDelta<O> focusDelta = focusContext.getWaveExecutableDelta(context.getExecutionWave());

			if (!focusContext.isDelete()) {
				focusDelta = policyRuleProcessor.applyAssignmentSituation(context, focusDelta);
			}

			if (focusDelta != null) {

				ObjectPolicyConfigurationType objectPolicyConfigurationType = focusContext
						.getObjectPolicyConfigurationType();
				applyObjectPolicy(focusContext, focusDelta, objectPolicyConfigurationType);

				OperationResult subResult = result.createSubresult(
						OPERATION_EXECUTE_FOCUS + "." + focusContext.getObjectTypeClass().getSimpleName());

				try {
					// Will remove credential deltas or hash them
					focusDelta = credentialsProcessor.transformFocusExectionDelta(context, focusDelta);
				} catch (EncryptionException e) {
					recordFatalError(subResult, result, null, e);
					result.computeStatus();
					throw new SystemException(e.getMessage(), e);
				}
				try {
					context.reportProgress(new ProgressInformation(FOCUS_OPERATION, ENTERING));
					executeDelta(focusDelta, focusContext, context, null, null, task, subResult);
					if (focusDelta.isAdd() && focusDelta.getOid() != null) {
						ConflictWatcher watcher = context.createAndRegisterConflictWatcher(focusDelta.getOid(), cacheRepositoryService);
						watcher.setExpectedVersion(focusDelta.getObjectToAdd().getVersion());
					}
					subResult.computeStatus();

				} catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | RuntimeException e) {
					recordFatalError(subResult, result, null, e);
					throw e;
				} catch (ObjectAlreadyExistsException e) {
					subResult.computeStatus();
					if (!subResult.isSuccess() && !subResult.isHandledError()) {
						subResult.recordFatalError(e);
					}
					result.computeStatusComposite();
					throw e;
				} finally {
					context.reportProgress(new ProgressInformation(FOCUS_OPERATION, subResult));
				}
			} else {
				LOGGER.trace("Skipping focus change execute, because user delta is null");
			}
		}

		// PROJECTIONS

		context.checkAbortRequested();

		boolean restartRequested = false;

		for (LensProjectionContext projCtx : context.getProjectionContexts()) {
			if (projCtx.getWave() != context.getExecutionWave()) {
				continue;
			}

			if (!projCtx.isCanProject()) {
				continue;
			}

			// we should not get here, but just to be sure
			if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
				LOGGER.trace("Skipping ignored projection context {}", projCtx.toHumanReadableString());
				continue;
			}

			OperationResult subResult = result.createSubresult(
					OPERATION_EXECUTE_PROJECTION + "." + projCtx.getObjectTypeClass().getSimpleName());
			subResult.addContext("discriminator", projCtx.getResourceShadowDiscriminator());
			if (projCtx.getResource() != null) {
				subResult.addParam("resource", projCtx.getResource().getName());
			}
			try {

				context.checkAbortRequested();

				context.reportProgress(new ProgressInformation(RESOURCE_OBJECT_OPERATION,
						projCtx.getResourceShadowDiscriminator(), ENTERING));

				executeReconciliationScript(projCtx, context, BeforeAfterType.BEFORE, task, subResult);

				ObjectDelta<ShadowType> projDelta = projCtx.getExecutableDelta();

				if (shouldBeDeleted(projDelta, projCtx)) {
					projDelta = ObjectDelta.createDeleteDelta(projCtx.getObjectTypeClass(), projCtx.getOid(),
							prismContext);
				}

				if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
					if (context.getFocusContext() != null
							&& context.getFocusContext().getDelta() != null
							&& context.getFocusContext().getDelta().isDelete()
							&& context.getOptions() != null
							&& ModelExecuteOptions.isForce(context.getOptions())) {
						if (projDelta == null) {
							projDelta = ObjectDelta.createDeleteDelta(projCtx.getObjectTypeClass(),
									projCtx.getOid(), prismContext);
						}
					}
					if (projDelta != null && projDelta.isDelete()) {

						executeDelta(projDelta, projCtx, context, null, projCtx.getResource(), task,
								subResult);

					}
				} else {

					if (projDelta == null || projDelta.isEmpty()) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("No change for " + projCtx.getResourceShadowDiscriminator());
						}
						if (focusContext != null) {
							updateLinks(focusContext, projCtx, task, subResult);
						}

						// Make sure post-reconcile delta is always executed,
						// even if there is no change
						executeReconciliationScript(projCtx, context, BeforeAfterType.AFTER, task,
								subResult);

						subResult.computeStatus();
						subResult.recordNotApplicableIfUnknown();
						continue;

					} else if (projDelta.isDelete() && projCtx.getResourceShadowDiscriminator() != null
							&& projCtx.getResourceShadowDiscriminator().getOrder() > 0) {
						// HACK ... for higher-order context check if this was
						// already deleted
						LensProjectionContext lowerOrderContext = LensUtil.findLowerOrderContext(context,
								projCtx);
						if (lowerOrderContext != null && lowerOrderContext.isDelete()) {
							// We assume that this was already executed
							subResult.setStatus(OperationResultStatus.NOT_APPLICABLE);
							continue;
						}
					}

					executeDelta(projDelta, projCtx, context, null, projCtx.getResource(), task, subResult);

				}

				subResult.computeStatus();
				if (focusContext != null) {
					updateLinks(focusContext, projCtx, task, subResult);
				}

				executeReconciliationScript(projCtx, context, BeforeAfterType.AFTER, task, subResult);

				subResult.computeStatus();
				subResult.recordNotApplicableIfUnknown();

			} catch (SchemaException e) {
				recordProjectionExecutionException(e, projCtx, subResult,
						SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (ObjectNotFoundException e) {
				recordProjectionExecutionException(e, projCtx, subResult,
						SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (ObjectAlreadyExistsException e) {

				// check if this is a repeated attempt - OAEE was not handled
				// correctly, e.g. if creating "Users" user in AD, whereas
				// "Users" is SAM Account Name which is used by a built-in group
				// - in such case, mark the context as broken

				if (isRepeatedAlreadyExistsException(projCtx)) {
					recordProjectionExecutionException(e, projCtx, subResult,
							SynchronizationPolicyDecision.BROKEN);
					continue;
				}

				// in his case we do not need to set account context as
				// broken, instead we need to restart projector for this
				// context to recompute new account or find out if the
				// account was already linked..
				// and also do not set fatal error to the operation result, this
				// is a special case
				// if it is fatal, it will be set later
				// but we need to set some result
				subResult.recordSuccess();
				subResult.muteLastSubresultError();
				restartRequested = true;
				break; // we will process remaining projections when retrying
						// the wave
			} catch (CommunicationException e) {
				recordProjectionExecutionException(e, projCtx, subResult,
						SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (ConfigurationException e) {
				recordProjectionExecutionException(e, projCtx, subResult,
						SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (SecurityViolationException e) {
				recordProjectionExecutionException(e, projCtx, subResult,
						SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (ExpressionEvaluationException e) {
				recordProjectionExecutionException(e, projCtx, subResult,
						SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (RuntimeException e) {
				recordProjectionExecutionException(e, projCtx, subResult,
						SynchronizationPolicyDecision.BROKEN);
				continue;
			} finally {
				context.reportProgress(new ProgressInformation(RESOURCE_OBJECT_OPERATION,
						projCtx.getResourceShadowDiscriminator(), subResult));
			}
		}

		// Result computation here needs to be slightly different
		result.computeStatusComposite();
		return restartRequested;

	}

	private boolean shouldBeDeleted(ObjectDelta<ShadowType> accDelta, LensProjectionContext accCtx) {
		return (accDelta == null || accDelta.isEmpty())
		&& (accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.DELETE
				|| accCtx.getSynchronizationIntent() == SynchronizationIntent.DELETE);
	}

	private <O extends ObjectType> boolean isRepeatedAlreadyExistsException(
			LensProjectionContext projContext) {
		int deltas = projContext.getExecutedDeltas().size();
		LOGGER.trace("isRepeatedAlreadyExistsException starting; number of executed deltas = {}", deltas);
		if (deltas < 2) {
			return false;
		}
		LensObjectDeltaOperation<ShadowType> lastDeltaOp = projContext.getExecutedDeltas().get(deltas - 1);
		LensObjectDeltaOperation<ShadowType> previousDeltaOp = projContext.getExecutedDeltas()
				.get(deltas - 2);
		// TODO check also previous execution result to see if it's
		// AlreadyExistException?
		ObjectDelta<ShadowType> lastDelta = lastDeltaOp.getObjectDelta();
		ObjectDelta<ShadowType> previousDelta = previousDeltaOp.getObjectDelta();
		boolean rv;
		if (lastDelta.isAdd() && previousDelta.isAdd()) {
			rv = isEquivalentAddDelta(lastDelta.getObjectToAdd(), previousDelta.getObjectToAdd());
		} else if (lastDelta.isModify() && previousDelta.isModify()) {
			rv = isEquivalentModifyDelta(lastDelta.getModifications(), previousDelta.getModifications());
		} else {
			rv = false;
		}
		LOGGER.trace(
				"isRepeatedAlreadyExistsException returning {}; based of comparison of previousDelta:\n{}\nwith lastDelta:\n{}",
				rv, previousDelta, lastDelta);
		return rv;
	}

	private boolean isEquivalentModifyDelta(Collection<? extends ItemDelta<?, ?>> modifications1,
			Collection<? extends ItemDelta<?, ?>> modifications2) {
		Collection<? extends ItemDelta<?, ?>> attrDeltas1 = ItemDelta.findItemDeltasSubPath(modifications1,
				new ItemPath(ShadowType.F_ATTRIBUTES));
		Collection<? extends ItemDelta<?, ?>> attrDeltas2 = ItemDelta.findItemDeltasSubPath(modifications2,
				new ItemPath(ShadowType.F_ATTRIBUTES));
		return MiscUtil.unorderedCollectionEquals(attrDeltas1, attrDeltas2);
	}

	private boolean isEquivalentAddDelta(PrismObject<ShadowType> object1, PrismObject<ShadowType> object2) {
		PrismContainer attributes1 = object1.findContainer(ShadowType.F_ATTRIBUTES);
		PrismContainer attributes2 = object2.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributes1 == null || attributes2 == null || attributes1.size() != 1
				|| attributes2.size() != 1) { // suspicious cases
			return false;
		}
		return attributes1.getValue().equivalent(attributes2.getValue());
	}

	private <O extends ObjectType> void applyObjectPolicy(LensFocusContext<O> focusContext,
			ObjectDelta<O> focusDelta, ObjectPolicyConfigurationType objectPolicyConfigurationType) {
		if (objectPolicyConfigurationType == null) {
			return;
		}
		PrismObject<O> objectNew = focusContext.getObjectNew();
		if (focusDelta.isAdd() && objectNew.getOid() == null) {

			for (PropertyConstraintType propertyConstraintType : objectPolicyConfigurationType
					.getPropertyConstraint()) {
				if (BooleanUtils.isTrue(propertyConstraintType.isOidBound())) {
					ItemPath itemPath = propertyConstraintType.getPath().getItemPath();
					PrismProperty<Object> prop = objectNew.findProperty(itemPath);
					String stringValue = prop.getRealValue().toString();
					focusContext.setOid(stringValue);
				}
			}

			// deprecated
			if (BooleanUtils.isTrue(objectPolicyConfigurationType.isOidNameBoundMode())) {
				String name = objectNew.asObjectable().getName().getOrig();
				focusContext.setOid(name);
			}
		}
	}

	private <P extends ObjectType> void recordProjectionExecutionException(Exception e,
			LensProjectionContext accCtx, OperationResult subResult, SynchronizationPolicyDecision decision) {
		subResult.recordFatalError(e);
		LOGGER.error("Error executing changes for {}: {}",
				new Object[] { accCtx.toHumanReadableString(), e.getMessage(), e });
		if (decision != null) {
			accCtx.setSynchronizationPolicyDecision(decision);
		}
	}

	private void recordFatalError(OperationResult subResult, OperationResult result, String message,
			Throwable e) {
		if (message == null) {
			message = e.getMessage();
		}
		subResult.recordFatalError(e);
		if (result != null) {
			result.computeStatusComposite();
		}
	}

	/**
	 * Make sure that the account is linked (or unlinked) as needed.
	 */
	private <O extends ObjectType, F extends FocusType> void updateLinks(
			LensFocusContext<O> focusObjectContext, LensProjectionContext projCtx, Task task,
			OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (focusObjectContext == null) {
			return;
		}
		Class<O> objectTypeClass = focusObjectContext.getObjectTypeClass();
		if (!FocusType.class.isAssignableFrom(objectTypeClass)) {
			return;
		}
		LensFocusContext<F> focusContext = (LensFocusContext<F>) focusObjectContext;

		if (projCtx.getResourceShadowDiscriminator() != null
				&& projCtx.getResourceShadowDiscriminator().getOrder() > 0) {
			// Don't mess with links for higher-order contexts. The link should
			// be dealt with
			// during processing of zero-order context.
			return;
		}

		String projOid = projCtx.getOid();
		if (projOid == null) {
			if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
				// This seems to be OK. In quite a strange way, but still OK.
				return;
			}
			LOGGER.trace("Shadow has null OID, this should not happen, context:\n{}", projCtx.debugDump());
			throw new IllegalStateException("Shadow has null OID, this should not happen");
		}

		if (linkShouldExist(projCtx, result)) {
			// Link should exist

						PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
						if (objectCurrent != null) {
							for (ObjectReferenceType linkRef : objectCurrent.asObjectable().getLinkRef()) {
								if (projOid.equals(linkRef.getOid())) {
									// Already linked, nothing to do, only be sure, the
									// situation is set with the good value
									LOGGER.trace("Updating situation in already linked shadow.");
									updateSituationInShadow(task, SynchronizationSituationType.LINKED, focusObjectContext,
											projCtx, result);
									return;
								}
							}
						}
						// Not linked, need to link
						linkShadow(focusContext.getOid(), projOid, focusObjectContext, projCtx, task, result);
						// be sure, that the situation is set correctly
						LOGGER.trace("Updating situation after shadow was linked.");
						updateSituationInShadow(task, SynchronizationSituationType.LINKED, focusObjectContext, projCtx,
								result);
		} else {
			// Link should NOT exist

			if (!focusContext.isDelete()) {
				PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
				// it is possible that objectCurrent is null (and objectNew is
				// non-null), in case of User ADD operation (MID-2176)
				if (objectCurrent != null) {
					PrismReference linkRef = objectCurrent.findReference(FocusType.F_LINK_REF);
					if (linkRef != null) {
						for (PrismReferenceValue linkRefVal : linkRef.getValues()) {
							if (linkRefVal.getOid().equals(projOid)) {
								// Linked, need to unlink
								unlinkShadow(focusContext.getOid(), linkRefVal, focusObjectContext, projCtx,
										task, result);
							}
						}
					}
				}
			}

			if (projCtx.isDelete() || projCtx.isThombstone()) {
				LOGGER.trace("Resource object {} deleted, updating also situation in shadow.", projOid);
				// HACK HACK?
				try {
					updateSituationInShadow(task, SynchronizationSituationType.DELETED, focusObjectContext,
							projCtx, result);
				} catch (ObjectNotFoundException e) {
					// HACK HACK?
					LOGGER.trace(
							"Resource object {} is gone, cannot update situation in shadow (this is probably harmless).",
							projOid);
					result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
				}
			} else {
				// This should NOT be UNLINKED. We just do not know the
				// situation here. Reflect that in the shadow.
				LOGGER.trace("Resource object {} unlinked from the user, updating also situation in shadow.",
						projOid);
				updateSituationInShadow(task, null, focusObjectContext, projCtx, result);
			}
			// Not linked, that's OK
		}
	}

	private boolean linkShouldExist(LensProjectionContext projCtx, OperationResult result) {
		if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.UNLINK) {
			return false;
		}
		if (isEmptyThombstone(projCtx)) {
			return false;
		}
		if (projCtx.hasPendingOperations()) {
			return true;
		}
		if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.DELETE
				|| projCtx.isDelete()) {
			if (result.isInProgress()) {
				// Keep the link until operation is finished
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return true if this projection is just a linkRef that points to no
	 * shadow.
	 */
	private boolean isEmptyThombstone(LensProjectionContext projCtx) {
		return projCtx.getResourceShadowDiscriminator() != null
				&& projCtx.getResourceShadowDiscriminator().isThombstone()
				&& projCtx.getObjectCurrent() == null;
	}

	private <F extends ObjectType> void linkShadow(String userOid, String shadowOid,
			LensElementContext<F> focusContext, LensProjectionContext projCtx, Task task,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

		Class<F> typeClass = focusContext.getObjectTypeClass();
		if (!FocusType.class.isAssignableFrom(typeClass)) {
			return;
		}

		String channel = focusContext.getLensContext().getChannel();

		LOGGER.debug("Linking shadow " + shadowOid + " to focus " + userOid);
		OperationResult result = parentResult.createSubresult(OPERATION_LINK_ACCOUNT);
		PrismReferenceValue linkRef = new PrismReferenceValue();
		linkRef.setOid(shadowOid);
		linkRef.setTargetType(ShadowType.COMPLEX_TYPE);
		Collection<? extends ItemDelta> linkRefDeltas = ReferenceDelta
				.createModificationAddCollection(FocusType.F_LINK_REF, getUserDefinition(), linkRef);

		try {
			cacheRepositoryService.modifyObject(typeClass, userOid, linkRefDeltas, result);
			task.recordObjectActionExecuted(focusContext.getObjectAny(), typeClass, userOid,
					ChangeType.MODIFY, channel, null);
		} catch (ObjectAlreadyExistsException ex) {
			task.recordObjectActionExecuted(focusContext.getObjectAny(), typeClass, userOid,
					ChangeType.MODIFY, channel, ex);
			throw new SystemException(ex);
		} catch (Throwable t) {
			task.recordObjectActionExecuted(focusContext.getObjectAny(), typeClass, userOid,
					ChangeType.MODIFY, channel, t);
			throw t;
		} finally {
			result.computeStatus();
			ObjectDelta<F> userDelta = ObjectDelta.createModifyDelta(userOid, linkRefDeltas, typeClass,
					prismContext);
			LensObjectDeltaOperation<F> userDeltaOp = LensUtil.createObjectDeltaOperation(userDelta, result,
					focusContext, projCtx);
			focusContext.addToExecutedDeltas(userDeltaOp);
		}

	}

	private PrismObjectDefinition<UserType> getUserDefinition() {
		return userDefinition;
	}

	private <F extends ObjectType> void unlinkShadow(String focusOid, PrismReferenceValue accountRef,
			LensElementContext<F> focusContext, LensProjectionContext projCtx, Task task,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

		Class<F> typeClass = focusContext.getObjectTypeClass();
		if (!FocusType.class.isAssignableFrom(typeClass)) {
			return;
		}

		String channel = focusContext.getLensContext().getChannel();

		LOGGER.debug("Unlinking shadow " + accountRef.getOid() + " from focus " + focusOid);
		OperationResult result = parentResult.createSubresult(OPERATION_UNLINK_ACCOUNT);
		Collection<? extends ItemDelta> accountRefDeltas = ReferenceDelta.createModificationDeleteCollection(
				FocusType.F_LINK_REF, getUserDefinition(), accountRef.clone());

		try {
			cacheRepositoryService.modifyObject(typeClass, focusOid, accountRefDeltas, result);
			task.recordObjectActionExecuted(focusContext.getObjectAny(), typeClass, focusOid,
					ChangeType.MODIFY, channel, null);
		} catch (ObjectAlreadyExistsException ex) {
			task.recordObjectActionExecuted(focusContext.getObjectAny(), typeClass, focusOid,
					ChangeType.MODIFY, channel, ex);
			result.recordFatalError(ex);
			throw new SystemException(ex);
		} catch (Throwable t) {
			task.recordObjectActionExecuted(focusContext.getObjectAny(), typeClass, focusOid,
					ChangeType.MODIFY, channel, t);
			throw t;
		} finally {
			result.computeStatus();
			ObjectDelta<F> userDelta = ObjectDelta.createModifyDelta(focusOid, accountRefDeltas, typeClass,
					prismContext);
			LensObjectDeltaOperation<F> userDeltaOp = LensUtil.createObjectDeltaOperation(userDelta, result,
					focusContext, projCtx);
			focusContext.addToExecutedDeltas(userDeltaOp);
		}

	}

	private <F extends ObjectType> void updateSituationInShadow(Task task,
			SynchronizationSituationType situation, LensFocusContext<F> focusContext,
			LensProjectionContext projectionCtx, OperationResult parentResult)
					throws ObjectNotFoundException, SchemaException {

		String projectionOid = projectionCtx.getOid();

		OperationResult result = new OperationResult(OPERATION_UPDATE_SITUATION_ACCOUNT);
		result.addParam("situation", situation);
		result.addParam("accountRef", projectionOid);

		PrismObject<ShadowType> account = null;
		GetOperationOptions getOptions = GetOperationOptions.createNoFetch();
		getOptions.setAllowNotFound(true);
		try {
			account = provisioning.getObject(ShadowType.class, projectionOid,
					SelectorOptions.createCollection(getOptions), task, result);
		} catch (Exception ex) {
			LOGGER.trace("Problem with getting account, skipping modifying situation in account.");
			return;
		}
		List<PropertyDelta<?>> syncSituationDeltas = SynchronizationUtils
				.createSynchronizationSituationAndDescriptionDelta(account, situation, task.getChannel(),
						projectionCtx.hasFullShadow());

		try {
			Utils.setRequestee(task, focusContext);
			ProvisioningOperationOptions options = ProvisioningOperationOptions
					.createCompletePostponed(false);
			options.setDoNotDiscovery(true);
			String changedOid = provisioning.modifyObject(ShadowType.class, projectionOid,
					syncSituationDeltas, null, options, task, result);
			// modifyProvisioningObject(AccountShadowType.class, accountRef,
			// syncSituationDeltas,
			// ProvisioningOperationOptions.createCompletePostponed(false),
			// task, result);
			projectionCtx.setSynchronizationSituationResolved(situation);
			LOGGER.trace("Situation in projection {} was updated to {}.", projectionCtx, situation);
		} catch (ObjectNotFoundException ex) {
			// if the object not found exception is thrown, it's ok..probably
			// the account was deleted by previous execution of changes..just
			// log in the trace the message for the user..
			LOGGER.trace(
					"Situation in account could not be updated. Account not found on the resource. Skipping modifying situation in account");
			return;
		} catch (Exception ex) {
			throw new SystemException(ex.getMessage(), ex);
		} finally {
			Utils.clearRequestee(task);
		}
		// if everything is OK, add result of the situation modification to the
		// parent result
		result.recordSuccess();
		parentResult.addSubresult(result);

	}

	private <T extends ObjectType, F extends ObjectType> void executeDelta(ObjectDelta<T> objectDelta,
			LensElementContext<T> objectContext, LensContext<F> context, ModelExecuteOptions options,
			ResourceType resource, Task task, OperationResult parentResult)
					throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException,
					CommunicationException, ConfigurationException, SecurityViolationException,
					ExpressionEvaluationException {

		if (objectDelta == null) {
			throw new IllegalArgumentException("Null change");
		}

		if (objectDelta.getOid() == null) {
			objectDelta.setOid(objectContext.getOid());
		}

		objectDelta = computeDeltaToExecute(objectDelta, objectContext);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("computeDeltaToExecute returned:\n{}",
					objectDelta != null ? objectDelta.debugDump() : "(null)");
		}

		if (objectDelta == null || objectDelta.isEmpty()) {
			LOGGER.debug("Skipping execution of delta because it was already executed: {}", objectContext);
			return;
		}

		if (InternalsConfig.consistencyChecks) {
			objectDelta.checkConsistence(ConsistencyCheckScope.fromBoolean(consistencyChecks));
		}

		// Other types than focus types may not be definition-complete (e.g.
		// accounts and resources are completed in provisioning)
		if (FocusType.class.isAssignableFrom(objectDelta.getObjectTypeClass())) {
			objectDelta.assertDefinitions();
		}

		LensUtil.setDeltaOldValue(objectContext, objectDelta);

		if (LOGGER.isTraceEnabled()) {
			logDeltaExecution(objectDelta, context, resource, null, task);
		}

		OperationResult result = parentResult.createSubresult(OPERATION_EXECUTE_DELTA);

		try {

			if (objectDelta.getChangeType() == ChangeType.ADD) {
				executeAddition(objectDelta, context, objectContext, options, resource, task, result);
			} else if (objectDelta.getChangeType() == ChangeType.MODIFY) {
				executeModification(objectDelta, context, objectContext, options, resource, task, result);
			} else if (objectDelta.getChangeType() == ChangeType.DELETE) {
				executeDeletion(objectDelta, context, objectContext, options, resource, task, result);
			}

			// To make sure that the OID is set (e.g. after ADD operation)
			LensUtil.setContextOid(context, objectContext, objectDelta.getOid());

		} finally {

			result.computeStatus();
			if (objectContext != null) {
				if (!objectDelta.hasCompleteDefinition()) {
					throw new SchemaException("object delta does not have complete definition");
				}
				LensObjectDeltaOperation<T> objectDeltaOp = LensUtil.createObjectDeltaOperation(
						objectDelta.clone(), result, objectContext, null, resource);
				objectContext.addToExecutedDeltas(objectDeltaOp);
			}

			if (LOGGER.isDebugEnabled()) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("EXECUTION result {}", result.getLastSubresult());
				} else {
					// Execution of deltas was not logged yet
					logDeltaExecution(objectDelta, context, resource, result.getLastSubresult(), task);
				}
			}
		}
	}

	private <T extends ObjectType, F extends FocusType> void removeExecutedItemDeltas(
			ObjectDelta<T> objectDelta, LensElementContext<T> objectContext) {
		if (objectContext == null) {
			return;
		}

		if (objectDelta == null || objectDelta.isEmpty()) {
			return;
		}

		if (objectDelta.getModifications() == null || objectDelta.getModifications().isEmpty()) {
			return;
		}

		List<LensObjectDeltaOperation<T>> executedDeltas = objectContext.getExecutedDeltas();
		for (LensObjectDeltaOperation<T> executedDelta : executedDeltas) {
			ObjectDelta<T> executed = executedDelta.getObjectDelta();
			Iterator<? extends ItemDelta> objectDeltaIterator = objectDelta.getModifications().iterator();
			while (objectDeltaIterator.hasNext()) {
				ItemDelta d = objectDeltaIterator.next();
				if (executed.containsModification(d, true, true) || d.isEmpty()) {
					objectDeltaIterator.remove();
				}
			}
		}
	}

	// TODO beware - what if the delta was executed but not successfully?
	private <T extends ObjectType, F extends FocusType> boolean alreadyExecuted(ObjectDelta<T> objectDelta,
			LensElementContext<T> objectContext) {
		if (objectContext == null) {
			return false;
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Checking for already executed delta:\n{}\nIn deltas:\n{}", objectDelta.debugDump(),
					DebugUtil.debugDump(objectContext.getExecutedDeltas()));
		}
		return ObjectDeltaOperation.containsDelta(objectContext.getExecutedDeltas(), objectDelta);
	}

	/**
	 * Was this object already added? (temporary method, should be removed soon)
	 */
	private <T extends ObjectType> boolean wasAdded(List<LensObjectDeltaOperation<T>> executedOperations,
			String oid) {
		for (LensObjectDeltaOperation operation : executedOperations) {
			if (operation.getObjectDelta().isAdd() && oid.equals(operation.getObjectDelta().getOid())
					&& !operation.getExecutionResult().isFatalError()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Computes delta to execute, given a list of already executes deltas. See
	 * below.
	 */
	private <T extends ObjectType> ObjectDelta<T> computeDeltaToExecute(ObjectDelta<T> objectDelta,
			LensElementContext<T> objectContext) {
		if (objectContext == null) {
			return objectDelta;
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Computing delta to execute from delta:\n{}\nGiven these executed deltas:\n{}",
					objectDelta.debugDump(), DebugUtil.debugDump(objectContext.getExecutedDeltas()));
		}
		List<LensObjectDeltaOperation<T>> executedDeltas = objectContext.getExecutedDeltas();
		return computeDiffDelta(executedDeltas, objectDelta);
	}

	/**
	 * Compute a "difference delta" - given that executedDeltas were executed,
	 * and objectDelta is about to be executed; eliminates parts that have
	 * already been done. It is meant as a kind of optimization (for MODIFY
	 * deltas) and error avoidance (for ADD deltas).
	 *
	 * Explanation for ADD deltas: there are situations where an execution wave
	 * is restarted - when unexpected AlreadyExistsException is reported from
	 * provisioning. However, in such cases, duplicate ADD Focus deltas were
	 * generated. So we (TEMPORARILY!) decided to filter them out here.
	 *
	 * Unfortunately, this mechanism is not well-defined, and seems to work more
	 * "by accident" than "by design". It should be replaced with something more
	 * serious. Perhaps by re-reading current focus state when repeating a wave?
	 * Actually, it is a supplement for rewriting ADD->MODIFY deltas in
	 * LensElementContext.getFixedPrimaryDelta. That method converts primary
	 * deltas (and as far as I know, that is the only place where this problem
	 * should occur). Nevertheless, for historical and safety reasons I keep
	 * also the processing in this method.
	 *
	 * Anyway, currently it treats only two cases: 1) if the objectDelta is
	 * present in the list of executed deltas 2) if the objectDelta is ADD, and
	 * another ADD delta is there (then the difference is computed)
	 *
	 */
	private <T extends ObjectType> ObjectDelta<T> computeDiffDelta(
			List<? extends ObjectDeltaOperation<T>> executedDeltas, ObjectDelta<T> objectDelta) {
		if (executedDeltas == null || executedDeltas.isEmpty()) {
			return objectDelta;
		}

		// any delta related to our OID, not ending with fatal error
		ObjectDeltaOperation<T> lastRelated = findLastRelatedDelta(executedDeltas, objectDelta);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("findLastRelatedDelta returned:\n{}",
					lastRelated != null ? lastRelated.debugDump() : "(null)");
		}
		if (lastRelated == null) {
			return objectDelta; // nothing found, let us apply our delta
		}
		if (lastRelated.getExecutionResult().isSuccess() && lastRelated.containsDelta(objectDelta)) {
			return null; // case 1 - exact match found with SUCCESS result,
							// let's skip the processing of our delta
		}
		if (!objectDelta.isAdd()) {
			return objectDelta; // MODIFY or DELETE delta - we may safely apply
								// it (not 100% sure about DELETE case)
		}
		// determine if we got case 2
		if (lastRelated.getObjectDelta().isDelete()) {
			return objectDelta; // we can (and should) apply the ADD delta as a
								// whole, because the object was deleted
		}
		// let us treat the most simple case here - meaning we have existing ADD
		// delta and nothing more
		// TODO add more sophistication if needed
		if (!lastRelated.getObjectDelta().isAdd()) {
			return objectDelta; // this will probably fail, but ...
		}
		// at this point we know that ADD was more-or-less successfully
		// executed, so let's compute the difference, creating a MODIFY delta
		PrismObject<T> alreadyAdded = lastRelated.getObjectDelta().getObjectToAdd();
		PrismObject<T> toBeAddedNow = objectDelta.getObjectToAdd();
		return alreadyAdded.diff(toBeAddedNow);
	}

	private <T extends ObjectType> ObjectDeltaOperation<T> findLastRelatedDelta(
			List<? extends ObjectDeltaOperation<T>> executedDeltas, ObjectDelta<T> objectDelta) {
		for (int i = executedDeltas.size() - 1; i >= 0; i--) {
			ObjectDeltaOperation<T> currentOdo = executedDeltas.get(i);
			if (!currentOdo.getExecutionResult().isFatalError()) {
				ObjectDelta<T> current = currentOdo.getObjectDelta();

				if (current.equals(objectDelta)) {
					return currentOdo;
				}

				String oid1 = current.isAdd() ? current.getObjectToAdd().getOid() : current.getOid();
				String oid2 = objectDelta.isAdd() ? objectDelta.getObjectToAdd().getOid()
						: objectDelta.getOid();
				if (oid1 != null && oid2 != null) {
					if (oid1.equals(oid2)) {
						return currentOdo;
					}
					continue;
				}
				// ADD-MODIFY and ADD-DELETE combinations lead to applying whole
				// delta (as a result of computeDiffDelta)
				// so we can be lazy and check only ADD-ADD combinations here...
				if (!current.isAdd() || !objectDelta.isAdd()) {
					continue;
				}
				// we simply check the type (for focus objects) and
				// resource+kind+intent (for shadows)
				PrismObject<T> currentObject = current.getObjectToAdd();
				PrismObject<T> objectTypeToAdd = objectDelta.getObjectToAdd();
				Class currentObjectClass = currentObject.getCompileTimeClass();
				Class objectTypeToAddClass = objectTypeToAdd.getCompileTimeClass();
				if (currentObjectClass == null || !currentObjectClass.equals(objectTypeToAddClass)) {
					continue;
				}
				if (FocusType.class.isAssignableFrom(currentObjectClass)) {
					return currentOdo; // we suppose there is only one delta of
										// Focus class
				}
				// Shadow deltas have to be matched exactly... because "ADD
				// largo" and "ADD largo1" are two different deltas
				// And, this is not a big problem, because ADD conflicts are
				// treated by provisioning
				// Again, all this stuff is highly temporary and has to be
				// thrown away as soon as possible!!!
				continue;
				// if (ShadowType.class.equals(currentObjectClass)) {
				// ShadowType currentShadow = (ShadowType)
				// currentObject.asObjectable();
				// ShadowType shadowToAdd = (ShadowType)
				// objectTypeToAdd.asObjectable();
				// if (currentShadow.getResourceRef() != null &&
				// shadowToAdd.getResourceRef() != null &&
				// currentShadow.getResourceRef().getOid().equals(shadowToAdd.getResourceRef().getOid())
				// &&
				// currentShadow.getKind() == shadowToAdd.getKind() && // TODO
				// default kind handling
				// currentShadow.getIntent() != null &&
				// currentShadow.getIntent().equals(shadowToAdd.getIntent())) {
				// // TODO default intent handling
				// return currentOdo;
				// }
				// }
			}
		}
		return null;
	}

	private ProvisioningOperationOptions copyFromModelOptions(ModelExecuteOptions options) {
		ProvisioningOperationOptions provisioningOptions = new ProvisioningOperationOptions();
		if (options == null) {
			return provisioningOptions;
		}

		provisioningOptions.setForce(options.getForce());
		provisioningOptions.setOverwrite(options.getOverwrite());
		return provisioningOptions;
	}

	private <F extends ObjectType> ProvisioningOperationOptions getProvisioningOptions(LensContext<F> context,
			ModelExecuteOptions modelOptions) {
		if (modelOptions == null && context != null) {
			modelOptions = context.getOptions();
		}
		ProvisioningOperationOptions provisioningOptions = copyFromModelOptions(modelOptions);

		if (context != null && context.getChannel() != null) {

			if (context.getChannel().equals(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECON))) {
				// TODO: this is probably wrong. We should not have special case
				// for recon channel! This should be handled by the provisioning task
				// setting the right options there.
				provisioningOptions.setCompletePostponed(false);
			}

			if (context.getChannel().equals(SchemaConstants.CHANGE_CHANNEL_DISCOVERY_URI)) {
				// We want to avoid endless loops in error handling.
				provisioningOptions.setDoNotDiscovery(true);
			}
		}

		return provisioningOptions;
	}

	private <T extends ObjectType, F extends ObjectType> void logDeltaExecution(ObjectDelta<T> objectDelta,
			LensContext<F> context, ResourceType resource, OperationResult result, Task task) {
		StringBuilder sb = new StringBuilder();
		sb.append("---[ ");
		if (result == null) {
			sb.append("Going to EXECUTE");
		} else {
			sb.append("EXECUTED");
		}
		sb.append(" delta of ").append(objectDelta.getObjectTypeClass().getSimpleName());
		sb.append(" ]---------------------\n");
		DebugUtil.debugDumpLabel(sb, "Channel", 0);
		sb.append(" ").append(LensUtil.getChannel(context, task)).append("\n");
		if (context != null) {
			DebugUtil.debugDumpLabel(sb, "Wave", 0);
			sb.append(" ").append(context.getExecutionWave()).append("\n");
		}
		if (resource != null) {
			sb.append("Resource: ").append(resource.toString()).append("\n");
		}
		sb.append(objectDelta.debugDump());
		sb.append("\n");
		if (result != null) {
			DebugUtil.debugDumpLabel(sb, "Result", 0);
			sb.append(" ").append(result.getStatus()).append(": ").append(result.getMessage());
		}
		sb.append("\n--------------------------------------------------");

		LOGGER.debug("\n{}", sb);
	}

	private <F extends ObjectType> OwnerResolver createOwnerResolver(final LensContext<F> context, Task task,
			OperationResult result) {
		return new LensOwnerResolver<>(context, objectResolver, task, result);
	}

	private <T extends ObjectType, F extends ObjectType> void executeAddition(ObjectDelta<T> change,
			final LensContext<F> context, LensElementContext<T> objectContext, ModelExecuteOptions options,
			ResourceType resource, Task task, OperationResult result) throws ObjectAlreadyExistsException,
					ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
					SecurityViolationException, ExpressionEvaluationException {

		PrismObject<T> objectToAdd = change.getObjectToAdd();

		if (change.getModifications() != null) {
			for (ItemDelta delta : change.getModifications()) {
				delta.applyTo(objectToAdd);
			}
			change.getModifications().clear();
		}

		OwnerResolver ownerResolver = createOwnerResolver(context, task, result);
		try {
			securityEnforcer.authorize(ModelAuthorizationAction.ADD.getUrl(),
					AuthorizationPhaseType.EXECUTION, objectToAdd, null, null, ownerResolver, result);

			T objectTypeToAdd = objectToAdd.asObjectable();

			metadataManager.applyMetadataAdd(context, objectToAdd, clock.currentTimeXMLGregorianCalendar(), task, result);

			if (options == null && context != null) {
				options = context.getOptions();
			}

			String oid;
			if (objectTypeToAdd instanceof TaskType) {
				oid = addTask((TaskType) objectTypeToAdd, result);
			} else if (objectTypeToAdd instanceof NodeType) {
				throw new UnsupportedOperationException("NodeType cannot be added using model interface");
			} else if (ObjectTypes.isManagedByProvisioning(objectTypeToAdd)) {

				ProvisioningOperationOptions provisioningOptions = getProvisioningOptions(context, options);

				oid = addProvisioningObject(objectToAdd, context, objectContext, provisioningOptions,
						resource, task, result);
				if (oid == null) {
					throw new SystemException(
							"Provisioning addObject returned null OID while adding " + objectToAdd);
				}
				result.addReturn("createdAccountOid", oid);
			} else {
				FocusConstraintsChecker.clearCacheFor(objectToAdd.asObjectable().getName());

				RepoAddOptions addOpt = new RepoAddOptions();
				if (ModelExecuteOptions.isOverwrite(options)) {
					addOpt.setOverwrite(true);
				}
				if (ModelExecuteOptions.isNoCrypt(options)) {
					addOpt.setAllowUnencryptedValues(true);
				}
				oid = cacheRepositoryService.addObject(objectToAdd, addOpt, result);
				if (oid == null) {
					throw new SystemException(
							"Repository addObject returned null OID while adding " + objectToAdd);
				}
			}
			change.setOid(oid);
			task.recordObjectActionExecuted(objectToAdd, objectToAdd.getCompileTimeClass(), oid,
					ChangeType.ADD, context.getChannel(), null);
		} catch (Throwable t) {
			task.recordObjectActionExecuted(objectToAdd, objectToAdd.getCompileTimeClass(), null,
					ChangeType.ADD, context.getChannel(), t);
			throw t;
		}
	}

	private <T extends ObjectType, F extends ObjectType> void executeDeletion(ObjectDelta<T> change,
			LensContext<F> context, LensElementContext<T> objectContext, ModelExecuteOptions options,
			ResourceType resource, Task task, OperationResult result) throws ObjectNotFoundException,
					ObjectAlreadyExistsException, SchemaException, CommunicationException,
					ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		String oid = change.getOid();
		Class<T> objectTypeClass = change.getObjectTypeClass();

		PrismObject<T> objectOld = objectContext.getObjectOld();
		OwnerResolver ownerResolver = createOwnerResolver(context, task, result);
		try {
			securityEnforcer.authorize(ModelAuthorizationAction.DELETE.getUrl(),
					AuthorizationPhaseType.EXECUTION, objectOld, null, null, ownerResolver, result);

			if (TaskType.class.isAssignableFrom(objectTypeClass)) {
				taskManager.deleteTask(oid, result);
			} else if (NodeType.class.isAssignableFrom(objectTypeClass)) {
				taskManager.deleteNode(oid, result);
			} else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
				ProvisioningOperationOptions provisioningOptions = getProvisioningOptions(context, options);
				try {
					deleteProvisioningObject(objectTypeClass, oid, context, objectContext,
							provisioningOptions, resource, task, result);
				} catch (ObjectNotFoundException e) {
					// Object that we wanted to delete is already gone. This can
					// happen in some race conditions.
					// As the resulting state is the same as we wanted it to be
					// we will not complain and we will go on.
					LOGGER.trace("Attempt to delete object {} ({}) that is already gone", oid,
							objectTypeClass);
					result.muteLastSubresultError();
				}
			} else {
				try {
					cacheRepositoryService.deleteObject(objectTypeClass, oid, result);
				} catch (ObjectNotFoundException e) {
					// Object that we wanted to delete is already gone. This can
					// happen in some race conditions.
					// As the resulting state is the same as we wanted it to be
					// we will not complain and we will go on.
					LOGGER.trace("Attempt to delete object {} ({}) that is already gone", oid,
							objectTypeClass);
					result.muteLastSubresultError();
				}
			}
			task.recordObjectActionExecuted(objectOld, objectTypeClass, oid, ChangeType.DELETE,
					context.getChannel(), null);
		} catch (Throwable t) {
			task.recordObjectActionExecuted(objectOld, objectTypeClass, oid, ChangeType.DELETE,
					context.getChannel(), t);
			throw t;
		}
	}

	private <T extends ObjectType, F extends ObjectType> void executeModification(ObjectDelta<T> change,
			LensContext<F> context, LensElementContext<T> objectContext, ModelExecuteOptions options,
			ResourceType resource, Task task, OperationResult result) throws ObjectNotFoundException,
					SchemaException, ObjectAlreadyExistsException, CommunicationException,
					ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Class<T> objectTypeClass = change.getObjectTypeClass();

		PrismObject<T> objectNew = objectContext.getObjectNew();
		OwnerResolver ownerResolver = createOwnerResolver(context, task, result);
		try {
			securityEnforcer.authorize(ModelAuthorizationAction.MODIFY.getUrl(),
					AuthorizationPhaseType.EXECUTION, objectNew, change, null, ownerResolver, result);

			metadataManager.applyMetadataModify(change, objectContext, objectTypeClass,
					clock.currentTimeXMLGregorianCalendar(), task, context, result);

			if (change.isEmpty()) {
				// Nothing to do
				return;
			}

			if (TaskType.class.isAssignableFrom(objectTypeClass)) {
				taskManager.modifyTask(change.getOid(), change.getModifications(), result);
			} else if (NodeType.class.isAssignableFrom(objectTypeClass)) {
				throw new UnsupportedOperationException("NodeType is not modifiable using model interface");
			} else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
				ProvisioningOperationOptions provisioningOptions = getProvisioningOptions(context, options);
				String oid = modifyProvisioningObject(objectTypeClass, change.getOid(),
						change.getModifications(), context, objectContext, provisioningOptions, resource,
						task, result);
				if (!oid.equals(change.getOid())) {
					change.setOid(oid);
				}
			} else {
				FocusConstraintsChecker.clearCacheForDelta(change.getModifications());
				cacheRepositoryService.modifyObject(objectTypeClass, change.getOid(),
						change.getModifications(), result);
			}
			task.recordObjectActionExecuted(objectNew, objectTypeClass, change.getOid(), ChangeType.MODIFY,
					context.getChannel(), null);
		} catch (Throwable t) {
			task.recordObjectActionExecuted(objectNew, objectTypeClass, change.getOid(), ChangeType.MODIFY,
					context.getChannel(), t);
			throw t;
		}
	}



	private String addTask(TaskType task, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException {
		try {
			return taskManager.addTask(task.asPrismObject(), result);
		} catch (ObjectAlreadyExistsException ex) {
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't add object {} to task manager", ex, task.getName());
			throw new SystemException(ex.getMessage(), ex);
		}
	}

	private <F extends ObjectType, T extends ObjectType> String addProvisioningObject(PrismObject<T> object,
			LensContext<F> context, LensElementContext<T> objectContext, ProvisioningOperationOptions options,
			ResourceType resource, Task task, OperationResult result) throws ObjectNotFoundException,
					ObjectAlreadyExistsException, SchemaException, CommunicationException,
					ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		if (object.canRepresent(ShadowType.class)) {
			ShadowType shadow = (ShadowType) object.asObjectable();
			String resourceOid = ShadowUtil.getResourceOid(shadow);
			if (resourceOid == null) {
				throw new IllegalArgumentException("Resource OID is null in shadow");
			}
		}

		OperationProvisioningScriptsType scripts = null;
		if (object.canRepresent(ShadowType.class)) {
			scripts = prepareScripts(object, context, objectContext, ProvisioningOperationTypeType.ADD,
					resource, task, result);
		}
		Utils.setRequestee(task, context);
		String oid = provisioning.addObject(object, scripts, options, task, result);
		Utils.clearRequestee(task);
		return oid;
	}

	private <F extends ObjectType, T extends ObjectType> void deleteProvisioningObject(
			Class<T> objectTypeClass, String oid, LensContext<F> context, LensElementContext<T> objectContext,
			ProvisioningOperationOptions options, ResourceType resource, Task task, OperationResult result)
					throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException,
					CommunicationException, ConfigurationException, SecurityViolationException,
					ExpressionEvaluationException {

		PrismObject<T> shadowToModify = null;
		OperationProvisioningScriptsType scripts = null;
		try {
			GetOperationOptions rootOpts = GetOperationOptions.createNoFetch();
			rootOpts.setPointInTimeType(PointInTimeType.FUTURE);
			shadowToModify = provisioning.getObject(objectTypeClass, oid,
					SelectorOptions.createCollection(rootOpts), task, result);
		} catch (ObjectNotFoundException ex) {
			// this is almost OK, mute the error and try to delete account (it
			// will fail if something is wrong)
			result.muteLastSubresultError();
		}
		if (ShadowType.class.isAssignableFrom(objectTypeClass)) {
			scripts = prepareScripts(shadowToModify, context, objectContext,
					ProvisioningOperationTypeType.DELETE, resource, task, result);
		}
		Utils.setRequestee(task, context);
		provisioning.deleteObject(objectTypeClass, oid, options, scripts, task, result);
		Utils.clearRequestee(task);
	}

	private <F extends ObjectType, T extends ObjectType> String modifyProvisioningObject(
			Class<T> objectTypeClass, String oid, Collection<? extends ItemDelta> modifications,
			LensContext<F> context, LensElementContext<T> objectContext, ProvisioningOperationOptions options,
			ResourceType resource, Task task, OperationResult result) throws ObjectNotFoundException,
					CommunicationException, SchemaException, ConfigurationException,
					SecurityViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException {

		PrismObject<T> shadowToModify = null;
		OperationProvisioningScriptsType scripts = null;
		try {
			GetOperationOptions rootOpts = GetOperationOptions.createNoFetch();
			rootOpts.setPointInTimeType(PointInTimeType.FUTURE);
			shadowToModify = provisioning.getObject(objectTypeClass, oid,
					SelectorOptions.createCollection(rootOpts), task, result);
		} catch (ObjectNotFoundException e) {
			// We do not want the operation to fail here. The object might have
			// been re-created on the resource
			// or discovery might re-create it. So simply ignore this error and
			// give provisioning a chance to fail
			// properly.
			result.muteLastSubresultError();
			LOGGER.warn("Repository object {}: {} is gone. But trying to modify resource object anyway",
					objectTypeClass, oid);
		}
		if (ShadowType.class.isAssignableFrom(objectTypeClass)) {
			scripts = prepareScripts(shadowToModify, context, objectContext,
					ProvisioningOperationTypeType.MODIFY, resource, task, result);
		}
		Utils.setRequestee(task, context);
		String changedOid = provisioning.modifyObject(objectTypeClass, oid, modifications, scripts, options,
				task, result);
		Utils.clearRequestee(task);
		return changedOid;
	}

	private <F extends ObjectType, T extends ObjectType> OperationProvisioningScriptsType prepareScripts(
			PrismObject<T> changedObject, LensContext<F> context, LensElementContext<T> objectContext,
			ProvisioningOperationTypeType operation, ResourceType resource, Task task, OperationResult result)
					throws ObjectNotFoundException, SchemaException, CommunicationException,
					ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		if (resource == null) {
			LOGGER.warn("Resource does not exist. Skipping processing scripts.");
			return null;
		}
		OperationProvisioningScriptsType resourceScripts = resource.getScripts();
		PrismObject<ShadowType> resourceObject = (PrismObject<ShadowType>) changedObject;

		PrismObject<F> user = null;
		if (context.getFocusContext() != null) {
			if (context.getFocusContext().getObjectNew() != null) {
				user = context.getFocusContext().getObjectNew();
			} else if (context.getFocusContext().getObjectCurrent() != null) {
				user = context.getFocusContext().getObjectCurrent();
			} else if (context.getFocusContext().getObjectOld() != null) {
				user = context.getFocusContext().getObjectOld();
			}
		}

		LensProjectionContext projectionCtx = (LensProjectionContext) objectContext;
		PrismObject<ShadowType> shadow = null;
		if (projectionCtx.getObjectNew() != null) {
			shadow = projectionCtx.getObjectNew();
		} else if (projectionCtx.getObjectCurrent() != null) {
			shadow = projectionCtx.getObjectCurrent();
		} else {
			shadow = projectionCtx.getObjectOld();
		}

		if (shadow == null) {
			//put at least something
			shadow = resourceObject.clone();
		}

		ResourceShadowDiscriminator discr = ((LensProjectionContext) objectContext)
				.getResourceShadowDiscriminator();

		ExpressionVariables variables = Utils.getDefaultExpressionVariables(user, shadow, discr,
				resource.asPrismObject(), context.getSystemConfiguration(), objectContext);
		ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(context, (LensProjectionContext) objectContext, task, result));
		try {
			return evaluateScript(resourceScripts, discr, operation, null, variables, context, objectContext, task, result);
		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}

	}

	private OperationProvisioningScriptsType evaluateScript(OperationProvisioningScriptsType resourceScripts,
			ResourceShadowDiscriminator discr, ProvisioningOperationTypeType operation, BeforeAfterType order,
			ExpressionVariables variables, LensContext<?> context,
			LensElementContext<?> objectContext, Task task,
			OperationResult result)
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		OperationProvisioningScriptsType outScripts = new OperationProvisioningScriptsType();

		if (resourceScripts != null) {
			OperationProvisioningScriptsType scripts = resourceScripts.clone();
			for (OperationProvisioningScriptType script : scripts.getScript()) {
				if (discr != null) {
					if (script.getKind() != null && !script.getKind().isEmpty()
							&& !script.getKind().contains(discr.getKind())) {
						continue;
					}
					if (script.getIntent() != null && !script.getIntent().isEmpty()
							&& !script.getIntent().contains(discr.getIntent()) && discr.getIntent() != null) {
						continue;
					}
				}
				if (!script.getOperation().contains(operation)) {
					continue;
				}
				if (order != null && order != script.getOrder()) {
					continue;
				}
				// Let's do the most expensive evaluation last
				if (!evaluateScriptCondition(script, variables, task, result)){
					continue;
				}
				for (ProvisioningScriptArgumentType argument : script.getArgument()) {
					evaluateScriptArgument(argument, variables, context, objectContext, task, result);
				}
				outScripts.getScript().add(script);
			}
		}

		return outScripts;
	}

	private boolean evaluateScriptCondition(OperationProvisioningScriptType script,
			ExpressionVariables variables, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		ExpressionType condition = script.getCondition();
		if (condition == null) {
			return true;
		}

		PrismPropertyValue<Boolean> conditionOutput = ExpressionUtil.evaluateCondition(variables, condition, expressionFactory, " condition for provisioning script ", task, result);
		if (conditionOutput == null) {
			return true;
		}

		Boolean conditionOutputValue = conditionOutput.getValue();

		return BooleanUtils.isNotFalse(conditionOutputValue);

	}

	private void evaluateScriptArgument(ProvisioningScriptArgumentType argument,
			ExpressionVariables variables, LensContext<?> context,
			LensElementContext<?> objectContext, Task task,
			OperationResult result)
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

		QName FAKE_SCRIPT_ARGUMENT_NAME = new QName(SchemaConstants.NS_C, "arg");

		PrismPropertyDefinition<String> scriptArgumentDefinition = new PrismPropertyDefinitionImpl<>(
				FAKE_SCRIPT_ARGUMENT_NAME, DOMUtil.XSD_STRING, prismContext);

		String shortDesc = "Provisioning script argument expression";
		Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression = expressionFactory
				.makeExpression(argument, scriptArgumentDefinition, shortDesc, task, result);

		ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, variables, shortDesc, task,
				result);
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = ModelExpressionThreadLocalHolder
				.evaluateExpressionInContext(expression, params, context,
						objectContext instanceof LensProjectionContext ? (LensProjectionContext) objectContext : null, task, result);

		Collection<PrismPropertyValue<String>> nonNegativeValues = null;
		if (outputTriple != null) {
			nonNegativeValues = outputTriple.getNonNegativeValues();
		}

		// replace dynamic script with static value..
		argument.getExpressionEvaluator().clear();
		if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
			// We need to create at least one evaluator. Otherwise the
			// expression code will complain
			// Element value = DOMUtil.createElement(SchemaConstants.C_VALUE);
			// DOMUtil.setNill(value);
			JAXBElement<RawType> el = new JAXBElement(SchemaConstants.C_VALUE, RawType.class,
					new RawType(prismContext));
			argument.getExpressionEvaluator().add(el);

		} else {
			for (PrismPropertyValue<String> val : nonNegativeValues) {
				// Element value =
				// DOMUtil.createElement(SchemaConstants.C_VALUE);
				// value.setTextContent(val.getValue());
				PrimitiveXNode<String> prim = new PrimitiveXNode<>();
				prim.setValue(val.getValue(), DOMUtil.XSD_STRING);
				JAXBElement<RawType> el = new JAXBElement(SchemaConstants.C_VALUE, RawType.class,
						new RawType(prim, prismContext));
				argument.getExpressionEvaluator().add(el);
			}
		}
	}

	private <T extends ObjectType, F extends ObjectType> void executeReconciliationScript(
			LensProjectionContext projContext, LensContext<F> context, BeforeAfterType order, Task task,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
					ExpressionEvaluationException, CommunicationException, ConfigurationException,
					SecurityViolationException, ObjectAlreadyExistsException {

		if (!projContext.isDoReconciliation()) {
			return;
		}

		ResourceType resource = projContext.getResource();
		if (resource == null) {
			LOGGER.warn("Resource does not exist. Skipping processing reconciliation scripts.");
			return;
		}

		OperationProvisioningScriptsType resourceScripts = resource.getScripts();
		if (resourceScripts == null) {
			return;
		}

		PrismObject<F> user = null;
		PrismObject<ShadowType> shadow = null;

		if (context.getFocusContext() != null) {
			if (context.getFocusContext().getObjectNew() != null) {
				user = context.getFocusContext().getObjectNew();
			} else if (context.getFocusContext().getObjectOld() != null) {
				user = context.getFocusContext().getObjectOld();
			}
			// if (order == ProvisioningScriptOrderType.BEFORE) {
			// user = context.getFocusContext().getObjectOld();
			// } else if (order == ProvisioningScriptOrderType.AFTER) {
			// user = context.getFocusContext().getObjectNew();
			// } else {
			// throw new IllegalArgumentException("Unknown order "+order);
			// }
		}

		if (order == BeforeAfterType.BEFORE) {
			shadow = (PrismObject<ShadowType>) projContext.getObjectOld();
		} else if (order == BeforeAfterType.AFTER) {
			shadow = (PrismObject<ShadowType>) projContext.getObjectNew();
		} else {
			throw new IllegalArgumentException("Unknown order " + order);
		}

		ExpressionVariables variables = Utils.getDefaultExpressionVariables(user, shadow,
				projContext.getResourceShadowDiscriminator(), resource.asPrismObject(),
				context.getSystemConfiguration(), projContext);
		ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(context, projContext, task, parentResult));
		try {
		OperationProvisioningScriptsType evaluatedScript = evaluateScript(resourceScripts,
				projContext.getResourceShadowDiscriminator(), ProvisioningOperationTypeType.RECONCILE, order,
				variables, context, projContext, task, parentResult);
		for (OperationProvisioningScriptType script : evaluatedScript.getScript()) {
			Utils.setRequestee(task, context);
			provisioning.executeScript(resource.getOid(), script, task, parentResult);
			Utils.clearRequestee(task);
		}
		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}

	}

}

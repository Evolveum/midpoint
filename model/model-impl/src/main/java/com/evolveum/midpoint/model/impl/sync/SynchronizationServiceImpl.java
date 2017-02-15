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

package com.evolveum.midpoint.model.impl.sync;

import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.common.SynchronizationUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BeforeAfterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationReactionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Synchronization service receives change notifications from provisioning. It
 * decides which synchronization policy to use and evaluates it (correlation,
 * confirmation, situations, reaction, ...)
 * 
 * @author lazyman
 * @author Radovan Semancik
 *
 *         Note: don't autowire this bean by implementing class, as it is
 *         proxied by Spring AOP. Use the interface instead.
 */
@Service(value = "synchronizationService")
public class SynchronizationServiceImpl implements SynchronizationService {

	private static final Trace LOGGER = TraceManager.getTrace(SynchronizationServiceImpl.class);
	
	@Autowired(required = true)
	private ActionManager<Action> actionManager;
	
	@Autowired
	private CorrelationConfirmationEvaluator correlationConfirmationEvaluator;
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	
	@Autowired(required = true)
	private ContextFactory contextFactory;
	
	@Autowired(required = true)
	private Clockwork clockwork;
	
	@Autowired(required = true)
	private ExpressionFactory expressionFactory;
	
	@Autowired(required = true)
	private SystemObjectCache systemObjectCache;

	@Override
	public void notifyChange(ResourceObjectShadowChangeDescription change, Task task,
			OperationResult parentResult) {
		validate(change);
		Validate.notNull(parentResult, "Parent operation result must not be null.");

		boolean logDebug = isLogDebug(change);
		if (logDebug) {
			LOGGER.debug("SYNCHRONIZATION: received change notification {}", change);
		} else {
			LOGGER.trace("SYNCHRONIZATION: received change notification {}", change);
		}

		OperationResult subResult = parentResult.createSubresult(NOTIFY_CHANGE);

		PrismObject<? extends ShadowType> currentShadow = change.getCurrentShadow();
		PrismObject<? extends ShadowType> applicableShadow = currentShadow;
		if (applicableShadow == null) {
			// We need this e.g. in case of delete
			applicableShadow = change.getOldShadow();
		}

		SynchronizationEventInformation eventInfo = new SynchronizationEventInformation(applicableShadow,
				change.getSourceChannel(), task);

		try {

			ResourceType resourceType = change.getResource().asObjectable();
			PrismObject<SystemConfigurationType> configuration = systemObjectCache.getSystemConfiguration(subResult);

			ObjectSynchronizationType synchronizationPolicy = determineSynchronizationPolicy(resourceType,
					applicableShadow, configuration, task, subResult);

			if (LOGGER.isTraceEnabled()) {
				String policyDesc = null;
				if (synchronizationPolicy != null) {
					if (synchronizationPolicy.getName() == null) {
						policyDesc = "(kind=" + synchronizationPolicy.getKind() + ", intent="
								+ synchronizationPolicy.getIntent() + ", objectclass="
								+ synchronizationPolicy.getObjectClass() + ")";
					} else {
						policyDesc = synchronizationPolicy.getName();
					}
				}
				LOGGER.trace("SYNCHRONIZATION determined policy: {}", policyDesc);
			}

			if (synchronizationPolicy == null) {
				String message = "SYNCHRONIZATION no matching policy for " + applicableShadow + " ("
						+ applicableShadow.asObjectable().getObjectClass() + ") " + " on " + resourceType
						+ ", ignoring change from channel " + change.getSourceChannel();
				LOGGER.debug(message);
				subResult.recordStatus(OperationResultStatus.NOT_APPLICABLE, message);
				eventInfo.setNoSynchronizationPolicy();
				eventInfo.record(task);
				return;
			}

			if (!isSynchronizationEnabled(synchronizationPolicy)) {
				String message = "SYNCHRONIZATION is not enabled for " + resourceType
						+ " ignoring change from channel " + change.getSourceChannel();
				LOGGER.debug(message);
				subResult.recordStatus(OperationResultStatus.NOT_APPLICABLE, message);
				eventInfo.setSynchronizationNotEnabled();
				eventInfo.record(task);
				return;
			}

			// check if the kind/intent in the syncPolicy satisfy constraints
			// defined in task
			if (!satisfyTaskConstraints(synchronizationPolicy, task)) {
				LOGGER.trace(
						"SYNCHRONIZATION skipping {} because it does not match kind/intent defined in task",
						new Object[] { applicableShadow });
				subResult.recordStatus(OperationResultStatus.NOT_APPLICABLE,
						"Skipped because it does not match objectClass/kind/intent");
				eventInfo.setDoesNotMatchTaskSpecification();
				eventInfo.record(task);
				return;
			}

			if (isProtected((PrismObject<ShadowType>) currentShadow)) {
				if (StringUtils.isNotBlank(synchronizationPolicy.getIntent())) {
					List<PropertyDelta<?>> modifications = SynchronizationUtils
							.createSynchronizationTimestampsDelta(currentShadow);

					PropertyDelta<String> intentDelta = PropertyDelta.createModificationReplaceProperty(
							ShadowType.F_INTENT, currentShadow.getDefinition(),
							synchronizationPolicy.getIntent());
					modifications.add(intentDelta);

					try {
						repositoryService.modifyObject(ShadowType.class, currentShadow.getOid(),
								modifications, subResult);
						task.recordObjectActionExecuted(currentShadow, ChangeType.MODIFY, null);
					} catch (Throwable t) {
						task.recordObjectActionExecuted(currentShadow, ChangeType.MODIFY, t);
					} finally {
						task.markObjectActionExecutedBoundary();
					}

				}
				subResult.recordSuccess();
				eventInfo.record(task);
				LOGGER.debug("SYNCHRONIZATION: DONE (dry run) for protected shadow {}", currentShadow);
				return;
			}

			Class<? extends FocusType> focusType = determineFocusClass(synchronizationPolicy, resourceType);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Synchronization is enabled, focus class: {}, found applicable policy: {}",
						focusType, Utils.getPolicyDesc(synchronizationPolicy));
			}

			SynchronizationSituation situation = determineSituation(focusType, change, synchronizationPolicy,
					configuration.asObjectable(), task, subResult);
			if (logDebug) {
				LOGGER.debug("SYNCHRONIZATION: SITUATION: '{}', currentOwner={}, correlatedOwner={}",
						situation.getSituation().value(), situation.getCurrentOwner(),
						situation.getCorrelatedOwner());
			} else {
				LOGGER.trace("SYNCHRONIZATION: SITUATION: '{}', currentOwner={}, correlatedOwner={}",
						situation.getSituation().value(), situation.getCurrentOwner(),
						situation.getCorrelatedOwner());
			}
			eventInfo.setOriginalSituation(situation.getSituation());
			eventInfo.setNewSituation(situation.getSituation()); // overwritten
																	// later
																	// (TODO fix
																	// this!)

			if (change.isUnrelatedChange() || Utils.isDryRun(task)) {
				PrismObject object = null;
				if (change.getCurrentShadow() != null) {
					object = change.getCurrentShadow();
				} else if (change.getOldShadow() != null) {
					object = change.getOldShadow();
				}

				Collection modifications = SynchronizationUtils
						.createSynchronizationSituationAndDescriptionDelta(object, situation.getSituation(),
								task.getChannel(), false);
				if (StringUtils.isNotBlank(synchronizationPolicy.getIntent())) {
					modifications.add(PropertyDelta.createModificationReplaceProperty(ShadowType.F_INTENT,
							object.getDefinition(), synchronizationPolicy.getIntent()));
				}
				try {
					repositoryService.modifyObject(ShadowType.class, object.getOid(), modifications,
							subResult);
					task.recordObjectActionExecuted(object, ChangeType.MODIFY, null);
				} catch (Throwable t) {
					task.recordObjectActionExecuted(object, ChangeType.MODIFY, t);
				} finally {
					task.markObjectActionExecutedBoundary();
				}
				subResult.recordSuccess();
				eventInfo.record(task);
				LOGGER.debug("SYNCHRONIZATION: DONE (dry run/unrelated) for {}", object);
				return;
			}

			// must be here, because when the reaction has no action, the
			// situation will be not set.
			PrismObject<ShadowType> newCurrentShadow = saveSyncMetadata(
					(PrismObject<ShadowType>) currentShadow, situation, change, synchronizationPolicy, task,
					parentResult);
			if (newCurrentShadow != null) {
				change.setCurrentShadow(newCurrentShadow);
			}

			SynchronizationSituationType newSituation = reactToChange(focusType, change,
					synchronizationPolicy, situation, resourceType, logDebug, configuration, task, subResult);
			eventInfo.setNewSituation(newSituation);
			eventInfo.record(task);
			subResult.computeStatus();
			
		} catch (SystemException ex) {
			// avoid unnecessary re-wrap
			eventInfo.setException(ex);
			eventInfo.record(task);
			subResult.recordFatalError(ex);
			throw ex;
			
		} catch (Exception ex) {
			eventInfo.setException(ex);
			eventInfo.record(task);
			subResult.recordFatalError(ex);
			throw new SystemException(ex);
			
		} finally {
			task.markObjectActionExecutedBoundary();
			// if (LOGGER.isTraceEnabled()) {
			// LOGGER.trace(subResult.dump());
			// }
		}
		LOGGER.debug("SYNCHRONIZATION: DONE for {}", currentShadow);
	}

	private boolean satisfyTaskConstraints(ObjectSynchronizationType synchronizationPolicy, Task task) {
		PrismProperty<ShadowKindType> kind = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_KIND);
		if (kind != null && !kind.isEmpty()) {
			ShadowKindType kindValue = kind.getRealValue();
			ShadowKindType policyKind = synchronizationPolicy.getKind();
			if (policyKind == null) {
				policyKind = ShadowKindType.ACCOUNT; // TODO is this ok? [med]
			}
			if (!policyKind.equals(kindValue)) {
				return false;
			}
		}

		PrismProperty<String> intent = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_INTENT);
		if (intent != null && !intent.isEmpty()) {
			String intentValue = intent.getRealValue();
			if (StringUtils.isEmpty(synchronizationPolicy.getIntent())) {
				return false;
			}
			if (!synchronizationPolicy.getIntent().equals(intentValue)) {
				return false;
			}
		}

		return true;
	}

	private boolean isProtected(PrismObject<ShadowType> shadow) {
		if (shadow == null) {
			return false;
		}

		ShadowType currentShadowType = shadow.asObjectable();
		if (currentShadowType.isProtectedObject() == null) {
			return false;
		}

		return currentShadowType.isProtectedObject();
	}

	private <F extends FocusType> Class<F> determineFocusClass(
			ObjectSynchronizationType synchronizationPolicy, ResourceType resource)
					throws ConfigurationException {
		if (synchronizationPolicy == null) {
			throw new IllegalStateException("synchronizationPolicy is null");
		}
		QName focusTypeQName = synchronizationPolicy.getFocusType();
		if (focusTypeQName == null) {
			return (Class<F>) UserType.class;
		}
		ObjectTypes objectType = ObjectTypes.getObjectTypeFromTypeQName(focusTypeQName);
		if (objectType == null) {
			throw new ConfigurationException(
					"Unknown focus type " + focusTypeQName + " in synchronization policy in " + resource);
		}
		return (Class<F>) objectType.getClassDefinition();
	}

	@Override
	public ObjectSynchronizationType determineSynchronizationPolicy(ResourceType resourceType,
			PrismObject<? extends ShadowType> currentShadow,
			PrismObject<SystemConfigurationType> configuration, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		
		SynchronizationType synchronization = resourceType.getSynchronization();
		if (synchronization == null) {
			return null;
		}
		for (ObjectSynchronizationType objectSynchronization : synchronization.getObjectSynchronization()) {
			if (isPolicyApplicable(currentShadow, objectSynchronization, resourceType.asPrismObject(),
					configuration, task, result)) {
				return objectSynchronization;
			}
		}
		return null;
	}

	private boolean isPolicyApplicable(PrismObject<? extends ShadowType> currentShadow,
			ObjectSynchronizationType synchronizationPolicy, PrismObject<ResourceType> resource,
			PrismObject<SystemConfigurationType> configuration, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

		if (!SynchronizationUtils.isPolicyApplicable(currentShadow, synchronizationPolicy, resource)) {
			return false;
		}
	
		Boolean conditionResult = evaluateSynchronizationPolicyCondition(synchronizationPolicy, currentShadow,
				resource, configuration, task, result);
		if (conditionResult != null) {
			return conditionResult.booleanValue();
		}

		return true;
	}

	private Boolean evaluateSynchronizationPolicyCondition(ObjectSynchronizationType synchronizationPolicy,
			PrismObject<? extends ShadowType> currentShadow, PrismObject<ResourceType> resource,
			PrismObject<SystemConfigurationType> configuration, Task task, OperationResult result)
					throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		if (synchronizationPolicy.getCondition() == null) {
			return null;
		}
		ExpressionType conditionExpressionType = synchronizationPolicy.getCondition();
		String desc = "condition in object synchronization " + synchronizationPolicy.getName();
		ExpressionVariables variables = Utils.getDefaultExpressionVariables(null, currentShadow, null,
				resource, configuration, null);
		try {
			ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
			PrismPropertyValue<Boolean> evaluateCondition = ExpressionUtil.evaluateCondition(variables,
					conditionExpressionType, expressionFactory, desc, task, result);
			return evaluateCondition.getValue();
		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}
	}

	private boolean isLogDebug(ResourceObjectShadowChangeDescription change) {
		// Reconciliation changes are routine. Do not let it polute the
		// logfiles.
		return !SchemaConstants.CHANGE_CHANNEL_RECON_URI.equals(change.getSourceChannel());
	}

	private void validate(ResourceObjectShadowChangeDescription change) {
		Validate.notNull(change, "Resource object shadow change description must not be null.");
		Validate.isTrue(change.getCurrentShadow() != null || change.getObjectDelta() != null,
				"Object delta and current shadow are null. At least one must be provided.");
		Validate.notNull(change.getResource(), "Resource in change must not be null.");

		if (consistencyChecks) {
			if (change.getCurrentShadow() != null) {
				change.getCurrentShadow().checkConsistence();
				ShadowUtil.checkConsistence(change.getCurrentShadow(),
						"current shadow in change description");
			}
			if (change.getObjectDelta() != null) {
				change.getObjectDelta().checkConsistence();
			}
		}
	}

	// @Override
	// public void notifyFailure(ResourceOperationFailureDescription
	// failureDescription,
	// Task task, OperationResult parentResult) {
	// Validate.notNull(failureDescription, "Resource object shadow failure
	// description must not be null.");
	// Validate.notNull(failureDescription.getCurrentShadow(), "Current shadow
	// in resource object shadow failure description must not be null.");
	// Validate.notNull(failureDescription.getObjectDelta(), "Delta in resource
	// object shadow failure description must not be null.");
	// Validate.notNull(failureDescription.getResource(), "Resource in failure
	// must not be null.");
	// Validate.notNull(failureDescription.getResult(), "Result in failure
	// description must not be null.");
	// Validate.notNull(parentResult, "Parent operation result must not be
	// null.");
	//
	// LOGGER.debug("SYNCHRONIZATION: received failure notifiation {}",
	// failureDescription);
	//
	// LOGGER.error("Provisioning error: {}",
	// failureDescription.getResult().getMessage());
	//
	// // TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
	// TODO TODO TODO TODO
	// }

	private boolean isSynchronizationEnabled(ObjectSynchronizationType synchronization) {
		if (synchronization == null || synchronization.isEnabled() == null) {
			return false;
		}
		return synchronization.isEnabled();
	}

	/**
	 * XXX: in situation when one account belongs to two different idm users
	 * (repository returns only first user, method
	 * {@link com.evolveum.midpoint.model.api.ModelService#findShadowOwner(String, Task, OperationResult)}
	 * (String, com.evolveum.midpoint.schema.result.OperationResult)} ). It
	 * should be changed because otherwise we can't find
	 * {@link SynchronizationSituationType#DISPUTED} situation
	 */
	private <F extends FocusType> SynchronizationSituation determineSituation(Class<F> focusType,
			ResourceObjectShadowChangeDescription change, ObjectSynchronizationType synchronizationPolicy,
			SystemConfigurationType configurationType, Task task, OperationResult result) {

		OperationResult subResult = result.createSubresult(CHECK_SITUATION);
		LOGGER.trace("Determining situation for resource object shadow.");

		SynchronizationSituation situation = null;
		try {
			String shadowOid = getOidFromChange(change);
			Validate.notEmpty(shadowOid, "Couldn't get resource object shadow oid from change.");
			PrismObject<F> owner = repositoryService.searchShadowOwner(shadowOid,
					SelectorOptions.createCollection(GetOperationOptions.createAllowNotFound()), subResult);

			if (owner != null) {
				F ownerType = owner.asObjectable();
				LOGGER.trace("Shadow OID {} does have owner: {}", shadowOid, ownerType.getName());
				SynchronizationSituationType state = null;
				switch (getModificationType(change)) {
					case ADD:
					case MODIFY:
						// if user is found it means account/group is linked to
						// resource
						state = SynchronizationSituationType.LINKED;
						break;
					case DELETE:
						state = SynchronizationSituationType.DELETED;
				}
				situation = new SynchronizationSituation<>(ownerType, null, state);
			} else {
				LOGGER.trace("Resource object shadow doesn't have owner.");
				situation = determineSituationWithCorrelation(focusType, change, synchronizationPolicy, owner,
						configurationType, task, result);
			}
		} catch (Exception ex) {
			LOGGER.error("Error occurred during resource object shadow owner lookup.");
			throw new SystemException(
					"Error occurred during resource object shadow owner lookup, reason: " + ex.getMessage(),
					ex);
		} finally {
			subResult.computeStatus();
		}

		return situation;
	}

	private String getOidFromChange(ResourceObjectShadowChangeDescription change) {
		if (change.getCurrentShadow() != null && StringUtils.isNotEmpty(change.getCurrentShadow().getOid())) {
			return change.getCurrentShadow().getOid();
		}
		if (change.getOldShadow() != null && StringUtils.isNotEmpty(change.getOldShadow().getOid())) {
			return change.getOldShadow().getOid();
		}

		if (change.getObjectDelta() == null || StringUtils.isEmpty(change.getObjectDelta().getOid())) {
			throw new IllegalArgumentException(
					"Oid was not defined in change (not in current, old shadow, delta).");
		}

		return change.getObjectDelta().getOid();
	}

	/**
	 * Tries to match specified focus and shadow. Return true if it matches,
	 * false otherwise.
	 */
	@Override
	public <F extends FocusType> boolean matchUserCorrelationRule(PrismObject<ShadowType> shadow,
			PrismObject<F> focus, ResourceType resourceType,
			PrismObject<SystemConfigurationType> configuration, Task task, OperationResult result)
					throws ConfigurationException, SchemaException, ObjectNotFoundException,
					ExpressionEvaluationException {
		ObjectSynchronizationType synchronizationPolicy = determineSynchronizationPolicy(resourceType, shadow,
				configuration, task, result);
		Class<F> focusClass;
		// TODO is this correct? The problem is that synchronizationPolicy can
		// be null...
		if (synchronizationPolicy != null) {
			focusClass = determineFocusClass(synchronizationPolicy, resourceType);
		} else {
			focusClass = (Class) focus.asObjectable().getClass();
		}
		return correlationConfirmationEvaluator.matchUserCorrelationRule(focusClass, shadow, focus,
				synchronizationPolicy, resourceType,
				configuration == null ? null : configuration.asObjectable(), task, result);
	}

	/**
	 * account is not linked to user. you have to use correlation and
	 * confirmation rule to be sure user for this account doesn't exists
	 * resourceShadow only contains the data that were in the repository before
	 * the change. But the correlation/confirmation should work on the updated
	 * data. Therefore let's apply the changes before running
	 * correlation/confirmation
	 */
	private <F extends FocusType> SynchronizationSituation determineSituationWithCorrelation(
			Class<F> focusType, ResourceObjectShadowChangeDescription change,
			ObjectSynchronizationType synchronizationPolicy, PrismObject<F> owner,
			SystemConfigurationType configurationType, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

		if (ChangeType.DELETE.equals(getModificationType(change))) {
			// account was deleted and we know it didn't have owner
			return new SynchronizationSituation<>(owner == null ? null : owner.asObjectable(), null,
					SynchronizationSituationType.DELETED);
		}

		PrismObject<? extends ShadowType> resourceShadow = change.getCurrentShadow();

		ObjectDelta syncDelta = change.getObjectDelta();
		if (resourceShadow == null && syncDelta != null && ChangeType.ADD.equals(syncDelta.getChangeType())) {
			LOGGER.trace("Trying to compute current shadow from change delta add.");
			PrismObject<ShadowType> shadow = syncDelta.computeChangedObject(syncDelta.getObjectToAdd());
			resourceShadow = shadow;
			change.setCurrentShadow(shadow);
		}
		Validate.notNull(resourceShadow, "Current shadow must not be null.");

		ResourceType resource = change.getResource().asObjectable();
		validateResourceInShadow(resourceShadow.asObjectable(), resource);

		SynchronizationSituationType state = null;
		LOGGER.trace(
				"SYNCHRONIZATION: CORRELATION: Looking for list of {} objects based on correlation rule.",
				focusType.getSimpleName());
		List<PrismObject<F>> users = correlationConfirmationEvaluator.findFocusesByCorrelationRule(focusType,
				resourceShadow.asObjectable(), synchronizationPolicy.getCorrelation(), resource,
				configurationType, task, result);
		if (users == null) {
			users = new ArrayList<>();
		}

		if (users.size() > 1) {
			if (synchronizationPolicy.getConfirmation() == null) {
				LOGGER.trace("SYNCHRONIZATION: CONFIRMATION: no confirmation defined.");
			} else {
				LOGGER.debug(
						"SYNCHRONIZATION: CONFIRMATION: Checking objects from correlation with confirmation rule.");
				users = correlationConfirmationEvaluator.findUserByConfirmationRule(focusType, users,
						resourceShadow.asObjectable(), resource, configurationType,
						synchronizationPolicy.getConfirmation(), task, result);
			}
		}

		F user = null;
		switch (users.size()) {
			case 0:
				state = SynchronizationSituationType.UNMATCHED;
				break;
			case 1:
				switch (getModificationType(change)) {
					case ADD:
					case MODIFY:
						state = SynchronizationSituationType.UNLINKED;
						break;
					case DELETE:
						state = SynchronizationSituationType.DELETED;
						break;
				}

				user = users.get(0).asObjectable();
				break;
			default:
				state = SynchronizationSituationType.DISPUTED;
		}

		return new SynchronizationSituation(null, user, state);
	}

	private void validateResourceInShadow(ShadowType shadow, ResourceType resource) {
		if (shadow.getResource() != null || shadow.getResourceRef() != null) {
			return;
		}

		ObjectReferenceType reference = new ObjectReferenceType();
		reference.setOid(resource.getOid());
		reference.setType(ObjectTypes.RESOURCE.getTypeQName());

		shadow.setResourceRef(reference);
	}

	/**
	 * @param change
	 * @return method checks change type in object delta if available, otherwise
	 *         returns {@link ChangeType#ADD}
	 */
	private ChangeType getModificationType(ResourceObjectShadowChangeDescription change) {
		if (change.getObjectDelta() != null) {
			return change.getObjectDelta().getChangeType();
		}

		return ChangeType.ADD;
	}

	private <F extends FocusType> SynchronizationSituationType reactToChange(Class<F> focusClass,
			ResourceObjectShadowChangeDescription change, ObjectSynchronizationType synchronizationPolicy,
			SynchronizationSituation<F> situation, ResourceType resource, boolean logDebug,
			PrismObject<SystemConfigurationType> configuration, Task task, OperationResult parentResult)
					throws ConfigurationException, ObjectNotFoundException, SchemaException,
					PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException,
					CommunicationException, SecurityViolationException {

		SynchronizationSituationType newSituation = situation.getSituation();

		SynchronizationReactionType reactionDefinition = findReactionDefinition(synchronizationPolicy,
				situation, change.getSourceChannel(), resource);
		if (reactionDefinition == null) {
			LOGGER.trace("No reaction is defined for situation {} in {}", situation.getSituation(), resource);
			return newSituation;
		}

		// seems to be unused so commented it out [med]
		// PrismObject<? extends ObjectType> shadow = null;
		// if (change.getCurrentShadow() != null) {
		// shadow = change.getCurrentShadow();
		// } else if (change.getOldShadow() != null) {
		// shadow = change.getOldShadow();
		// }

		Boolean doReconciliation = determineReconciliation(synchronizationPolicy, reactionDefinition);
		if (doReconciliation == null) {
			// We have to do reconciliation if we have got a full shadow and no
			// delta.
			// There is no other good way how to reflect the changes from the
			// shadow.
			if (change.getObjectDelta() == null) {
				doReconciliation = true;
			}
		}

		Boolean limitPropagation = determinePropagationLimitation(synchronizationPolicy, reactionDefinition,
				change.getSourceChannel());
		ModelExecuteOptions options = new ModelExecuteOptions();
		options.setReconcile(doReconciliation);
		options.setLimitPropagation(limitPropagation);

		final boolean willSynchronize = isSynchronize(reactionDefinition);
		LensContext<F> lensContext = null;
		if (willSynchronize) {
			lensContext = createLensContext(focusClass, change, reactionDefinition, synchronizationPolicy,
					situation, options, configuration, parentResult);
		}

		if (LOGGER.isTraceEnabled() && lensContext != null) {
			LOGGER.trace("---[ SYNCHRONIZATION context before action execution ]-------------------------\n"
					+ "{}\n------------------------------------------", lensContext.debugDump());
		}

		if (willSynchronize) {

			// there's no point in calling executeAction without context - so
			// the actions are executed only if synchronize == true
			executeActions(reactionDefinition, lensContext, situation, BeforeAfterType.BEFORE, resource,
					logDebug, task, parentResult);

			Iterator<LensProjectionContext> iterator = lensContext.getProjectionContextsIterator();
			LensProjectionContext originalProjectionContext = iterator.hasNext() ? iterator.next() : null;

			try {
				
				clockwork.run(lensContext, task, parentResult);
				
			} catch (ConfigurationException | ObjectNotFoundException | SchemaException |
					PolicyViolationException | ExpressionEvaluationException | ObjectAlreadyExistsException |
					CommunicationException | SecurityViolationException e) {
				LOGGER.error("SYNCHRONIZATION: Error in synchronization on {} for situation {}: {}: {}. Change was {}",
						new Object[] {resource, situation.getSituation(), e.getClass().getSimpleName(), 
								e.getMessage(), change, e});
				// what to do here? We cannot throw the error back. All that the notifyChange method
				// could do is to convert it to SystemException. But that indicates an internal error and it will
				// break whatever code called the notifyChange in the first place. We do not want that.
				// If the clockwork could not do anything with the exception then perhaps nothing can be done at all.
				// So just log the error (the error should be remembered in the result and task already)
				// and then just go on.
			}

			// note: actions "AFTER" seem to be useless here (basically they
			// modify lens context - which is relevant only if followed by
			// clockwork run)
			executeActions(reactionDefinition, lensContext, situation, BeforeAfterType.AFTER, resource,
					logDebug, task, parentResult);

			if (originalProjectionContext != null) {
				newSituation = originalProjectionContext.getSynchronizationSituationResolved();
			}

		} else {
			LOGGER.trace("Skipping clockwork run on {} for situation {}, synchronize is set to false.",
					new Object[] { resource, situation.getSituation() });
		}

		return newSituation;

	}

	private Boolean determineReconciliation(ObjectSynchronizationType synchronizationPolicy,
			SynchronizationReactionType reactionDefinition) {
		if (reactionDefinition.isReconcile() != null) {
			return reactionDefinition.isReconcile();
		}
		if (synchronizationPolicy.isReconcile() != null) {
			return synchronizationPolicy.isReconcile();
		}
		return null;
	}

	private Boolean determinePropagationLimitation(ObjectSynchronizationType synchronizationPolicy,
			SynchronizationReactionType reactionDefinition, String channel) {

		if (StringUtils.isNotBlank(channel)) {
			QName channelQName = QNameUtil.uriToQName(channel);
			// Discovery channel is used when compensating some inconsistent
			// state. Therefore we do not want to propagate changes to other
			// resources. We only want to resolve the problem and continue in
			// previous provisioning/synchronization during which this
			// compensation was triggered.
			if (SchemaConstants.CHANGE_CHANNEL_DISCOVERY.equals(channelQName)
					&& SynchronizationSituationType.DELETED != reactionDefinition.getSituation()) {
				return true;
			}
		}

		if (reactionDefinition.isLimitPropagation() != null) {
			return reactionDefinition.isLimitPropagation();
		}
		if (synchronizationPolicy.isLimitPropagation() != null) {
			return synchronizationPolicy.isLimitPropagation();
		}
		return null;
	}

	@NotNull
	private <F extends FocusType> LensContext<F> createLensContext(Class<F> focusClass,
			ResourceObjectShadowChangeDescription change, SynchronizationReactionType reactionDefinition,
			ObjectSynchronizationType synchronizationPolicy, SynchronizationSituation<F> situation,
			ModelExecuteOptions options, PrismObject<SystemConfigurationType> configuration,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

		LensContext<F> context = contextFactory.createSyncContext(focusClass, change);
		context.setLazyAuditRequest(true);
		context.setSystemConfiguration(configuration);
		context.setOptions(options);

		ResourceType resource = change.getResource().asObjectable();
		if (ModelExecuteOptions.isLimitPropagation(options)) {
			context.setTriggeredResource(resource);
		}

		context.rememberResource(resource);
		PrismObject<ShadowType> shadow = getShadowFromChange(change);
		if (InternalsConfig.consistencyChecks)
			shadow.checkConsistence();

		// Projection context

		ShadowKindType kind = getKind(shadow, synchronizationPolicy);
		String intent = getIntent(shadow, synchronizationPolicy);
		boolean thombstone = isThombstone(change);
		ResourceShadowDiscriminator descr = new ResourceShadowDiscriminator(resource.getOid(), kind, intent,
				thombstone);
		LensProjectionContext projectionContext = context.createProjectionContext(descr);
		projectionContext.setResource(resource);
		projectionContext.setOid(getOidFromChange(change));
		projectionContext.setSynchronizationSituationDetected(situation.getSituation());

		// insert object delta if available in change
		ObjectDelta<? extends ShadowType> delta = change.getObjectDelta();
		if (delta != null) {
			projectionContext.setSyncDelta((ObjectDelta<ShadowType>) delta);
		} else {
			projectionContext.setSyncAbsoluteTrigger(true);
		}

		// we insert account if available in change
		PrismObject<ShadowType> currentAccount = shadow;
		if (currentAccount != null) {
			projectionContext.setLoadedObject(currentAccount);
			if (!thombstone) {
				projectionContext.setFullShadow(true);
			}
			projectionContext.setFresh(true);
		}

		if (delta != null && delta.isDelete()) {
			projectionContext.setExists(false);
		} else {
			projectionContext.setExists(true);
		}

		projectionContext.setDoReconciliation(ModelExecuteOptions.isReconcile(options));

		// Focus context
		if (situation.getCurrentOwner() != null) {
			F focusType = situation.getCurrentOwner();
			LensFocusContext<F> focusContext = context.createFocusContext();
			PrismObject<F> focusOld = focusType.asPrismObject();
			focusContext.setLoadedObject(focusOld);
		}

		// Global stuff
		ObjectReferenceType objectTemplateRef = null;
		if (reactionDefinition.getObjectTemplateRef() != null) {
			objectTemplateRef = reactionDefinition.getObjectTemplateRef();
		} else if (synchronizationPolicy.getObjectTemplateRef() != null) {
			objectTemplateRef = synchronizationPolicy.getObjectTemplateRef();
		}
		if (objectTemplateRef != null) {
			ObjectTemplateType objectTemplate = repositoryService
					.getObject(ObjectTemplateType.class, objectTemplateRef.getOid(), null, parentResult)
					.asObjectable();
			context.setFocusTemplate(objectTemplate);
		}

		return context;
	}

	protected PrismObject<ShadowType> getShadowFromChange(ResourceObjectShadowChangeDescription change) {
		if (change.getCurrentShadow() != null) {
			return (PrismObject<ShadowType>) change.getCurrentShadow();
		}

		if (change.getOldShadow() != null) {
			return (PrismObject<ShadowType>) change.getOldShadow();
		}

		return null;
	}

	private ShadowKindType getKind(PrismObject<ShadowType> shadow,
			ObjectSynchronizationType synchronizationPolicy) {
		ShadowKindType shadowKind = shadow.asObjectable().getKind();
		if (shadowKind != null) {
			return shadowKind;
		}
		if (synchronizationPolicy.getKind() != null) {
			return synchronizationPolicy.getKind();
		}
		return ShadowKindType.ACCOUNT;
	}

	private String getIntent(PrismObject<ShadowType> shadow,
			ObjectSynchronizationType synchronizationPolicy) {
		String shadowIntent = shadow.asObjectable().getIntent();
		if (shadowIntent != null) {
			return shadowIntent;
		}
		return synchronizationPolicy.getIntent();
	}

	private boolean isThombstone(ResourceObjectShadowChangeDescription change) {
		PrismObject<? extends ShadowType> shadow = null;
		if (change.getOldShadow() != null) {
			shadow = change.getOldShadow();
		} else if (change.getCurrentShadow() != null) {
			shadow = change.getCurrentShadow();
		}
		if (shadow != null) {
			if (shadow.asObjectable().isDead() != null) {
				return shadow.asObjectable().isDead().booleanValue();
			}
		}
		ObjectDelta<? extends ShadowType> objectDelta = change.getObjectDelta();
		if (objectDelta == null) {
			return false;
		}
		return objectDelta.isDelete();
	}

	private boolean isSynchronize(SynchronizationReactionType reactionDefinition) {
		if (reactionDefinition.isSynchronize() != null) {
			return reactionDefinition.isSynchronize();
		}
		return !reactionDefinition.getAction().isEmpty();
	}

	private SynchronizationReactionType findReactionDefinition(
			ObjectSynchronizationType synchronizationPolicy, SynchronizationSituation situation,
			String channel, ResourceType resource) throws ConfigurationException {
		SynchronizationReactionType defaultReaction = null;
		for (SynchronizationReactionType reaction : synchronizationPolicy.getReaction()) {
			SynchronizationSituationType reactionSituation = reaction.getSituation();
			if (reactionSituation == null) {
				throw new ConfigurationException("No situation defined for a reaction in " + resource);
			}
			if (reactionSituation.equals(situation.getSituation())) {
				if (reaction.getChannel() != null && !reaction.getChannel().isEmpty()) {
					if (reaction.getChannel().contains("") || reaction.getChannel().contains(null)) {
						defaultReaction = reaction;
					}
					if (reaction.getChannel().contains(channel)) {
						return reaction;
					} else {
						LOGGER.trace("Skipping reaction {} because the channel does not match {}", reaction,
								channel);
						continue;
					}
				} else {
					defaultReaction = reaction;
				}
			}
		}
		LOGGER.trace("Using default reaction {}", defaultReaction);
		return defaultReaction;
	}

	/**
	 * Saves situation, timestamps, kind and intent (if needed)
	 */
	private PrismObject<ShadowType> saveSyncMetadata(PrismObject<ShadowType> shadow,
			SynchronizationSituation situation, ResourceObjectShadowChangeDescription change,
			ObjectSynchronizationType synchronizationPolicy, Task task, OperationResult parentResult) {
		if (shadow == null) {
			return null;
		}

		ShadowType shadowType = shadow.asObjectable();
		// new situation description
		List<PropertyDelta<?>> deltas = SynchronizationUtils
				.createSynchronizationSituationAndDescriptionDelta(shadow, situation.getSituation(),
						change.getSourceChannel(), true);
		
		if (shadowType.getKind() == null) {
			ShadowKindType kind = synchronizationPolicy.getKind();
			if (kind == null) {
				kind = ShadowKindType.ACCOUNT;
			}
			PropertyDelta<ShadowKindType> kindDelta = PropertyDelta.createReplaceDelta(shadow.getDefinition(),
					ShadowType.F_KIND, kind);
			deltas.add(kindDelta);
		}

		if (shadowType.getIntent() == null) {
			String intent = synchronizationPolicy.getIntent();
			if (intent == null) {
				intent = SchemaConstants.INTENT_DEFAULT;
			}
			PropertyDelta<String> intentDelta = PropertyDelta.createReplaceDelta(shadow.getDefinition(),
					ShadowType.F_INTENT, intent);
			deltas.add(intentDelta);
		}

		try {
			repositoryService.modifyObject(shadowType.getClass(), shadow.getOid(), deltas, parentResult);
			ItemDelta.applyTo(deltas, shadow);
			task.recordObjectActionExecuted(shadow, ChangeType.MODIFY, null);
			return shadow;
		} catch (ObjectNotFoundException ex) {
			task.recordObjectActionExecuted(shadow, ChangeType.MODIFY, ex);
			// This may happen e.g. during some recon-livesync interactions.
			// If the shadow is gone then it is gone. No point in recording the
			// situation any more.
			LOGGER.debug(
					"Could not update situation in account, because shadow {} does not exist any more (this may be harmless)",
					shadow.getOid());
			parentResult.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
		} catch (ObjectAlreadyExistsException | SchemaException ex) {
			task.recordObjectActionExecuted(shadow, ChangeType.MODIFY, ex);
			LoggingUtils.logException(LOGGER,
					"### SYNCHRONIZATION # notifyChange(..): Save of synchronization situation failed: could not modify shadow "
							+ shadow.getOid() + ": " + ex.getMessage(),
					ex);
			parentResult.recordFatalError("Save of synchronization situation failed: could not modify shadow "
					+ shadow.getOid() + ": " + ex.getMessage(), ex);
			throw new SystemException("Save of synchronization situation failed: could not modify shadow "
					+ shadow.getOid() + ": " + ex.getMessage(), ex);
		} catch (Throwable t) {
			task.recordObjectActionExecuted(shadow, ChangeType.MODIFY, t);
			throw t;
		}

		return null;
	}

	private <F extends FocusType> void executeActions(SynchronizationReactionType reactionDef,
			LensContext<F> context, SynchronizationSituation<F> situation, BeforeAfterType order,
			ResourceType resource, boolean logDebug, Task task, OperationResult parentResult)
					throws ConfigurationException, SchemaException {

		for (SynchronizationActionType actionDef : reactionDef.getAction()) {
			if ((actionDef.getOrder() == null && order == BeforeAfterType.BEFORE)
					|| (actionDef.getOrder() != null && actionDef.getOrder() == order)) {

				String handlerUri = actionDef.getHandlerUri();
				if (handlerUri == null) {
					handlerUri = actionDef.getRef();
				}
				if (handlerUri == null) {
					LOGGER.error("Action definition in resource {} doesn't contain handler URI", resource);
					throw new ConfigurationException(
							"Action definition in resource " + resource + " doesn't contain handler URI");
				}

				Action action = actionManager.getActionInstance(handlerUri);
				if (action == null) {
					LOGGER.warn("Couldn't create action with uri '{}' in resource {}, skipping action.",
							new Object[] { handlerUri, resource });
					continue;
				}

				// TODO: legacy userTemplate

				Map<QName, Object> parameters = null;
				if (actionDef.getParameters() != null) {
					// TODO: process parameters
					// parameters = actionDef.getParameters().getAny();
				}

				if (logDebug) {
					LOGGER.debug("SYNCHRONIZATION: ACTION: Executing: {}.",
							new Object[] { action.getClass() });
				} else {
					LOGGER.trace("SYNCHRONIZATION: ACTION: Executing: {}.",
							new Object[] { action.getClass() });
				}

				action.handle(context, situation, parameters, task, parentResult);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#
	 * getName ()
	 */
	@Override
	public String getName() {
		return "model synchronization service";
	}

	private static class SynchronizationEventInformation {

		private String objectName;
		private String objectDisplayName;
		private String objectOid;
		private Throwable exception;
		private long started;
		private String channel;

		private SynchronizationInformation.Record originalStateIncrement = new SynchronizationInformation.Record();
		private SynchronizationInformation.Record newStateIncrement = new SynchronizationInformation.Record();

		public SynchronizationEventInformation(PrismObject<? extends ShadowType> currentShadow,
				String channel, Task task) {
			this.channel = channel;
			started = System.currentTimeMillis();
			if (currentShadow != null) {
				final ShadowType shadow = currentShadow.asObjectable();
				objectName = PolyString.getOrig(shadow.getName());
				objectDisplayName = StatisticsUtil.getDisplayName(shadow);
				objectOid = currentShadow.getOid();
			}
			task.recordSynchronizationOperationStart(objectName, objectDisplayName, ShadowType.COMPLEX_TYPE,
					objectOid);
			if (SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC_URI.equals(channel)) {
				// livesync processing is not controlled via model -> so we
				// cannot do this in upper layers
				task.recordIterativeOperationStart(objectName, objectDisplayName, ShadowType.COMPLEX_TYPE,
						objectOid);
			}
		}

		public void setProtected() {
			originalStateIncrement.setCountProtected(1);
			newStateIncrement.setCountProtected(1);
		}

		public void setNoSynchronizationPolicy() {
			originalStateIncrement.setCountNoSynchronizationPolicy(1);
			newStateIncrement.setCountNoSynchronizationPolicy(1);
		}

		public void setSynchronizationNotEnabled() {
			originalStateIncrement.setCountSynchronizationDisabled(1);
			newStateIncrement.setCountSynchronizationDisabled(1);
		}

		public void setDoesNotMatchTaskSpecification() {
			originalStateIncrement.setCountNotApplicableForTask(1);
			newStateIncrement.setCountNotApplicableForTask(1);
		}

		private void setSituation(SynchronizationInformation.Record increment,
				SynchronizationSituationType situation) {
			if (situation != null) {
				switch (situation) {
					case LINKED:
						increment.setCountLinked(1);
						break;
					case UNLINKED:
						increment.setCountUnlinked(1);
						break;
					case DELETED:
						increment.setCountDeleted(1);
						break;
					case DISPUTED:
						increment.setCountDisputed(1);
						break;
					case UNMATCHED:
						increment.setCountUnmatched(1);
						break;
					default:
						// noop (or throw exception?)
				}
			}
		}

		public void setOriginalSituation(SynchronizationSituationType situation) {
			setSituation(originalStateIncrement, situation);
		}

		public void setNewSituation(SynchronizationSituationType situation) {
			newStateIncrement = new SynchronizationInformation.Record(); // brutal
																			// hack,
																			// TODO
																			// fix
																			// this!
			setSituation(newStateIncrement, situation);
		}

		public void setException(Exception ex) {
			exception = ex;
		}

		public void record(Task task) {
			task.recordSynchronizationOperationEnd(objectName, objectDisplayName, ShadowType.COMPLEX_TYPE,
					objectOid, started, exception, originalStateIncrement, newStateIncrement);
			if (SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC_URI.equals(channel)) {
				// livesync processing is not controlled via model -> so we
				// cannot do this in upper layers
				task.recordIterativeOperationEnd(objectName, objectDisplayName, ShadowType.COMPLEX_TYPE,
						objectOid, started, exception);
			}
		}
	}
}

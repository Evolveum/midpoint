/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.model.sync;

import static com.evolveum.midpoint.common.InternalsConfig.consistencyChecks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.SynchronizationSituationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSynchronizationType.Reaction;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author lazyman
 */
@Service(value = "synchronizationService")
public class SynchronizationService implements ResourceObjectChangeListener {

	private static final Trace LOGGER = TraceManager.getTrace(SynchronizationService.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	private ActionManager<Action> actionManager;
	@Autowired
	private CorrelationConfirmationEvaluator correlationConfirmationEvaluator;
//	private ExpressionHandler expressionHandler;
	@Autowired
	private ChangeNotificationDispatcher notificationManager;
	@Autowired(required = true)
	private AuditService auditService;
	@Autowired(required = true)
	private PrismContext prismContext;
	@Autowired(required = true)
	private RepositoryService repositoryService;

	@PostConstruct
	public void registerForResourceObjectChangeNotifications() {
		notificationManager.registerNotificationListener(this);
	}

	@PreDestroy
	public void unregisterForResourceObjectChangeNotifications() {
		notificationManager.unregisterNotificationListener(this);
	}

	@Override
	public void notifyChange(ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult) {
		validate(change);
		Validate.notNull(parentResult, "Parent operation result must not be null.");
		
		LOGGER.debug("SYNCHRONIZATION: received change notifiation {}", change);

		OperationResult subResult = parentResult.createSubresult(NOTIFY_CHANGE);
		try {
			ResourceType resource = change.getResource().asObjectable();
		
			if (!isSynchronizationEnabled(ResourceTypeUtil.determineSynchronization(resource, UserType.class))) {
				String message = "SYNCHRONIZATION is not enabled for " + ObjectTypeUtil.toShortString(resource)
						+ " ignoring change from channel " + change.getSourceChannel();
				LOGGER.debug(message);
				subResult.recordStatus(OperationResultStatus.SUCCESS, message);
				return;
			}
			LOGGER.trace("Synchronization is enabled.");

			SynchronizationSituation situation = checkSituation(change, subResult);
			LOGGER.debug("SYNCHRONIZATION: SITUATION: '{}', {}", situation.getSituation().value(), situation.getUser());

			if (task.getExtension() != null){
				PrismProperty<Boolean> item = task.getExtension().findProperty(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
				if (item != null && !item.isEmpty()){
					if (item.getValues().size() > 1){
						throw new SchemaException("Unexpected number of values for option 'dry run'.");
					}
					
					Boolean dryRun = item.getValues().iterator().next().getValue();
					if (dryRun != null && dryRun.booleanValue()){
						PrismObject object = null;
						if (change.getCurrentShadow() != null){
							object = change.getCurrentShadow();
						} else if (change.getOldShadow() != null){
							object = change.getOldShadow();
						}
						
						XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
						Collection modifications = SynchronizationSituationUtil
								.createSynchronizationSituationAndDescriptionDelta(object,
										situation.getSituation(), task.getChannel());
//						PropertyDelta<SynchronizationSituationType> syncSituationDelta = SynchronizationSituationUtil.createSynchronizationSituationDelta(object,
//								situation.getSituation());
//						if (syncSituationDelta != null){
//							modifications.add(syncSituationDelta);
//						}
//						modifications.add(SynchronizationSituationUtil.createSynchronizationTimestampDelta(object, timestamp));
						repositoryService.modifyObject(ShadowType.class, object.getOid(), modifications, subResult);
						subResult.recordSuccess();
						return;
					}
				}
			}
			
			notifyChange(change, situation, resource, task, subResult);

			subResult.computeStatus();
		} catch (Exception ex) {
			subResult.recordFatalError(ex);
			throw new SystemException(ex);
		} finally {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(subResult.dump());
			}
		}
	}
	
	private void validate(ResourceObjectShadowChangeDescription change) {
		Validate.notNull(change, "Resource object shadow change description must not be null.");
		Validate.isTrue(change.getCurrentShadow() != null || change.getObjectDelta() != null,
				"Object delta and current shadow are null. At least one must be provided.");
		Validate.notNull(change.getResource(), "Resource in change must not be null.");
		
		if (consistencyChecks) {
			if (change.getCurrentShadow() != null) {
				change.getCurrentShadow().checkConsistence();
				ShadowUtil.checkConsistence(change.getCurrentShadow(), "current shadow in change description");
			}
			if (change.getObjectDelta() != null) {
				change.getObjectDelta().checkConsistence();
			}
		}
	}

//	@Override
//	public void notifyFailure(ResourceOperationFailureDescription failureDescription,
//			Task task, OperationResult parentResult) {
//		Validate.notNull(failureDescription, "Resource object shadow failure description must not be null.");
//		Validate.notNull(failureDescription.getCurrentShadow(), "Current shadow in resource object shadow failure description must not be null.");
//		Validate.notNull(failureDescription.getObjectDelta(), "Delta in resource object shadow failure description must not be null.");
//		Validate.notNull(failureDescription.getResource(), "Resource in failure must not be null.");
//		Validate.notNull(failureDescription.getResult(), "Result in failure description must not be null.");
//		Validate.notNull(parentResult, "Parent operation result must not be null.");
//		
//		LOGGER.debug("SYNCHRONIZATION: received failure notifiation {}", failureDescription);
//		
//		LOGGER.error("Provisioning error: {}", failureDescription.getResult().getMessage());
//		
//		// TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
//	}

	private boolean isSynchronizationEnabled(ObjectSynchronizationType synchronization) {
		if (synchronization == null || synchronization.isEnabled() == null) {
			return false;
		}
		return synchronization.isEnabled();
	}

	/**
	 * XXX: in situation when one account belongs to two different idm users
	 * (repository returns only first user, method
	 * {@link com.evolveum.midpoint.model.api.ModelService#listAccountShadowOwner(String, com.evolveum.midpoint.schema.result.OperationResult)}
	 * ). It should be changed because otherwise we can't find
	 * {@link SynchronizationSituationType#DISPUTED} situation
	 * 
	 * @param change
	 * @param result
	 * @return
	 */
	private SynchronizationSituation checkSituation(ResourceObjectShadowChangeDescription change, OperationResult result) {

		OperationResult subResult = result.createSubresult(CHECK_SITUATION);
		LOGGER.trace("Determining situation for resource object shadow.");

		SynchronizationSituation situation = null;
		try {
			String shadowOid = getOidFromChange(change);
			Validate.notEmpty(shadowOid, "Couldn't get resource object shadow oid from change.");
			PrismObject<UserType> user = repositoryService.listAccountShadowOwner(shadowOid, subResult);

			if (user != null) {
				UserType userType = user.asObjectable();
				LOGGER.trace("Shadow OID {} does have owner: {}", shadowOid, userType.getName());
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
				situation = new SynchronizationSituation(userType, state);
			} else {
				LOGGER.trace("Resource object shadow doesn't have owner.");
				situation = checkSituationWithCorrelation(change, result);
			}
		} catch (Exception ex) {
			LOGGER.error("Error occurred during resource object shadow owner lookup.");
			throw new SystemException("Error occurred during resource object shadow owner lookup, reason: "
					+ ex.getMessage(), ex);
		} finally {
			subResult.computeStatus();
		}

		LOGGER.trace("checkSituation::end - {}, {}", new Object[] {
				(situation.getUser() == null ? "null" : situation.getUser().getOid()), situation.getSituation() });

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
			throw new IllegalArgumentException("Oid was not defined in change (not in current, old shadow, delta).");
		}

		return change.getObjectDelta().getOid();
	}

	/**
	 * account is not linked to user. you have to use correlation and
	 * confirmation rule to be sure user for this account doesn't exists
	 * resourceShadow only contains the data that were in the repository before
	 * the change. But the correlation/confirmation should work on the updated
	 * data. Therefore let's apply the changes before running
	 * correlation/confirmation
	 */
	private SynchronizationSituation checkSituationWithCorrelation(ResourceObjectShadowChangeDescription change,
			OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

		if (ChangeType.DELETE.equals(getModificationType(change))) {
			// account was deleted and we know it didn't have owner
			return new SynchronizationSituation(null, SynchronizationSituationType.DELETED);
		}

		PrismObject<? extends ShadowType> resourceShadow = change.getCurrentShadow();

		ObjectDelta syncDelta = change.getObjectDelta();
		if (resourceShadow == null && syncDelta != null && ChangeType.ADD.equals(syncDelta.getChangeType())) {
			LOGGER.trace("Trying to compute current shadow from change delta add.");
			PrismObject<? extends ShadowType> shadow = syncDelta.computeChangedObject(syncDelta
					.getObjectToAdd());
			resourceShadow = shadow;
			change.setCurrentShadow(shadow);
		}
		Validate.notNull(resourceShadow, "Current shadow must not be null.");

		ResourceType resource = change.getResource().asObjectable();
		validateResourceInShadow(resourceShadow.asObjectable(), resource);

		ObjectSynchronizationType synchronization = ResourceTypeUtil.determineSynchronization(resource, UserType.class);

		SynchronizationSituationType state = null;
		LOGGER.trace("SYNCHRONIZATION: CORRELATION: Looking for list of users based on correlation rule.");
		List<PrismObject<UserType>> users = correlationConfirmationEvaluator.findUsersByCorrelationRule(resourceShadow.asObjectable(),
				synchronization.getCorrelation(), resource, result);
		if (users == null) {
			users = new ArrayList<PrismObject<UserType>>();
		}

		if (users.size() > 1) {
			if (synchronization.getConfirmation() == null) {
				LOGGER.trace("SYNCHRONIZATION: CONFIRMATION: no confirmation defined.");
			} else {
				LOGGER.debug("SYNCHRONIZATION: CONFIRMATION: Checking users from correlation with confirmation rule.");
				users = correlationConfirmationEvaluator.findUserByConfirmationRule(users, resourceShadow.asObjectable(), resource, 
						synchronization.getConfirmation(), result);
			}
		}

		UserType user = null;
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

		return new SynchronizationSituation(user, state);
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

	private void notifyChange(ResourceObjectShadowChangeDescription change, SynchronizationSituation situation,
			ResourceType resource, Task task, OperationResult parentResult) {

		// Audit:request
		AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.SYNCHRONIZATION, AuditEventStage.REQUEST);
		if (change.getObjectDelta() != null) {
			auditRecord.addDelta(new ObjectDeltaOperation(change.getObjectDelta()));
		}
		if (change.getCurrentShadow() != null) {
			auditRecord.setTarget(change.getCurrentShadow());
		} else if (change.getOldShadow() != null) {
			auditRecord.setTarget(change.getOldShadow());
		}
		auditRecord.setChannel(change.getSourceChannel());
		auditService.audit(auditRecord, task);

		ObjectSynchronizationType synchronization = ResourceTypeUtil.determineSynchronization(resource, UserType.class);
		List<Action> actions = findActionsForReaction(synchronization.getReaction(), situation.getSituation());
		
		if (actions.isEmpty()) {

			LOGGER.warn("Skipping synchronization on resource: {}. Actions was not found.",
					new Object[] { resource.getName() });
			return;
		}

		try {
			LOGGER.trace("Updating user started.");
			String userOid = situation.getUser() == null ? null : situation.getUser().getOid();
			saveExecutedSituationDescription(auditRecord.getTarget(), situation, change, parentResult);
			if (userOid == null && situation.getSituation() == SynchronizationSituationType.DELETED){
				LOGGER.trace("Detected DELETE change by synchronization, but the account does not have any owner in the midpoint.");
				parentResult.recordSuccess();
				return;
			}
			
			ObjectTemplateType userTemplate = null;
			if (synchronization != null){
			ObjectReferenceType userTemplateRef = synchronization.getObjectTemplateRef();
			if (userTemplateRef != null){
				userTemplate = repositoryService.getObject(ObjectTemplateType.class, userTemplateRef.getOid(), parentResult).asObjectable();
			}
			} 
			
			for (Action action : actions) {
				LOGGER.debug("SYNCHRONIZATION: ACTION: Executing: {}.", new Object[] { action.getClass() });

				userOid = action.executeChanges(userOid, change, userTemplate, situation.getSituation(), auditRecord, task,
						parentResult);
			}
			parentResult.recordSuccess();
			LOGGER.trace("Updating user finished.");
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "### SYNCHRONIZATION # notifyChange(..): Synchronization action failed",
					ex);
			parentResult.recordFatalError("Synchronization action failed.", ex);
			throw new SystemException("Synchronization action failed, reason: " + ex.getMessage(), ex);
		}

	}

	private <T extends ObjectType> void saveExecutedSituationDescription(PrismObject<T> object,
			SynchronizationSituation situation, ResourceObjectShadowChangeDescription change,
			OperationResult parentResult) {
		if (object == null) {
			return;
		}
		
		T objectType = object.asObjectable();
		// new situation description
		XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
		List<PropertyDelta<?>> syncSituationDeltas = SynchronizationSituationUtil
				.createSynchronizationSituationAndDescriptionDelta(object, situation.getSituation(), change.getSourceChannel());
		// refresh situation
//		PropertyDelta<SynchronizationSituationType> syncSituationDelta = SynchronizationSituationUtil.createSynchronizationSituationDelta(object,
//				situation.getSituation());
//		if (syncSituationDelta != null){
//		syncSituationDeltas.add(syncSituationDelta);
//		}
//		syncSituationDeltas.add(SynchronizationSituationUtil.createSynchronizationTimestampDelta(object, timestamp));
//		
		try {

			repositoryService.modifyObject(objectType.getClass(), object.getOid(), syncSituationDeltas, parentResult);
		} catch (ObjectNotFoundException ex) {
			if (change.getObjectDelta() != null && change.getObjectDelta().isDelete()){
				LOGGER.warn("Could not update situation in account, because account does not exist. {}", ex);
				parentResult.recordPartialError("Could not update situation in account, because account does not exist. ", ex);
				return;
			}
			LoggingUtils.logException(LOGGER,
					"### SYNCHRONIZATION # notifyChange(..): Synchronization action failed. Could not modify object "
							+ ObjectTypeUtil.toShortString(objectType), ex);
			parentResult.recordFatalError(
					"Synchronization action failed, Could not modify object "
							+ ObjectTypeUtil.toShortString(objectType), ex);
			throw new SystemException("Synchronization action failed, Could not modify object "
					+ ObjectTypeUtil.toShortString(objectType) + " reason: " + ex.getMessage(), ex);
		} catch (ObjectAlreadyExistsException ex) {
			LoggingUtils.logException(LOGGER,
					"### SYNCHRONIZATION # notifyChange(..): Synchronization action failed. Could not modify object "
							+ ObjectTypeUtil.toShortString(objectType), ex);
			parentResult.recordFatalError(
					"Synchronization action failed, Could not modify object "
							+ ObjectTypeUtil.toShortString(objectType), ex);
			throw new SystemException("Synchronization action failed, Could not modify object "
					+ ObjectTypeUtil.toShortString(objectType) + " reason: " + ex.getMessage(), ex);
		} catch (SchemaException ex) {
			LoggingUtils.logException(LOGGER,
					"### SYNCHRONIZATION # notifyChange(..): Synchronization action failed. Could not modify object "
							+ ObjectTypeUtil.toShortString(objectType), ex);
			parentResult.recordFatalError(
					"Synchronization action failed, Could not modify object "
							+ ObjectTypeUtil.toShortString(objectType), ex);
			throw new SystemException("Synchronization action failed, Could not modify object "
					+ ObjectTypeUtil.toShortString(objectType) + " reason: " + ex.getMessage(), ex);
		}

	}

	private List<Action> findActionsForReaction(List<Reaction> reactions, SynchronizationSituationType situation) {
		List<Action> actions = new ArrayList<Action>();
		if (reactions == null) {
			return actions;
		}

		Reaction reaction = null;
		for (Reaction react : reactions) {
			if (react.getSituation() == null) {
				LOGGER.warn("Reaction ({}) doesn't contain situation element, skipping.", reactions.indexOf(react));
				continue;
			}
			if (situation.equals(react.getSituation())) {
				reaction = react;
				break;
			}
		}

		if (reaction == null) {
			LOGGER.warn("Reaction on situation {} was not found.", situation);
			return actions;
		}

		List<Reaction.Action> actionList = reaction.getAction();
		for (Reaction.Action actionXml : actionList) {
			if (actionXml == null) {
				LOGGER.warn("Reaction ({}) doesn't contain action element, skipping.", reactions.indexOf(reaction));
				return actions;
			}
			if (actionXml.getRef() == null) {
				LOGGER.warn("Reaction ({}): Action element doesn't contain ref attribute, skipping.",
						reactions.indexOf(reaction));
				return actions;
			}

			Action action = actionManager.getActionInstance(actionXml.getRef());
			if (action == null) {
				LOGGER.warn("Couldn't create action with uri '{}' for reaction {}, skipping action.", new Object[] {
						actionXml.getRef(), reactions.indexOf(reaction) });
				continue;
			}
			action.setParameters(actionXml.getAny());
			actions.add(action);
		}

		return actions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#getName
	 * ()
	 */
	@Override
	public String getName() {
		return "model synchronization service";
	}
}

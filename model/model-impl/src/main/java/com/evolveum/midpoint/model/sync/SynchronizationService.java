/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.sync;

import static com.evolveum.midpoint.common.CompiletimeConfig.CONSISTENCY_CHECKS;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.expr.ExpressionHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowFailureDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSynchronizationType.Reaction;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

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
	private ExpressionHandler expressionHandler;
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
		
		if (CONSISTENCY_CHECKS) {
			if (change.getCurrentShadow() != null) {
				change.getCurrentShadow().checkConsistence();
				ResourceObjectShadowUtil.checkConsistence(change.getCurrentShadow(), "current shadow in change description");
			}
			if (change.getObjectDelta() != null) {
				change.getObjectDelta().checkConsistence();
			}
		}
	}

	@Override
	public void notifyFailure(ResourceObjectShadowFailureDescription failureDescription,
			Task task, OperationResult parentResult) {
		Validate.notNull(failureDescription, "Resource object shadow failure description must not be null.");
		Validate.notNull(failureDescription.getCurrentShadow(), "Current shadow in resource object shadow failure description must not be null.");
		Validate.notNull(failureDescription.getObjectDelta(), "Delta in resource object shadow failure description must not be null.");
		Validate.notNull(failureDescription.getResource(), "Resource in failure must not be null.");
		Validate.notNull(failureDescription.getResult(), "Result in failure description must not be null.");
		Validate.notNull(parentResult, "Parent operation result must not be null.");
		
		LOGGER.debug("SYNCHRONIZATION: received failure notifiation {}", failureDescription);
		
		LOGGER.error("Provisioning error: {}", failureDescription.getResult().getMessage());
		
		// TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
	}

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
	 * 
	 * @throws SynchronizationException
	 */
	private SynchronizationSituation checkSituationWithCorrelation(ResourceObjectShadowChangeDescription change,
			OperationResult result) throws SynchronizationException, SchemaException {

		if (ChangeType.DELETE.equals(getModificationType(change))) {
			// account was deleted and we know it didn't have owner
			return new SynchronizationSituation(null, SynchronizationSituationType.DELETED);
		}

		PrismObject<? extends ResourceObjectShadowType> resourceShadow = change.getCurrentShadow();

		ObjectDelta syncDelta = change.getObjectDelta();
		if (resourceShadow == null && syncDelta != null && ChangeType.ADD.equals(syncDelta.getChangeType())) {
			LOGGER.trace("Trying to compute current shadow from change delta add.");
			PrismObject<? extends ResourceObjectShadowType> shadow = syncDelta.computeChangedObject(syncDelta
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
		List<PrismObject<UserType>> users = findUsersByCorrelationRule(resourceShadow.asObjectable(),
				synchronization.getCorrelation(), resource, result);
		if (users == null) {
			users = new ArrayList<PrismObject<UserType>>();
		}

		if (users.size() > 1) {
			if (synchronization.getConfirmation() == null) {
				LOGGER.trace("SYNCHRONIZATION: CONFIRMATION: no confirmation defined.");
			} else {
				LOGGER.debug("SYNCHRONIZATION: CONFIRMATION: Checking users from correlation with confirmation rule.");
				users = findUserByConfirmationRule(users, resourceShadow.asObjectable(),
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

	private void validateResourceInShadow(ResourceObjectShadowType shadow, ResourceType resource) {
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
			auditRecord.addDelta(change.getObjectDelta());
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
			for (Action action : actions) {
				LOGGER.debug("SYNCHRONIZATION: ACTION: Executing: {}.", new Object[] { action.getClass() });

				userOid = action.executeChanges(userOid, change, situation.getSituation(), auditRecord, task,
						parentResult);
			}
			parentResult.recordSuccess();
			LOGGER.trace("Updating user finished.");
		} catch (SynchronizationException ex) {
			LoggingUtils.logException(LOGGER, "### SYNCHRONIZATION # notifyChange(..): Synchronization action failed",
					ex);
			parentResult.recordFatalError("Synchronization action failed.", ex);
			throw new SystemException("Synchronization action failed, reason: " + ex.getMessage(), ex);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "### SYNCHRONIZATION # notifyChange(..): Unexpected "
					+ "error occurred, synchronization action failed", ex);
			parentResult.recordFatalError("Unexpected error occurred, synchronization action failed.", ex);
			throw new SystemException("Unexpected error occurred, synchronization action failed, reason: "
					+ ex.getMessage(), ex);
		}
		// Note: The EXECUTION stage audit records are recorded in individual
		// actions

	}

	private <T extends ObjectType> void saveExecutedSituationDescription(PrismObject<T> object,
			SynchronizationSituation situation, ResourceObjectShadowChangeDescription change,
			OperationResult parentResult) {
		if (object == null) {
			return;
		}

		List<PrismPropertyValue> syncSituationDescriptionList = new ArrayList<PrismPropertyValue>();
		// old situation description
		// SynchronizationSituationDescriptionType syncSituationDescription =
		// new SynchronizationSituationDescriptionType();
		// syncSituationDescription.setSituation(situation.getSituation());
		// syncSituationDescription.setChannel(change.getSourceChannel());
		// syncSituationDescription.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));
		// syncSituationDescriptionList.add(new
		// PrismPropertyValue(syncSituationDescription));

		// refresh situation
		// situation = checkSituation(change, parentResult);
		List<PropertyDelta> syncSituationDeltas = new ArrayList<PropertyDelta>();
		PropertyDelta syncSituationDelta = PropertyDelta.createReplaceDelta(object.getDefinition(),
				ResourceObjectShadowType.F_SYNCHRONIZATION_SITUATION, situation.getSituation());
		syncSituationDeltas.add(syncSituationDelta);

		// new situation description
		SynchronizationSituationDescriptionType syncSituationDescription = new SynchronizationSituationDescriptionType();
		syncSituationDescription.setSituation(situation.getSituation());
		syncSituationDescription.setChannel(change.getSourceChannel());
		syncSituationDescription.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));
		syncSituationDescriptionList.add(new PrismPropertyValue(syncSituationDescription));

		syncSituationDelta = PropertyDelta.createDelta(new ItemPath(
				ResourceObjectShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION), object.getDefinition());
		syncSituationDelta.addValuesToAdd(syncSituationDescriptionList);
		syncSituationDeltas.add(syncSituationDelta);

		T objectType = object.asObjectable();

		try {

			repositoryService.modifyObject(objectType.getClass(), object.getOid(), syncSituationDeltas, parentResult);
		} catch (ObjectNotFoundException ex) {
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

	private List<PrismObject<UserType>> findUsersByCorrelationRule(ResourceObjectShadowType currentShadow,
			QueryType query, ResourceType resourceType, OperationResult result) throws SynchronizationException {

		if (query == null) {
			LOGGER.warn(
					"Correlation rule for resource '{}' doesn't contain query, " + "returning empty list of users.",
					resourceType);
			return null;
		}

		Element element = query.getFilter();
		if (element == null) {
			LOGGER.warn("Correlation rule for resource '{}' doesn't contain query filter, "
					+ "returning empty list of users.", resourceType);
			return null;
		}
		
		ObjectQuery q = null;
		try {
			q = QueryConvertor.createObjectQuery(UserType.class, query, prismContext);
			q = updateFilterWithAccountValues(currentShadow, q, "Correlation expression", result);
			if (q == null) {
				// Null is OK here, it means that the value in the filter
				// evaluated
				// to null and the processing should be skipped
				return null;
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
					SchemaDebugUtil.prettyPrint(query));
			throw new SynchronizationException("Couldn't convert query.", ex);
		}
		List<PrismObject<UserType>> users = null;
		try {
//			query = new QueryType();
//			query.setFilter(filter);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("SYNCHRONIZATION: CORRELATION: expression for results in filter\n{}", new Object[] {
						currentShadow, SchemaDebugUtil.prettyPrint(query) });
			}
			PagingType paging = new PagingType();
//			ObjectQuery q = QueryConvertor.createObjectQuery(UserType.class, query, prismContext);
			users = repositoryService.searchObjects(UserType.class, q, result);

			if (users == null) {
				users = new ArrayList<PrismObject<UserType>>();
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't search users in repository, based on filter (simplified)\n{}.",
					ex, q.dump());
			throw new SynchronizationException("Couldn't search users in repository, based on filter (See logs).", ex);
		}

		LOGGER.debug("SYNCHRONIZATION: CORRELATION: expression for {} returned {} users: {}", new Object[] {
				currentShadow, users.size(), PrettyPrinter.prettyPrint(users, 3) });
		return users;
	}

	private List<PrismObject<UserType>> findUserByConfirmationRule(List<PrismObject<UserType>> users,
			ResourceObjectShadowType currentShadow, ExpressionType expression, OperationResult result)
			throws SynchronizationException {

		List<PrismObject<UserType>> list = new ArrayList<PrismObject<UserType>>();
		for (PrismObject<UserType> user : users) {
			try {
				UserType userType = user.asObjectable();
				boolean confirmedUser = expressionHandler.evaluateConfirmationExpression(userType, currentShadow,
						expression, result);
				if (user != null && confirmedUser) {
					list.add(user);
				}
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't confirm user {}", ex, user.getName());
				throw new SynchronizationException("Couldn't confirm user " + user.getName(), ex);
			}
		}

		LOGGER.debug("SYNCHRONIZATION: CONFIRMATION: expression for {} matched {} users.", new Object[] {
				currentShadow, list.size() });
		return list;
	}

	private ObjectQuery updateFilterWithAccountValues(ResourceObjectShadowType currentShadow, ObjectQuery query,
			String shortDesc, OperationResult result) throws SynchronizationException {
		LOGGER.trace("updateFilterWithAccountValues::begin");
		if (query.getFilter() == null) {
			return null;
		}

		try {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Transforming search filter from:\n{}", query.dump());
			}
//			Document document = DOMUtil.getDocument();

//			Element and = document.createElementNS(SchemaConstants.NS_QUERY, "and");
//			document.appendChild(and);
//			and.appendChild(QueryUtil.createTypeFilter(document, ObjectTypes.USER.getObjectTypeUri()));
//			Element equal = null;
//			if (SchemaConstants.NS_QUERY.equals(filter.getNamespaceURI()) && "equal".equals(filter.getLocalName())) {
//				equal = (Element) document.adoptNode(filter.cloneNode(true));
//				document.appendChild(equal);
//
//				Element path = findChildElement(equal, SchemaConstants.NS_QUERY, "path");
//				if (path != null) {
//					equal.removeChild(path);
//				}
//
//				Element valueExpressionElement = findChildElement(equal, SchemaConstants.NS_C, "expression");
//				if (valueExpressionElement == null) {
//					// Compatibility
//					valueExpressionElement = findChildElement(equal, SchemaConstants.NS_C, "valueExpression");
//				}
			Element valueExpressionElement = query.getFilter().getExpression();
			if (valueExpressionElement == null && (((PropertyValueFilter) query.getFilter()).getValues() == null
					|| ((PropertyValueFilter) query.getFilter()).getValues().isEmpty())) {
				LOGGER.warn("No valueExpression in rule for {}", currentShadow);
				return null;
			}
//				if (valueExpressionElement != null) {
//					equal.removeChild(valueExpressionElement);
//					copyNamespaceDefinitions(equal, valueExpressionElement);

//					Element refElement = findChildElement(valueExpressionElement, SchemaConstants.NS_C, "ref");
//					if (refElement == null) {
//						throw new SchemaException("No <ref> element in valueExpression in correlation rule for "
//								+ currentShadow.getResource());
//					}
//					QName ref = DOMUtil.resolveQName(refElement);

//					Element value = document.createElementNS(SchemaConstants.NS_QUERY, "value");
//					equal.appendChild(value);
//					Document document = DOMUtil.getDocument();
//					Element attribute = document.createElementNS(ref.getNamespaceURI(), ref.getLocalPart());
					ExpressionType valueExpression = prismContext.getPrismJaxbProcessor().toJavaValue(
							valueExpressionElement, ExpressionType.class);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Filter transformed to expression\n{}", valueExpression);
					}
					String expressionResult = expressionHandler.evaluateExpression(currentShadow, valueExpression,
							shortDesc, result);

					if (StringUtils.isEmpty(expressionResult)) {
						LOGGER.debug("Result of search filter expression was null or empty. Expression: {}",
								valueExpression);
						return null;
					}
					// TODO: log more context
					LOGGER.trace("Search filter expression in the rule for {} evaluated to {}.", new Object[] {
							currentShadow, expressionResult });
//					attribute.setTextContent(expressionResult);
//					value.appendChild(attribute);
//					and.appendChild(equal);
					if (query.getFilter() instanceof EqualsFilter){
						((EqualsFilter)query.getFilter()).setValue(new PrismPropertyValue(expressionResult));
						query.getFilter().setExpression(null);
					}
//				} else {
//					LOGGER.warn("No valueExpression in rule for OID {}", currentShadow.getOid());
//				}
//			}
//			filter = equal;
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Transforming filter to:\n{}", query.getFilter().dump());
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't transform filter.", ex);
			throw new SynchronizationException("Couldn't transform filter, reason: " + ex.getMessage(), ex);
		}

		LOGGER.trace("updateFilterWithAccountValues::end");
		return query;
	}

	private void copyNamespaceDefinitions(Element from, Element to) {
		NamedNodeMap attributes = from.getAttributes();
		List<Attr> xmlns = new ArrayList<Attr>();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node node = attributes.item(i);
			if (!(node instanceof Attr)) {
				continue;
			}
			xmlns.add((Attr) attributes.item(i));
		}
		for (Attr attr : xmlns) {
			from.removeAttributeNode(attr);
			to.setAttributeNode(attr);
		}
	}

	private Element findChildElement(Element element, String namespace, String name) {
		NodeList list = element.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && namespace.equals(node.getNamespaceURI())
					&& name.equals(node.getLocalName())) {
				return (Element) node;
			}
		}
		return null;
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

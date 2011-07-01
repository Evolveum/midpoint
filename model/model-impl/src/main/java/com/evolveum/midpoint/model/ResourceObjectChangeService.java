/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.sync.Action;
import com.evolveum.midpoint.model.sync.ActionManager;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.model.xpath.SchemaHandling;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.patch.PatchException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.EmptyType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeDeletionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationType.Reaction;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.SystemFaultType;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;
import com.evolveum.midpoint.xml.schema.ExpressionHolder;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author Vilo Repan
 */
@Service
public class ResourceObjectChangeService implements ResourceObjectChangeListenerPortType {

	private static transient Trace trace = TraceManager.getTrace(ResourceObjectChangeService.class);
	@Autowired(required = true)
	private ProvisioningService provisioning;
	@Autowired(required = true)
	private RepositoryService repository;
	@Autowired(required = true)
	private ActionManager<Action> actionManager;
	@Autowired(required = true)
	private SchemaHandling schemaHandling;
	private PatchXml patchXml = new PatchXml();

	@Override
	public EmptyType notifyChange(ResourceObjectShadowChangeDescriptionType change) throws FaultMessage {
		trace.info("### MODEL # Enter notifyChange({})", DebugUtil.prettyPrint(change));

		ObjectChangeType objectChange = change.getObjectChange();
		if (objectChange == null) {
			FaultType fault = new IllegalArgumentFaultType();
			fault.setMessage("Object change not defined.");

			trace.error("### MODEL # Fault notifyChange(..): Object change not defined.");
			throw new FaultMessage(fault.getMessage(), fault);
		}

		ResourceObjectShadowType resourceShadow = change.getShadow();
		if (resourceShadow == null) {
			if (change.getObjectChange() instanceof ObjectChangeAdditionType) {
				// There may not be a previous shadow in addition. But in that
				// case we have (almost)
				// everything in the ObjectChangeType - almost everything except
				// OID. But we can live with that.
				resourceShadow = (ResourceObjectShadowType) ((ObjectChangeAdditionType) change
						.getObjectChange()).getObject();
			} else {
				trace.error("### MODEL # Fault notifyChange(..): Change doesn't contain ResourceObjectShadow");
				throw createFaultMessage("Change doesn't contain ResourceObjectShadow.",
						new SystemFaultType(), null);
			}
		}

		ResourceType resource = change.getResource();
		if (resource == null) {
			trace.error("### MODEL # Fault notifyChange(..): Change doesn't contain resource");
			throw createFaultMessage("Change doesn't contain resource", null, null);
		}

		ResourceObjectShadowType shadowAfterChange = null;
		try {
			shadowAfterChange = getObjectAfterChange(resourceShadow, objectChange);
		} catch (PatchException ex) {
			throw createFaultMessage("The changes cannot be applied to the shadow",
					new IllegalArgumentFaultType(), ex);
		}

		SynchronizationType synchronization = resource.getSynchronization();
		if (synchronization == null) {
			trace.warn("Skipping synchronization on resource: {}. Synchronization element not defined.",
					resource.getName());
			trace.warn("### MODEL # Exit notifyChange(...): Skipping synchronization, synchronization element not defined.");
			return new EmptyType();
		}

		SituationState state = checkSituation(change);
		if (state.situation == null) {
			trace.error(
					"### MODEL # Fault notifyChange(..): Change on account with oid '{}' couldn't match to any of defined situation.",
					resourceShadow.getOid());
			throw createFaultMessage("Change on account with oid '" + resourceShadow.getOid()
					+ "' couldn't match to any of defined situation.", null, null);
		}

		trace.info("SITUATION: Determined situation {} for shadow {} (user {})", new Object[] {
				state.situation, DebugUtil.prettyPrint(resourceShadow), DebugUtil.prettyPrint(state.user) });

		notifyChange(change, state, resource, shadowAfterChange);

		trace.info("### MODEL # Exit notifyChange(...)");
		return new EmptyType();
	}

	private void notifyChange(ResourceObjectShadowChangeDescriptionType change, SituationState state,
			ResourceType resource, ResourceObjectShadowType shadowAfterChange) throws FaultMessage {
		SynchronizationType synchronization = resource.getSynchronization();

		List<Action> actions = findActionsForReaction(synchronization.getReaction(), state.situation);
		if (actions.isEmpty()) {
			trace.warn("Skipping synchronization on resource: {}. Action not found.", resource.getName());
			return;
		}

		// Some of the following methods assume that the resource is in the
		// shadow is resolved.
		// Also methods in actions assume this
		// So now let's make sure it is.

		if (change.getShadow() != null && change.getShadow().getResource() == null) {
			// This should hold under interface contract, but let's be on the
			// safe side
			if (change.getShadow().getResourceRef() != null) {
				if (!change.getShadow().getResourceRef().getOid().equals(resource.getOid())) {
					throw createFaultMessage("OID of resource does not match OID in shadow resourceRef",
							new SystemFaultType(), null);
				}
			}
			change.getShadow().setResource(resource);
		}

		try {
			trace.trace("Updating user started.");
			String userOid = state.user == null ? null : state.user.getOid();
			for (Action action : actions) {
				trace.debug("ACTION: Executing: {}.", action.getClass());

				// TODO: fix operation result type
				userOid = action.executeChanges(userOid, change, state.situation, shadowAfterChange,
						new OperationResultType());
			}
			trace.trace("Updating user finished.");
		} catch (SynchronizationException ex) {
			trace.error("### MODEL # Fault notifyChange(..): Synchronization action failed, reason: {}.",
					ex.getMessage());
			throw createFaultMessage("Synchronization action failed, reason: " + getMessage(ex),
					new SystemFaultType(), ex);
		} catch (Exception ex) {
			trace.error(
					"### MODEL # Fault notifyChange(..): Unexpected error occured, synchronization action failed, reason: {}.",
					ex.getMessage());
			throw createFaultMessage("Unexpected error occured, synchronization action failed.", null, ex);
		}
	}

	private String getMessage(SynchronizationException ex) {
		return ex.getMessage();
	}

	private List<Action> findActionsForReaction(List<Reaction> reactions,
			SynchronizationSituationType situation) {
		List<Action> actions = new ArrayList<Action>();
		if (reactions == null) {
			return actions;
		}

		Reaction reaction = null;
		for (Reaction react : reactions) {
			if (react.getSituation() == null) {
				trace.warn("Reaction ({}) doesn't contain situation element, skipping.",
						reactions.indexOf(react));
				continue;
			}

			if (situation.equals(react.getSituation())) {
				reaction = react;
				break;
			}
		}

		if (reaction == null) {
			trace.warn("Reaction on situation {} was not found.", situation);
			return actions;
		}

		List<Reaction.Action> actionList = reaction.getAction();
		for (Reaction.Action actionXml : actionList) {
			if (actionXml == null) {
				trace.warn("Reaction ({}) doesn't contain action element, skipping.",
						reactions.indexOf(reaction));
				return actions;
			}
			if (actionXml.getRef() == null) {
				trace.warn("Reaction ({}): Action element doesn't contain ref attribute, skipping.",
						reactions.indexOf(reaction));
				return actions;
			}

			Action action = actionManager.getActionInstance(actionXml.getRef());
			if (action == null) {
				trace.warn("Couln't create action with uri '{}' for reaction {}, skipping action.",
						actionXml.getRef(), reactions.indexOf(reaction));
				continue;
			}
			action.setParameters(actionXml.getAny());
			actions.add(action);
		}

		return actions;
	}

	private FaultMessage createFaultMessage(String message, FaultType faultType, Exception ex) {
		if (faultType == null) {
			faultType = new SystemFaultType();
		}
		if (faultType.getMessage() == null || faultType.getMessage().isEmpty()) {
			if (ex != null) {
				faultType.setMessage(message + ":" + ex.getMessage());
			} else {
				faultType.setMessage(message);
			}
		}

		// Message in the fault is not important. It may get lost in ESB.
		return new FaultMessage(message, faultType, ex);
	}

	private Element updateFilterWithAccountValues(ResourceObjectShadowType resourceObjectShadow,
			Element filter) {
		trace.trace("updateFilterWithAccountValues::begin");
		if (filter == null) {
			return null;
		}

		try {
			trace.trace("Transforming search filter from:\n{}", DOMUtil.printDom(filter.getOwnerDocument()));
			Document document = DOMUtil.getDocument();
			String prefix = filter.lookupPrefix(SchemaConstants.NS_C) == null ? "c" : filter
					.lookupPrefix(SchemaConstants.NS_C);
			Element and = document.createElementNS(SchemaConstants.NS_C, prefix + ":and");
			document.appendChild(and);
			Element type = document.createElementNS(SchemaConstants.NS_C, prefix + ":type");
			type.setAttribute("uri",
					"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd#UserType");
			and.appendChild(type);
			if (SchemaConstants.NS_C.equals(filter.getNamespaceURI())
					&& "equal".equals(filter.getLocalName())) {
				Element equal = (Element) document.adoptNode(filter.cloneNode(true));
				and.appendChild(equal);

				Element path = findChildElement(equal, SchemaConstants.NS_C, "path");
				if (path != null) {
					equal.removeChild(path);
				}

				Element valueExpression = findChildElement(equal, SchemaConstants.NS_C, "valueExpression");
				if (valueExpression != null) {
					equal.removeChild(valueExpression);
					String ref = valueExpression.getAttribute("ref");
					String namespace = filter.getOwnerDocument().getNamespaceURI();
					if (ref.contains(":")) {
						String pref = ref.substring(0, ref.indexOf(":"));
						namespace = filter.lookupNamespaceURI(pref);
					}

					Element value = document.createElementNS(SchemaConstants.NS_C, prefix + ":value");
					equal.appendChild(value);
					Element attribute = document.createElementNS(namespace, ref);
					String expressionResult = resolveValueExpression(path, valueExpression,
							resourceObjectShadow);
					// TODO: log more context
					trace.debug("Search filter expression in the rule for OID {} evaluated to '{}'",
							resourceObjectShadow.getOid(), expressionResult);
					attribute.setTextContent(expressionResult);
					value.appendChild(attribute);
				} else {
					trace.warn("No valueExpression in rule for OID {}", resourceObjectShadow.getOid());
				}
			}

			filter = and;
			trace.trace("Transforming filter to:\n{}", DOMUtil.printDom(filter.getOwnerDocument()));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		trace.trace("updateFilterWithAccountValues::end");
		return filter;
	}

	// XXX: what to do with path element?
	private String resolveValueExpression(Element path, Element expression,
			ResourceObjectShadowType resourceObjectShadow) {
		return schemaHandling.evaluateCorrelationExpression(resourceObjectShadow, new ExpressionHolder(
				expression));
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

	private List<UserType> findUsersByCorrelationRule(ResourceObjectShadowType resourceShadow, QueryType query)
			throws FaultMessage {
		List<UserType> users = new ArrayList<UserType>();

		if (query == null) {
			trace.error("Corrrelation rule for resource '{}' doesn't contain query, "
					+ "returning empty list of users.", resourceShadow.getName());
			return users;
		}

		Element element = query.getFilter();
		if (element == null) {
			trace.error("Corrrelation rule for resource '{}' doesn't contain query, "
					+ "returning empty list of users.", resourceShadow.getName());
			return users;
		}
		Element filter = updateFilterWithAccountValues(resourceShadow, element);
		try {
			ObjectFactory of = new ObjectFactory();
			query = of.createQueryType();
			query.setFilter(filter);
			trace.debug("CORRELATION: expression for OID {} results in filter {}", resourceShadow.getOid(),
					DebugUtil.prettyPrint(query));
			PagingType paging = new PagingType();
			ObjectListType container = provisioning.searchObjects(query, paging, new OperationResult(
					"Search Objects"));
			if (container == null) {
				return users;
			}

			List<ObjectType> objects = container.getObject();
			for (ObjectType object : objects) {
				if (object instanceof UserType) {
					users.add((UserType) object);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		trace.debug("CORRELATION: expression for OID {} returned {} users.", resourceShadow.getOid(),
				users.size());
		return users;
	}

	private List<UserType> findUserByConfirmationRule(List<UserType> users,
			ResourceObjectShadowType resourceObjectShadowType, ExpressionHolder expression) {
		List<UserType> list = new ArrayList<UserType>();
		for (UserType user : users) {
			if (user != null && schemaHandling.confirmUser(user, resourceObjectShadowType, expression)) {
				list.add(user);
			}
		}

		trace.debug("CONFIRMATION: expression for OID {} matched {} users.",
				resourceObjectShadowType.getOid(), list.size());
		return list;
	}

	// TODO: in situation when one account belongs to two different idm users
	// (repository returns only first user). It should be changed because
	// otherwise
	// we can't check SynchronizationSituationType.CONFLICT situation
	private SituationState checkSituation(ResourceObjectShadowChangeDescriptionType change)
			throws FaultMessage {
		trace.trace("checkSituation::begin");
		if (change.getShadow() != null) {
			trace.trace("Determining situation for OID {}", change.getShadow().getOid());
		} else {
			trace.trace("Determining situation for [new resource object]");
		}
		ResourceObjectShadowType resourceShadow = change.getShadow();
		ModificationType modType = getModificationType(change.getObjectChange());

		// It is better to get resource from change. The resource object may
		// have only resourceRef
		ResourceType resource = change.getResource();
		SynchronizationType synchronization = resource.getSynchronization();

		SynchronizationSituationType situation = null;
		UserType userType = null;
		try {

			if (resourceShadow != null && resourceShadow.getOid() != null
					&& !resourceShadow.getOid().isEmpty()) {
				// XXX: HACK! we should be calling method listAccountShadowOwner
				// on provisioning service, OPENIDM-284
				userType = repository.listAccountShadowOwner(resourceShadow.getOid(), new OperationResult(
						"List Account Shadow Owner"));
			}

			if (userType != null) {
				trace.trace("Shadow OID {} does have owner: {}", change.getShadow().getOid(),
						userType.getOid());

				switch (modType) {
					case ADD:
					case MODIFY:
						// if user is found it means account/group is linked to
						// resource
						situation = SynchronizationSituationType.CONFIRMED;
						break;
					case DELETE:
						situation = SynchronizationSituationType.DELETED;
				}

			} else {
				trace.trace("Resource object shadow doesn't have owner.");
				// account is not linked to user. you have to use correlation
				// and confirmation
				// rule to be shure user for this account doesn't exists
				// resourceShadow only contains the data that were in the
				// repository before the change. But the
				// correlation/confirmation
				// should work on the updated data. Therefore let's apply the
				// changes
				// before running correlation/confirmation

				ResourceObjectShadowType objectAfterChange = null;
				try {
					objectAfterChange = getObjectAfterChange(resourceShadow, change.getObjectChange());
				} catch (PatchException ex) {
					throw createFaultMessage("Application of changes to object failed", null, ex);
				}

				trace.trace("Object after change: {}", DebugUtil.prettyPrint(objectAfterChange));

				List<UserType> users = findUsersByCorrelationRule(objectAfterChange,
						synchronization.getCorrelation());
				if (synchronization.getConfirmation() == null) {
					if (resourceShadow != null) {
						trace.debug(
								"CONFIRMATION: No expression for OID {}, accepting all results of correlation",
								resourceShadow.getOid());
					} else {
						trace.debug("CONFIRMATION: No expression for [new resource object], accepting all results of correlation");
					}
				} else {
					users = findUserByConfirmationRule(users, objectAfterChange, new ExpressionHolder(
							synchronization.getConfirmation()));
				}
				switch (users.size()) {
					case 0:
						situation = SynchronizationSituationType.UNMATCHED;
						break;
					case 1:
						if (ModificationType.ADD.equals(modType)) {
							situation = SynchronizationSituationType.FOUND;
						} else {
							situation = SynchronizationSituationType.UNASSIGNED;
						}
						userType = users.get(0);
						break;
					default:
						situation = SynchronizationSituationType.DISPUTED;
				}

			}
		} catch (Exception ex) {
			trace.error("Error occured during resource object shadow owner lookup.");
			throw createFaultMessage("Error occured during resource object shadow owner lookup.", null, ex);
		}

		trace.trace("checkSituation::end - '{}', '{}'", (userType == null ? "null" : userType.getOid()),
				situation);
		return new SituationState(userType, situation);
	}

	/**
	 * Apply the changes to the provided shadow.
	 * 
	 * @param resourceShadow
	 *            shadow with some data
	 * @param objectChange
	 *            changes to be applied
	 */
	private ResourceObjectShadowType getObjectAfterChange(ResourceObjectShadowType resourceShadow,
			ObjectChangeType objectChange) throws PatchException {
		if (objectChange instanceof ObjectChangeAdditionType) {
			ObjectChangeAdditionType objectAddition = (ObjectChangeAdditionType) objectChange;
			ObjectType object = objectAddition.getObject();
			if (object instanceof ResourceObjectShadowType) {
				return (ResourceObjectShadowType) object;
			} else {
				throw new IllegalArgumentException("The changed object is not a shadow, it is "
						+ object.getClass().getName());
			}
		} else if (objectChange instanceof ObjectChangeModificationType) {
			ObjectChangeModificationType objectModification = (ObjectChangeModificationType) objectChange;
			ObjectModificationType modification = objectModification.getObjectModification();
			ResourceObjectShadowType changedResourceShadow = unmarshalChangedObject(patchXml
					.applyDifferences(modification, resourceShadow));
			return changedResourceShadow;
		} else if (objectChange instanceof ObjectChangeDeletionType) {
			// in case of deletion the object has already all that it can have
			return resourceShadow;
		} else {
			throw new IllegalArgumentException("Unknown change type " + objectChange.getClass().getName());
		}

	}

	@SuppressWarnings("unchecked")
	private ResourceObjectShadowType unmarshalChangedObject(String patchedObject) throws PatchException {
		Validate.notNull(patchedObject);
		ResourceObjectShadowType changedResourceShadow = null;
		try {
			JAXBElement<ResourceObjectShadowType> el = (JAXBElement<ResourceObjectShadowType>) JAXBUtil
					.unmarshal(patchedObject);
			changedResourceShadow = el.getValue();
			trace.debug("Shadow after change: {}", patchedObject);
		} catch (JAXBException ex) {
			trace.error("Failed to unmarshal object: {}", patchedObject, ex);
			throw new PatchException("Failed to unmarshal object: " + patchedObject, ex);
		}
		return changedResourceShadow;
	}

	private ModificationType getModificationType(ObjectChangeType change) throws FaultMessage {
		if (change == null) {
			throw new IllegalArgumentException("Can't check modification type, object change type is null.");
		}

		if (change instanceof ObjectChangeAdditionType) {
			return ModificationType.ADD;
		} else if (change instanceof ObjectChangeModificationType) {
			return ModificationType.MODIFY;
		} else if (change instanceof ObjectChangeDeletionType) {
			return ModificationType.DELETE;
		}

		throw createFaultMessage("Unknown modification type - change '" + change.getClass() + "'.", null,
				null);
	}

	private enum ModificationType {

		ADD, DELETE, MODIFY;
	}

	private static class SituationState {

		UserType user;
		SynchronizationSituationType situation;

		private SituationState(UserType user, SynchronizationSituationType situation) {
			this.user = user;
			this.situation = situation;
		}
	}
}

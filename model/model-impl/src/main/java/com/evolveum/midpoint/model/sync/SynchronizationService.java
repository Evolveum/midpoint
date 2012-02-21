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

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.expr.ExpressionException;
import com.evolveum.midpoint.model.expr.ExpressionHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.ResultArrayList;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationType.Reaction;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

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
        Validate.notNull(change, "Resource object shadow change description must not be null.");
        Validate.isTrue(change.getCurrentShadow() != null || change.getObjectDelta() != null,
                "Object delta and change are null. At least one must be provided.");
        Validate.notNull(change.getResource(), "Resource in change must not be null.");
        Validate.notNull(parentResult, "Parent operation result must not be null.");
        LOGGER.debug("SYNC NEW CHANGE NOTIFICATION");

        OperationResult subResult = parentResult.createSubresult(NOTIFY_CHANGE);
        try {
            ResourceType resource = change.getResource().asObjectable();

            if (!isSynchronizationEnabled(resource.getSynchronization())) {
                String message = "Synchronization is not enabled for " + ObjectTypeUtil.toShortString(resource) + " ignoring change from channel " + change.getSourceChannel();
                LOGGER.debug(message);
                subResult.recordStatus(OperationResultStatus.SUCCESS, message);
                return;
            }
            LOGGER.trace("Synchronization is enabled.");

            SynchronizationSituation situation = checkSituation(change, subResult);
            LOGGER.debug("SITUATION: {} {}",
                    situation.getSituation().value(), ObjectTypeUtil.toShortString(situation.getUser()));

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

    private boolean isSynchronizationEnabled(SynchronizationType synchronization) {
        if (synchronization == null || synchronization.isEnabled() == null) {
            return false;
        }
        return synchronization.isEnabled();
    }

    /**
     * XXX: in situation when one account belongs to two different idm users (repository returns only first user,
     * method {@link com.evolveum.midpoint.model.api.ModelService#listAccountShadowOwner(String, com.evolveum.midpoint.schema.result.OperationResult)}).
     * It should be changed because otherwise we can't find {@link SynchronizationSituationType#DISPUTED} situation
     *
     * @param change
     * @param result
     * @return
     */
    private SynchronizationSituation checkSituation(ResourceObjectShadowChangeDescription change,
            OperationResult result) {

        OperationResult subResult = result.createSubresult(CHECK_SITUATION);
        LOGGER.trace("Determining situation for resource object shadow.");

        SynchronizationSituation situation = null;
        try {
            String shadowOid = getOidFromChange(change);
            Validate.notEmpty(shadowOid, "Couldn't get resource object shadow oid from change.");
            PrismObject<UserType> user = controller.listAccountShadowOwner(shadowOid, subResult);

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

        LOGGER.trace("checkSituation::end - {}, {}",
                new Object[]{(situation.getUser() == null ? "null" : situation.getUser().getOid()),
                        situation.getSituation()});

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
    private SynchronizationSituation checkSituationWithCorrelation(
            ResourceObjectShadowChangeDescription change, OperationResult result) throws SynchronizationException {

        if (ChangeType.DELETE.equals(getModificationType(change))) {
            //account was deleted and we know it didn't have owner
            return new SynchronizationSituation(null, SynchronizationSituationType.DELETED);
        }

        ResourceObjectShadowType resourceShadow = change.getCurrentShadow().asObjectable();
        ObjectDelta syncDelta = change.getObjectDelta();
        if (resourceShadow == null && syncDelta != null
                && ChangeType.ADD.equals(syncDelta.getChangeType())) {
            LOGGER.debug("Trying to compute current shadow from change delta add.");
            PrismObject<ResourceObjectShadowType> shadow =
                    syncDelta.computeChangedObject(syncDelta.getObjectToAdd());
            resourceShadow = (ResourceObjectShadowType) shadow.asObjectable();
            change.setCurrentShadow(shadow);
        }
        Validate.notNull(resourceShadow, "Current shadow must not be null.");

        ResourceType resource = change.getResource().asObjectable();
        validateResourceInShadow(resourceShadow, resource);

        SynchronizationType synchronization = resource.getSynchronization();

        SynchronizationSituationType state = null;
        LOGGER.debug("CORRELATION: Looking for list of users based on correlation rule.");
        List<PrismObject<UserType>> users = findUsersByCorrelationRule(resourceShadow, synchronization.getCorrelation(), result);
        if (users == null) {
            users = new ArrayList<PrismObject<UserType>>();
        }

        if (users.size() > 1) {
            if (synchronization.getConfirmation() == null) {
                LOGGER.debug("CONFIRMATION: no confirmation defined.");
            } else {
                LOGGER.debug("CONFIRMATION: Checking users from correlation with confirmation rule.");
                users = findUserByConfirmationRule(users, resourceShadow, synchronization.getConfirmation(), result);
            }
        } else {
            LOGGER.debug("CORRELATION: found {} users.", users.size());
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
     * @return method checks change type in object delta if available, otherwise returns {@link ChangeType#ADD}
     */
    private ChangeType getModificationType(ResourceObjectShadowChangeDescription change) {
        if (change.getObjectDelta() != null) {
            return change.getObjectDelta().getChangeType();
        }

        return ChangeType.ADD;
    }

    private void notifyChange(ResourceObjectShadowChangeDescription change,
            SynchronizationSituation situation, ResourceType resource, Task task,
            OperationResult parentResult) {

        // Audit:request
        AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.SYNCHRONIZATION,
                AuditEventStage.REQUEST);
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

        SynchronizationType synchronization = resource.getSynchronization();
        List<Action> actions = findActionsForReaction(synchronization.getReaction(), situation.getSituation());
        if (actions.isEmpty()) {
            LOGGER.warn("Skipping synchronization on resource: {}. Actions was not found.",
                    new Object[]{resource.getName()});
            return;
        }

        try {
            LOGGER.trace("Updating user started.");
            String userOid = situation.getUser() == null ? null : situation.getUser().getOid();
            for (Action action : actions) {
                LOGGER.debug("ACTION: Executing: {}.", new Object[]{action.getClass()});

                userOid = action.executeChanges(userOid, change, situation.getSituation(), auditRecord, task, parentResult);
            }
            parentResult.recordSuccess();
            LOGGER.trace("Updating user finished.");
        } catch (SynchronizationException ex) {
            LoggingUtils.logException(LOGGER,
                    "### SYNCHRONIZATION # notifyChange(..): Synchronization action failed", ex);
            parentResult.recordFatalError("Synchronization action failed.", ex);
            throw new SystemException("Synchronization action failed, reason: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "### SYNCHRONIZATION # notifyChange(..): Unexpected "
                    + "error occurred, synchronization action failed", ex);
            parentResult.recordFatalError("Unexpected error occurred, synchronization action failed.", ex);
            throw new SystemException("Unexpected error occurred, synchronization action failed, reason: "
                    + ex.getMessage(), ex);
        }
        // Note: The EXECUTION stage audit records are recorded in individual actions

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
                LOGGER.warn("Reaction ({}) doesn't contain situation element, skipping.",
                        reactions.indexOf(react));
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
                LOGGER.warn("Reaction ({}) doesn't contain action element, skipping.",
                        reactions.indexOf(reaction));
                return actions;
            }
            if (actionXml.getRef() == null) {
                LOGGER.warn("Reaction ({}): Action element doesn't contain ref attribute, skipping.",
                        reactions.indexOf(reaction));
                return actions;
            }

            Action action = actionManager.getActionInstance(actionXml.getRef());
            if (action == null) {
                LOGGER.warn("Couldn't create action with uri '{}' for reaction {}, skipping action.",
                        new Object[]{actionXml.getRef(), reactions.indexOf(reaction)});
                continue;
            }
            action.setParameters(actionXml.getAny());
            actions.add(action);
        }

        return actions;
    }

    private List<PrismObject<UserType>> findUsersByCorrelationRule(ResourceObjectShadowType currentShadow,
            QueryType query, OperationResult result) throws SynchronizationException {

        if (query == null) {
            LOGGER.error("Correlation rule for resource '{}' doesn't contain query, "
                    + "returning empty list of users.", currentShadow.getName());
            return null;
        }

        Element element = query.getFilter();
        if (element == null) {
            LOGGER.error("Correlation rule for resource '{}' doesn't contain query, "
                    + "returning empty list of users.", currentShadow.getName());
            return null;
        }
        Element filter = updateFilterWithAccountValues(currentShadow, element, result);
        if (filter == null) {
            LOGGER.error("Couldn't create search filter from correlation rule.");
            return null;
        }
        ResultList<PrismObject<UserType>> users = null;
        try {
            query = new ObjectFactory().createQueryType();
            query.setFilter(filter);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("CORRELATION: expression for OID {} results in filter {}", new Object[]{
                        currentShadow.getOid(), SchemaDebugUtil.prettyPrint(query)});
            }
            PagingType paging = new PagingType();
            users = controller.searchObjects(UserType.class, query, paging, result);

            if (users == null) {
                users = new ResultArrayList<PrismObject<UserType>>();
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER,
                    "Couldn't search users in repository, based on filter (simplified)\n{}.", ex,
                    SchemaDebugUtil.prettyPrint(filter));
            throw new SynchronizationException(
                    "Couldn't search users in repository, based on filter (See logs).", ex);
        }

        LOGGER.debug("CORRELATION: expression for OID {} returned {} users.",
                new Object[]{currentShadow.getOid(), users.size()});
        return users;
    }

    private List<PrismObject<UserType>> findUserByConfirmationRule(List<PrismObject<UserType>> users, ResourceObjectShadowType currentShadow,
            ExpressionType expression, OperationResult result) throws SynchronizationException {

        List<PrismObject<UserType>> list = new ArrayList<PrismObject<UserType>>();
        for (PrismObject<UserType> user : users) {
            try {
            	UserType userType = user.asObjectable();
                boolean confirmedUser = expressionHandler.evaluateConfirmationExpression(userType,
                        currentShadow, expression, result);
                if (user != null && confirmedUser) {
                    list.add(user);
                }
            } catch (ExpressionException ex) {
                LoggingUtils.logException(LOGGER, "Couldn't confirm user {}", ex, user.getName());
                throw new SynchronizationException("Couldn't confirm user " + user.getName(), ex);
            }
        }

        LOGGER.debug("CONFIRMATION: expression for OID {} matched {} users.", new Object[]{
                currentShadow.getOid(), list.size()});
        return list;
    }

    private Element updateFilterWithAccountValues(ResourceObjectShadowType currentShadow,
            Element filter, OperationResult result) throws SynchronizationException {
        LOGGER.trace("updateFilterWithAccountValues::begin");
        if (filter == null) {
            return null;
        }

        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Transforming search filter from:\n{}",
                        DOMUtil.printDom(filter.getOwnerDocument()));
            }
            Document document = DOMUtil.getDocument();

            Element and = document.createElementNS(SchemaConstants.NS_C, "and");
            document.appendChild(and);
            and.appendChild(QueryUtil.createTypeFilter(document, ObjectTypes.USER.getObjectTypeUri()));
            Element equal = null;
            if (SchemaConstants.NS_C.equals(filter.getNamespaceURI())
                    && "equal".equals(filter.getLocalName())) {
                equal = (Element) document.adoptNode(filter.cloneNode(true));

                Element path = findChildElement(equal, SchemaConstants.NS_C, "path");
                if (path != null) {
                    equal.removeChild(path);
                }

                Element valueExpressionElement = findChildElement(equal, SchemaConstants.NS_C,
                        "valueExpression");
                if (valueExpressionElement != null) {
                    equal.removeChild(valueExpressionElement);
                    copyNamespaceDefinitions(equal, valueExpressionElement);

                    Element refElement = findChildElement(valueExpressionElement, SchemaConstants.NS_C, "ref");
                    QName ref = DOMUtil.resolveQName(refElement);

                    Element value = document.createElementNS(SchemaConstants.NS_C, "value");
                    equal.appendChild(value);
                    Element attribute = document.createElementNS(ref.getNamespaceURI(), ref.getLocalPart());
                    ExpressionType valueExpression = XmlTypeConverter.toJavaValue(valueExpressionElement,
                            ExpressionType.class);
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Filter transformed to expression\n{}", valueExpression);
                    }
                    String expressionResult = expressionHandler.evaluateExpression(currentShadow,
                            valueExpression, result);

                    if (StringUtils.isEmpty(expressionResult)) {
                        LOGGER.debug("Result of search filter expression was null or empty. Expression: {}", valueExpression);
                        return null;
                    }
                    // TODO: log more context
                    LOGGER.debug("Search filter expression in the rule for OID {} evaluated to {}.",
                            new Object[]{currentShadow.getOid(), expressionResult});
                    attribute.setTextContent(expressionResult);
                    value.appendChild(attribute);
                    and.appendChild(equal);
                } else {
                    LOGGER.warn("No valueExpression in rule for OID {}", currentShadow.getOid());
                }
            }
            filter = and;
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Transforming filter to:\n{}", DOMUtil.printDom(filter.getOwnerDocument()));
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't transform filter.", ex);
            throw new SynchronizationException("Couldn't transform filter, reason: " + ex.getMessage(), ex);
        }

        LOGGER.trace("updateFilterWithAccountValues::end");
        return filter;
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
}

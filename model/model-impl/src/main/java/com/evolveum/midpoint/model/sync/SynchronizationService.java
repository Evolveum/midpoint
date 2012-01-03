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

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.expr.ExpressionException;
import com.evolveum.midpoint.model.expr.ExpressionHandler;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationType.Reaction;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
    @Autowired
    private SchemaRegistry schemaRegistry;

    @PostConstruct
    public void registerForResourceObjectChangeNotifications() {
        notificationManager.registerNotificationListener(this);
    }

    @PreDestroy
    public void unregisterForResourceObjectChangeNotifications() {
        notificationManager.unregisterNotificationListener(this);
    }

    @Override
    public void notifyChange(ResourceObjectShadowChangeDescriptionType change, OperationResult parentResult) {
        Validate.notNull(change, "Resource object shadow change description must not be null.");
        Validate.notNull(change.getObjectChange(), "Object change in change description must not be null.");
        Validate.notNull(change.getResource(), "Resource in change must not be null.");
        Validate.notNull(parentResult, "Parent operation result must not be null.");

        OperationResult subResult = parentResult.createSubresult(NOTIFY_CHANGE);
        try {
            ResourceType resource = change.getResource();
            if (resource == null) {
                String message = "Resource definition not found in change.";
                LOGGER.error(message);
                subResult.recordFatalError(message);
                return;
            }
            LOGGER.trace("Resource definition found in change.");

            if (!isSynchronizationEnabled(resource.getSynchronization())) {
                String message = "Synchronization is not enabled for " + ObjectTypeUtil.toShortString(resource) + " ignoring change from channel " + change.getSourceChannel();
                LOGGER.debug(message);
                subResult.recordStatus(OperationResultStatus.SUCCESS, message);
                return;
            }
            LOGGER.trace("Synchronization is enabled.");

            ResourceObjectShadowType objectShadow = change.getShadow();
            if (objectShadow == null && (change.getObjectChange() instanceof ObjectChangeAdditionType)) {
                // There may not be a previous shadow in addition. But in that
                // case we have (almost) everything in the ObjectChangeType -
                // almost everything except OID. But we can live with that.
                objectShadow = (ResourceObjectShadowType) ((ObjectChangeAdditionType) change
                        .getObjectChange()).getObject();
            }
            if (objectShadow == null) {
                throw new IllegalArgumentException("Change doesn't contain ResourceObjectShadow.");
            }

            ResourceObjectShadowType objectShadowAfterChange = getObjectAfterChange(change.getResource(), objectShadow,
                    change.getObjectChange());

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Resource object shadow after change resolved.");
                LOGGER.trace(JAXBUtil.silentMarshalWrap(objectShadowAfterChange));
            }
            SynchronizationSituation situation = checkSituation(change, objectShadowAfterChange, subResult);

            LOGGER.debug("SITUATION: {} ()", situation.getSituation().value(), ObjectTypeUtil.toShortString(situation.getUser()));

            notifyChange(change, situation, resource, objectShadowAfterChange, subResult);
        } catch (Exception ex) {
            throw new SystemException(ex);
        } finally {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(subResult.dump());
            }
        }
    }

    /**
     * Apply the changes to the provided shadow.
     *
     * @param objectShadow shadow with some data
     * @param change       changes to be applied
     */
    @SuppressWarnings("unchecked")
    private ResourceObjectShadowType getObjectAfterChange(ResourceType resource, ResourceObjectShadowType objectShadow,
            ObjectChangeType change) throws SchemaException {
        LOGGER.trace("Resolving resource object shadow after change.");
        if (change instanceof ObjectChangeAdditionType) {
            ObjectChangeAdditionType objectAddition = (ObjectChangeAdditionType) change;
            ObjectType object = objectAddition.getObject();
            if (object instanceof ResourceObjectShadowType) {
                return (ResourceObjectShadowType) object;
            } else {
                throw new IllegalArgumentException("The changed object is not a shadow, it is "
                        + object.getClass().getName());
            }
        } else if (change instanceof ObjectChangeModificationType) {
            AccountShadowType account = (AccountShadowType) objectShadow;
            ObjectDefinition<AccountShadowType> definition = RefinedResourceSchema.getRefinedSchema(resource,
                    schemaRegistry).getObjectDefinition(account);

            MidPointObject<AccountShadowType> shadowObject = definition.parseObjectType(account);

            ObjectChangeModificationType objectModification = (ObjectChangeModificationType) change;
            ObjectDelta<AccountShadowType> delta = ObjectDelta.createDelta(objectModification.getObjectModification(), definition);

            delta.applyTo(shadowObject);

            //we set object type to null, than it will be parsed, not only returned
            shadowObject.setObjectType(null);
            return shadowObject.getOrParseObjectType();
        } else if (change instanceof ObjectChangeDeletionType) {
            // in case of deletion the object has already all that it can have
            return objectShadow;
        } else {
            throw new IllegalArgumentException("Unknown change type " + change.getClass().getName());
        }
    }

    private boolean isSynchronizationEnabled(SynchronizationType synchronization) {
        if (synchronization == null || synchronization.isEnabled() == null) {
            return false;
        }
        return synchronization.isEnabled();
    }

    // XXX: in situation when one account belongs to two different idm users
    // (repository returns only first user). It should be changed because
    // otherwise we can't check SynchronizationSituationType.CONFLICT situation
    private SynchronizationSituation checkSituation(ResourceObjectShadowChangeDescriptionType change,
            ResourceObjectShadowType objectShadowAfterChange, OperationResult result) {
        OperationResult subResult = result.createSubresult(CHECK_SITUATION);

        if (change.getShadow() != null) {
            LOGGER.trace("Determining situation for OID {}.", new Object[]{change.getShadow().getOid()});
        } else {
            LOGGER.trace("Determining situation for new resource object (shadow OID {}).", objectShadowAfterChange.getOid());
        }
        ModificationType modification = getModificationType(change.getObjectChange());
        SynchronizationSituation situation = null;
        try {
            UserType user = null;
            ResourceObjectShadowType shadow = null;
            ResourceObjectShadowType shadowFromChange = change.getShadow();
            if (shadowFromChange != null && shadowFromChange.getOid() != null
                    && !shadowFromChange.getOid().isEmpty()) {
                user = controller.listAccountShadowOwner(shadowFromChange.getOid(), subResult);
                shadow = shadowFromChange;

            } else if (objectShadowAfterChange != null && objectShadowAfterChange.getOid() != null
                    && !objectShadowAfterChange.getOid().isEmpty()) {
                user = controller.listAccountShadowOwner(objectShadowAfterChange.getOid(), subResult);
                shadow = objectShadowAfterChange;
            }

            if (user != null) {
                LOGGER.trace("Shadow OID {} does have owner: {}", shadow.getOid(), user.getOid());
                SynchronizationSituationType state = null;
                switch (modification) {
                    case ADD:
                    case MODIFY:
                        // if user is found it means account/group is linked to
                        // resource
                        state = SynchronizationSituationType.LINKED;
                        break;
                    case DELETE:
                        state = SynchronizationSituationType.DELETED;
                }
                situation = new SynchronizationSituation(user, state);
            } else {
                LOGGER.trace("Resource object shadow doesn't have owner.");
                situation = checkSituationWithCorrelation(change, objectShadowAfterChange, modification,
                        result);
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
            ResourceObjectShadowChangeDescriptionType change,
            ResourceObjectShadowType objectShadowAfterChange, ModificationType modification,
            OperationResult result) throws SynchronizationException {

        ResourceObjectShadowType resourceShadow = change.getShadow();
        // It is better to get resource from change. The resource object may
        // have only resourceRef
        ResourceType resource = change.getResource();
        SynchronizationType synchronization = resource.getSynchronization();

        SynchronizationSituationType state = null;
        LOGGER.debug("CORRELATION: Looking for list of users based on correlation rule.");
        List<UserType> users = findUsersByCorrelationRule(objectShadowAfterChange,
                synchronization.getCorrelation(), result);
        if (synchronization.getConfirmation() == null) {
            if (resourceShadow != null) {
                LOGGER.debug("CONFIRMATION: No expression for {}, accepting all results of correlation",
                        new Object[]{ObjectTypeUtil.toShortString(resourceShadow)});
            } else {
                LOGGER.debug("CONFIRMATION: No expression for [new resource object], accepting all results of correlation");
            }
        } else {
            LOGGER.debug("CONFIRMATION: Checking users from correlation with confirmation rule.");
            users = findUserByConfirmationRule(users, objectShadowAfterChange,
                    synchronization.getConfirmation(), result);
        }

        if (users == null) {
            return new SynchronizationSituation(null, SynchronizationSituationType.UNMATCHED);
        }

        UserType user = null;
        switch (users.size()) {
            case 0:
                state = SynchronizationSituationType.UNMATCHED;
                break;
            case 1:
                switch (modification) {
                    case ADD:
                        state = SynchronizationSituationType.UNLINKED;
                        break;
                    case DELETE:
                        state = SynchronizationSituationType.DELETED;
                        break;
                    case MODIFY:
                        state = SynchronizationSituationType.LINKED;
                        break;
                }

                user = users.get(0);
                break;
            default:
                state = SynchronizationSituationType.DISPUTED;
        }

        return new SynchronizationSituation(user, state);
    }

    private ModificationType getModificationType(ObjectChangeType change) {
        if (change instanceof ObjectChangeAdditionType) {
            return ModificationType.ADD;
        } else if (change instanceof ObjectChangeModificationType) {
            return ModificationType.MODIFY;
        } else if (change instanceof ObjectChangeDeletionType) {
            return ModificationType.DELETE;
        }

        throw new SystemException("Unknown modification type - change '" + change.getClass() + "'.");
    }

    private enum ModificationType {

        ADD, DELETE, MODIFY;
    }

    private void notifyChange(ResourceObjectShadowChangeDescriptionType change,
            SynchronizationSituation situation, ResourceType resource,
            ResourceObjectShadowType objectShadowAfterChange, OperationResult parentResult) {
        SynchronizationType synchronization = resource.getSynchronization();
        List<Action> actions = findActionsForReaction(synchronization.getReaction(), situation.getSituation());
        if (actions.isEmpty()) {
            LOGGER.warn("Skipping synchronization on resource: {}. Actions was not found.",
                    new Object[]{resource.getName()});
            return;
        }

        if (change.getShadow() != null && change.getShadow().getResource() == null) {
            // This should hold under interface contract, but let's be on the
            // safe side
            if (change.getShadow().getResourceRef() != null) {
                if (!change.getShadow().getResourceRef().getOid().equals(resource.getOid())) {
                    String message = "OID of resource does not match OID in shadow resourceRef";
                    parentResult.recordFatalError(message);
                    throw new SystemException(message);
                }
            }
            change.getShadow().setResource(resource);
        }

        try {
            LOGGER.trace("Updating user started.");
            String userOid = situation.getUser() == null ? null : situation.getUser().getOid();
            for (Action action : actions) {
                LOGGER.debug("ACTION: Executing: {}.", new Object[]{action.getClass()});

                userOid = action.executeChanges(userOid, change, situation.getSituation(),
                        objectShadowAfterChange, parentResult);
            }
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

        parentResult.recordSuccess();
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

    private List<UserType> findUsersByCorrelationRule(ResourceObjectShadowType resourceShadow,
            QueryType query, OperationResult result) throws SynchronizationException {

        if (query == null) {
            LOGGER.error("Correlation rule for resource '{}' doesn't contain query, "
                    + "returning empty list of users.", resourceShadow.getName());
            return null;
        }

        Element element = query.getFilter();
        if (element == null) {
            LOGGER.error("Correlation rule for resource '{}' doesn't contain query, "
                    + "returning empty list of users.", resourceShadow.getName());
            return null;
        }
        Element filter = updateFilterWithAccountValues(resourceShadow, element, result);
        if (filter == null) {
            LOGGER.error("Couldn't create search filter from correlation rule.");
            return null;
        }
        List<UserType> users = null;
        try {
            query = new ObjectFactory().createQueryType();
            query.setFilter(filter);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("CORRELATION: expression for OID {} results in filter {}", new Object[]{
                        resourceShadow.getOid(), DebugUtil.prettyPrint(query)});
            }
            PagingType paging = new PagingType();
            users = controller.searchObjects(UserType.class, query, paging, result);
            if (users == null) {
                return null;
            }

        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER,
                    "Couldn't search users in repository, based on filter (simplified)\n{}.", ex,
                    DebugUtil.prettyPrint(filter));
            throw new SynchronizationException(
                    "Couldn't search users in repository, based on filter (See logs).", ex);
        }

        LOGGER.debug("CORRELATION: expression for OID {} returned {} users.",
                new Object[]{resourceShadow.getOid(), users.size()});
        return users;
    }

    private List<UserType> findUserByConfirmationRule(List<UserType> users,
            ResourceObjectShadowType resourceObjectShadowType, ExpressionType expression,
            OperationResult result) throws SynchronizationException {
        List<UserType> list = new ArrayList<UserType>();
        if (users == null) {
            LOGGER.debug("Correlation list is null or empty. Returning empty confirmation list.");
            return list;
        }
        for (UserType user : users) {
            try {
                boolean confirmedUser = expressionHandler.evaluateConfirmationExpression(user,
                        resourceObjectShadowType, expression, result);
                if (user != null && confirmedUser) {
                    list.add(user);
                }
            } catch (ExpressionException ex) {
                LoggingUtils.logException(LOGGER, "Couldn't confirm user {}", ex, user.getName());
                throw new SynchronizationException("Couldn't confirm user " + user.getName(), ex);
            }
        }

        LOGGER.debug("CONFIRMATION: expression for OID {} matched {} users.", new Object[]{
                resourceObjectShadowType.getOid(), list.size()});
        return list;
    }

    private Element updateFilterWithAccountValues(ResourceObjectShadowType resourceObjectShadow,
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
                    Element refElement = findChildElement(valueExpressionElement, SchemaConstants.NS_C, "ref");
                    QName ref = DOMUtil.resolveQName(refElement);

                    Element value = document.createElementNS(SchemaConstants.NS_C, "value");
                    equal.appendChild(value);
                    Element attribute = document.createElementNS(ref.getNamespaceURI(), ref.getLocalPart());
                    ExpressionType valueExpression = XsdTypeConverter.toJavaValue(valueExpressionElement,
                            ExpressionType.class);
                    String expressionResult = expressionHandler.evaluateExpression(resourceObjectShadow,
                            valueExpression, result);

                    if (StringUtils.isEmpty(expressionResult)) {
                        LOGGER.debug("Result of search filter expression was null or empty. Expression: {}", valueExpression);
                        return null;
                    }
                    // TODO: log more context
                    LOGGER.debug("Search filter expression in the rule for OID {} evaluated to {}.",
                            new Object[]{resourceObjectShadow.getOid(), expressionResult});
                    attribute.setTextContent(expressionResult);
                    value.appendChild(attribute);
                    and.appendChild(equal);
                } else {
                    LOGGER.warn("No valueExpression in rule for OID {}", resourceObjectShadow.getOid());
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

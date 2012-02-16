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

package com.evolveum.midpoint.model.synchronizer;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.controller.Filter;
import com.evolveum.midpoint.model.controller.FilterManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.SourceType;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Processor that takes changes from synchronization and updates user attributes if necessary
 * (by creating secondary user object delta {@link ObjectDelta}).
 *
 * @author lazyman
 */
@Component
public class InboundProcessor {

    public static final String PROCESS_INBOUND_HANDLING = InboundProcessor.class.getName() + ".processInbound";
    private static final Trace LOGGER = TraceManager.getTrace(InboundProcessor.class);

    @Autowired(required = true)
    private SchemaRegistry schemaRegistry;
    @Autowired(required = true)
    private FilterManager<Filter> filterManager;
    @Autowired(required = true)
    private ValueConstructionFactory valueConstructionFactory;

    void processInbound(SyncContext context, OperationResult result) throws SchemaException {
        OperationResult subResult = result.createSubresult(PROCESS_INBOUND_HANDLING);

        ObjectDelta<UserType> userDelta = context.getUserSecondaryDelta();
        if (userDelta == null) {
            userDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY);
            if (context.getUserOld() != null) {
                userDelta.setOid(context.getUserOld().getOid());
            }
            context.setUserSecondaryDelta(userDelta);
        }

        if (ChangeType.DELETE.equals(userDelta.getChangeType())) {
            //we don't need to do inbound if we are deleting this user
            return;
        }

        try {
            for (AccountSyncContext accountContext : context.getAccountContexts()) {
                ResourceAccountType rat = accountContext.getResourceAccountType();
                LOGGER.trace("Processing inbound expressions for account {} starting", rat);

                RefinedAccountDefinition accountDefinition = context.getRefinedAccountDefinition(rat, schemaRegistry);
                if (accountDefinition == null) {
                    LOGGER.error("Definition for account type {} not found in the context, but it " +
                            "should be there, dumping context:\n{}", rat, context.dump());
                    throw new IllegalStateException("Definition for account type " + rat
                            + " not found in the context, but it should be there");
                }

                if (accountContext.getAccountDelta() != null
                        && ChangeType.DELETE.equals(accountContext.getAccountDelta().getChangeType())) {
                    //we don't need to do inbound if account was deleted
                    continue;
                }

                processInboundForAccount(context, accountContext, accountDefinition, result);
            }

            if (userDelta.isEmpty()) {
                context.setUserSecondaryDelta(null);
            }
        } finally {
            subResult.computeStatus();
        }
    }

    private void processInboundForAccount(SyncContext context, AccountSyncContext accContext,
            RefinedAccountDefinition accountDefinition, OperationResult result) throws SchemaException {

        if (accContext.getAccountSyncDelta() == null && accContext.getAccountOld() == null) {
            LOGGER.debug("Nothing to process in inbound, account sync delta and account old was null.");
            return;
        }

        ObjectDelta<UserType> userDelta = context.getUserSecondaryDelta();

        ObjectDelta<AccountShadowType> syncDelta = accContext.getAccountSyncDelta();
        PrismObject<AccountShadowType> oldAccount = accContext.getAccountOld();
        for (QName name : accountDefinition.getNamesOfAttributesWithInboundExpressions()) {
            LOGGER.trace("Processing inbound for {}", name);
            PropertyDelta propertyDelta = null;
            if (syncDelta != null) {
                propertyDelta = syncDelta.getPropertyDelta(new PropertyPath(SchemaConstants.I_ATTRIBUTES), name);
                if (propertyDelta == null) {
                    LOGGER.trace("Account sync delta exists, but doesn't have change for processed property, skipping.");
                    continue;
                }
            }

            RefinedAttributeDefinition attrDef = accountDefinition.getAttributeDefinition(name);
            List<ValueAssignmentType> inbounds = attrDef.getInboundAssignmentTypes();
            LOGGER.trace("Number of inbounds: {}", new Object[]{(inbounds != null ? inbounds.size() : 0)});

            for (ValueAssignmentType inbound : inbounds) {
                if (checkInitialSkip(inbound, context.getUserNew())) {
                    LOGGER.debug("Skipping because of initial flag.");
                    continue;
                }

                PropertyDelta delta = null;
                if (syncDelta != null) {
                    LOGGER.debug("Processing inbound from account sync delta.");
                    delta = createUserPropertyDelta(inbound, propertyDelta, context.getUserNew());
                } else if (oldAccount != null) {
                    LOGGER.debug("Processing inbound from account sync absolute state (oldAccount).");
                    PrismProperty oldAccountProperty = oldAccount.findProperty(new PropertyPath(SchemaConstants.I_ATTRIBUTES), name);
                    delta = createUserPropertyDelta(inbound, oldAccountProperty, context.getUserNew());
                }

                if (delta != null && !delta.isEmpty()) {
                    LOGGER.trace("Created delta \n{}", new Object[]{delta.debugDump(3)});
                    userDelta.addModification(delta);
                    context.recomputeUserNew();
                } else {
                    LOGGER.trace("Created delta was null or empty.");
                }
            }
        }
        processCustomPropertyInbound(accountDefinition.getCredentialsInbound(), SchemaConstants.PATH_PASSWORD_VALUE,
                context.getUserNew(), accContext, accountDefinition, userDelta, result);
        processCustomPropertyInbound(accountDefinition.getActivationInbound(), SchemaConstants.PATH_ACTIVATION_ENABLE,
                context.getUserNew(), accContext, accountDefinition, userDelta, result);
    }

    private boolean checkInitialSkip(ValueAssignmentType inbound, PrismObject<UserType> newUser) {
        ValueConstructionType valueConstruction = inbound.getSource();
        if (valueConstruction == null) {
            return false;
        }

        boolean initial = valueConstruction.isInitial() == null ? false : valueConstruction.isInitial();
        PrismProperty property = newUser.findProperty(createUserPropertyPath(inbound));
        if (initial && (property == null || property.isEmpty())) {
            return true;
        }

        return false;
    }

    private PropertyDelta createUserPropertyDelta(ValueAssignmentType inbound, PrismProperty oldAccountProperty,
            PrismObject<UserType> newUser) {
        List<ValueFilterType> filters = inbound.getValueFilter();

        PropertyPath targetUserAttribute = createUserPropertyPath(inbound);
        PrismProperty userProperty = newUser.findProperty(targetUserAttribute);

        PropertyDelta delta = null;
        if (userProperty != null) {
            LOGGER.trace("Simple property comparing user property {} to old account property {} ",
                    new Object[]{userProperty, oldAccountProperty});
            //simple property comparing if user property exists
            delta = userProperty.compareRealValuesTo(oldAccountProperty);
            delta.setName(targetUserAttribute.last());
            delta.setParentPath(targetUserAttribute.allExceptLast());
        } else {
            if (oldAccountProperty != null) {
                LOGGER.trace("Adding user property because inbound say so (account doesn't contain that value)");
                //if user property doesn't exist we have to add it (as delta), because inbound say so
                delta = new PropertyDelta(targetUserAttribute);
                delta.addValuesToAdd(oldAccountProperty.getValues());
            }
            //we don't have to create delta, because everything is alright
            LOGGER.trace("We don't have to create delta, everything is alright.");
        }

        return delta;
    }

    private PropertyDelta createUserPropertyDelta(ValueAssignmentType inbound, PropertyDelta propertyDelta,
            PrismObject<UserType> newUser) {
        List<ValueFilterType> filters = inbound.getValueFilter();

        PropertyPath targetUserAttribute = createUserPropertyPath(inbound);
        PrismProperty property = newUser.findProperty(targetUserAttribute);

        PropertyDelta delta = new PropertyDelta(targetUserAttribute);
        if (propertyDelta.getValuesToAdd() != null) {
            LOGGER.trace("Checking account sync property delta values to add");
            for (PrismPropertyValue<Object> value : propertyDelta.getValuesToAdd()) {
                PrismPropertyValue<Object> filteredValue = filterValue(value, filters);

                if (property != null && property.hasRealValue(filteredValue)) {
                    continue;
                }

                //if property is not multi value replace existing attribute
                if (property != null && !property.getDefinition().isMultiValue() && !property.isEmpty()) {
                    Collection<PrismPropertyValue<Object>> replace = new ArrayList<PrismPropertyValue<Object>>();
                    replace.add(filteredValue);
                    delta.setValuesToReplace(replace);
                } else {
                    delta.addValueToAdd(filteredValue);
                }
            }
        }
        if (propertyDelta.getValuesToDelete() != null) {
            LOGGER.trace("Checking account sync property delta values to delete");
            for (PrismPropertyValue<Object> value : propertyDelta.getValuesToDelete()) {
                PrismPropertyValue<Object> filteredValue = filterValue(value, filters);

                if (property == null || property.hasRealValue(filteredValue)) {
                    delta.addValueToDelete(filteredValue);
                }
            }
        }

        //if nothing changes was generated return null
        return delta.getValues(Object.class).isEmpty() ? null : delta;
    }

    private PropertyPath createUserPropertyPath(ValueAssignmentType inbound) {
        PropertyPath path = new PropertyPath(new XPathHolder(inbound.getTarget()));
        List<QName> segments = path.getSegments();
        if (!segments.isEmpty() && SchemaConstants.I_USER.equals(segments.get(0))) {
            segments.remove(0);
        }

        return path;
    }

    private PrismPropertyValue<Object> filterValue(PrismPropertyValue<Object> propertyValue, List<ValueFilterType> filters) {
        PrismPropertyValue<Object> filteredValue = propertyValue.clone();
        filteredValue.setType(SourceType.INBOUND);

        if (filters == null || filters.isEmpty()) {
            return filteredValue;
        }

        for (ValueFilterType filter : filters) {
            Filter filterInstance = filterManager.getFilterInstance(filter.getType(), filter.getAny());
            filterInstance.apply(filteredValue);
        }

        return filteredValue;
    }

    private void processCustomPropertyInbound(ValueAssignmentType inbound, PropertyPath path,
            PrismObject<UserType> newUser, AccountSyncContext accContext, RefinedAccountDefinition accountDefinition,
            ObjectDelta<UserType> userSecondaryDelta, OperationResult opResult) throws SchemaException {
        if (inbound == null || newUser == null) {
            return;
        }

        ValueConstructionType valueConstruction = inbound.getSource();
        boolean initial = valueConstruction.isInitial() == null ? false : valueConstruction.isInitial();

        PrismProperty property = newUser.findOrCreateProperty(path.allExceptLast(),
                path.last(), String.class);
        if (initial && !property.isEmpty()) {
            //inbound will be constructed only if initial == false or initial == true and value doesn't exist
            return;
        }

        PropertyDelta delta = userSecondaryDelta.getPropertyDelta(path);
        if (delta != null) {
            //remove delta if exists, it will be handled by inbound
            userSecondaryDelta.getModifications().remove(delta);
        }

        if (accContext.getAccountNew() == null) {
            accContext.recomputeAccountNew();
            if (accContext.getAccountNew() == null) {
                // Still null? something must be really wrong here.
                String message = "Recomputing account " + accContext.getResourceAccountType()
                        + " results in null new account. Something must be really broken.";
                LOGGER.error(message);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Account context:\n{}", accContext.dump());
                }
                throw new SystemException(message);
            }
        }

        PrismProperty input = accContext.getAccountNew().findProperty(path);
        PrismProperty result;
        try {
        	// TODO: is the parentPath correct (null)?
            ValueConstruction construction = valueConstructionFactory.createValueConstruction(
                    valueConstruction, property.getDefinition(), null, "Inbound value construction");
            construction.setInput(input);
            construction.evaluate(opResult);
            result = construction.getOutput();
        } catch (SchemaException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SchemaException(ex.getMessage(), ex);
        }

        delta = property.compareRealValuesTo(result);
        delta.setParentPath(path.allExceptLast());
        if (!delta.isEmpty()) {
            userSecondaryDelta.addModification(delta);
        }
    }
}

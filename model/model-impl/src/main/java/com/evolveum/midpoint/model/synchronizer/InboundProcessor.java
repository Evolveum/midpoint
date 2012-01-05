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
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.controller.Filter;
import com.evolveum.midpoint.model.controller.FilterManager;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.delta.PropertyDelta;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.holder.ValueAssignmentHolder;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueFilterType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

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

                processInboundForAccount(context, accountContext, accountDefinition);
            }

            if (userDelta.isEmpty()) {
                context.setUserSecondaryDelta(null);
            }
        } finally {
            subResult.computeStatus();
        }
    }

    private void processInboundForAccount(SyncContext context, AccountSyncContext accContext,
            RefinedAccountDefinition accountDefinition) {

        if (accContext.getAccountSyncDelta() == null && accContext.getAccountOld() == null) {
            LOGGER.debug("Nothing to process in inbound, account sync delta and account old was null.");
            return;
        }

        ObjectDelta<UserType> userDelta = context.getUserSecondaryDelta();

        ObjectDelta<AccountShadowType> accountDelta = accContext.getAccountSyncDelta();
        MidPointObject<AccountShadowType> oldAccount = accContext.getAccountOld();
        for (QName name : accountDefinition.getNamesOfAttributesWithInboundExpressions()) {
            PropertyDelta propertyDelta = null;
            if (accountDelta != null) {
                propertyDelta = accountDelta.getPropertyDelta(new PropertyPath(SchemaConstants.I_ATTRIBUTES), name);
                if (propertyDelta == null) {
                    continue;
                }
            }

            RefinedAttributeDefinition attrDef = accountDefinition.getAttributeDefinition(name);
            List<Element> inbounds = attrDef.getInboundAssignmentTypes();

            for (Element inbound : inbounds) {
                PropertyDelta delta = null;
                if (accountDelta != null) {
                    delta = createUserPropertyDelta(inbound, propertyDelta, context.getUserNew());
                } else if (oldAccount != null) {
                    Property oldAccountProperty = oldAccount.findProperty(new PropertyPath(SchemaConstants.I_ATTRIBUTES), name);
                    delta = createUserPropertyDelta(inbound, oldAccountProperty, context.getUserNew());
                }

                if (delta != null && !delta.isEmpty()) {
                    userDelta.addModification(delta);
                    context.recomputeUserNew();
                }
            }
        }
    }

    private PropertyDelta createUserPropertyDelta(Element inbound, Property oldAccountProperty,
            MidPointObject<UserType> newUser) {
        ValueAssignmentHolder holder = new ValueAssignmentHolder(inbound);
        List<ValueFilterType> filters = holder.getFilter();

        PropertyPath targetUserAttribute = createUserPropertyPath(holder);
        Property userProperty = newUser.findProperty(targetUserAttribute);

        PropertyDelta delta = null;
        if (oldAccountProperty != null) {
            //simple property comparing if oldAccountProperty exists
            delta = oldAccountProperty.compareRealValuesTo(userProperty);
            delta.setName(targetUserAttribute.last());
            delta.setParentPath(targetUserAttribute.allExceptLast());
        } else {
            if (userProperty != null) {
                //if user property exists we have to delete it (as delta), because inbound say so
                delta = new PropertyDelta(targetUserAttribute);
                delta.addValuesToDelete(userProperty.getValues());
            }
            //we don't have to create delta, because everything is alright
        }

        return delta;
    }

    private PropertyDelta createUserPropertyDelta(Element inbound, PropertyDelta propertyDelta,
            MidPointObject<UserType> newUser) {
        ValueAssignmentHolder holder = new ValueAssignmentHolder(inbound);
        List<ValueFilterType> filters = holder.getFilter();

        PropertyPath targetUserAttribute = createUserPropertyPath(holder);
        Property property = newUser.findProperty(targetUserAttribute);

        PropertyDelta delta = new PropertyDelta(targetUserAttribute);
        if (propertyDelta.getValuesToAdd() != null) {
            for (PropertyValue<Object> value : propertyDelta.getValuesToAdd()) {
                PropertyValue<Object> filteredValue = filterValue(value, filters);

                if (property == null || property.hasRealValue(filteredValue)) {
                    continue;
                }

                //if property is not multi value replace existing attribute
                if (property != null && !property.getDefinition().isMultiValue() && !property.isEmpty()) {
                    Collection<PropertyValue<Object>> replace = new ArrayList<PropertyValue<Object>>();
                    replace.add(filteredValue);
                    delta.setValuesToReplace(replace);
                } else {
                    delta.addValueToAdd(filteredValue);
                }
            }
        }
        if (propertyDelta.getValuesToDelete() != null) {
            for (PropertyValue<Object> value : propertyDelta.getValuesToDelete()) {
                PropertyValue<Object> filteredValue = filterValue(value, filters);

                if (property == null || property.hasRealValue(filteredValue)) {
                    delta.addValueToDelete(filteredValue);
                }
            }
        }

        //if nothing changes was generated return null
        return delta.getValues(Object.class).isEmpty() ? null : delta;
    }

    private PropertyPath createUserPropertyPath(ValueAssignmentHolder holder) {
        PropertyPath path = new PropertyPath(holder.getTarget());
        List<QName> segments = path.getSegments();
        if (!segments.isEmpty() && SchemaConstants.I_USER.equals(segments.get(0))) {
            segments.remove(0);
        }

        return path;
    }

    private PropertyValue<Object> filterValue(PropertyValue<Object> propertyValue, List<ValueFilterType> filters) {
        PropertyValue<Object> filteredValue = propertyValue.clone();
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
}

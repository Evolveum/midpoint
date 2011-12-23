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
 */

package com.evolveum.midpoint.model.synchronizer;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.controller.Filter;
import com.evolveum.midpoint.model.controller.FilterManager;
import com.evolveum.midpoint.model.util.Utils;
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
 * (by creating user object delta {@link ObjectDelta}).
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

        ObjectDelta<UserType> userDelta = context.getUserDelta();
        if (userDelta == null) {
            userDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY);
            userDelta.setOid(context.getUserOld().getOid());
            context.setUserPrimaryDelta(userDelta);
        }

        final PropertyPath attributes = new PropertyPath(SchemaConstants.I_ATTRIBUTES);
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

                ObjectDelta<AccountShadowType> accountDelta = accountContext.getAccountSyncDelta();
                for (QName name : accountDefinition.getNamesOfAttributesWithInboundExpressions()) {
                    PropertyDelta propertyDelta = accountDelta.getPropertyDelta(attributes, name);
                    if (propertyDelta == null) {
                        continue;
                    }

                    RefinedAttributeDefinition attrDef = accountDefinition.getAttributeDefinition(name);
                    List<Element> inbounds = attrDef.getInboundAssignmentTypes();

                    for (Element inbound : inbounds) {
                        PropertyDelta delta = createUserPropertyDelta(inbound, propertyDelta, context.getUserNew());
                        if (delta != null) {
                            userDelta.addModification(delta);
                            context.recomputeUserNew();
                        }
                    }
                }
            }
        } finally {
            subResult.computeStatus();
        }
    }

    private PropertyDelta createUserPropertyDelta(Element inbound, PropertyDelta propertyDelta,
            MidPointObject<UserType> newUser) {
        ValueAssignmentHolder holder = new ValueAssignmentHolder(inbound);
        List<ValueFilterType> filters = holder.getFilter();

        PropertyPath targetUserAttribute = createPropertyPath(holder);
        Property property = newUser.findProperty(targetUserAttribute);

        PropertyDelta delta = new PropertyDelta(targetUserAttribute);
        if (propertyDelta.getValuesToAdd() != null) {
            for (PropertyValue<Object> value : propertyDelta.getValuesToAdd()) {
                PropertyValue<Object> filteredValue = filterValue(value, filters);

                if (Utils.hasPropertyValue(property, filteredValue)) {
                    continue;
                }

                //if property is not multivalue replace existing attribute
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

                if (Utils.hasPropertyValue(property, filteredValue)) {
                    delta.addValueToDelete(filteredValue);
                }
            }
        }

        //if nothing changes was generated return null
        return delta.getValues(Object.class).isEmpty() ? null : delta;
    }

    private PropertyPath createPropertyPath(ValueAssignmentHolder holder) {
        PropertyPath path = new PropertyPath(holder.getTarget());
        List<QName> segments = path.getSegments();
        if (!segments.isEmpty() && SchemaConstants.I_USER.equals(segments.get(0))) {
            segments.remove(0);
        }

        return path;
    }

    private PropertyValue<Object> filterValue(PropertyValue<Object> propertyValue, List<ValueFilterType> filters) {
        PropertyValue<Object> filteredValue = propertyValue.clone();
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

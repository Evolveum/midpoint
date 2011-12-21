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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.List;

/**
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

                ObjectDelta<AccountShadowType> accountDelta = accountContext.getAccountDelta();
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

        PropertyPath targetUserAttribute = createPropertyPath(holder);
        PropertyDelta delta = new PropertyDelta(targetUserAttribute);
        if (propertyDelta.getValuesToAdd() != null) {
            for (PropertyValue<Object> value : propertyDelta.getValuesToAdd()) {
                Property property = newUser.findProperty(targetUserAttribute);
                if (!hasPropertyValue(property, value)) {
                    delta.addValueToAdd(value);
                }
            }
        }
        if (propertyDelta.getValuesToDelete() != null) {
            for (PropertyValue<Object> value : propertyDelta.getValuesToDelete()) {
                Property property = newUser.findProperty(targetUserAttribute);
                if (hasPropertyValue(property, value)) {
                    delta.addValueToDelete(value);
                }
            }
        }

        return delta.getValues(Object.class).isEmpty() ? null : delta;
    }

    private boolean hasPropertyValue(Property property, PropertyValue<Object> value) {
        if (property == null || property.getValues() == null) {
            return false;
        }

        for (PropertyValue val : property.getValues()) {
            if (val.equalsRealValue(value.getValue())) {
                return true;
            }
        }

        return false;
    }

    private PropertyPath createPropertyPath(ValueAssignmentHolder holder) {
        PropertyPath path = new PropertyPath(holder.getTarget());
        List<QName> segments = path.getSegments();
        if (!segments.isEmpty() && SchemaConstants.I_USER.equals(segments.get(0))) {
            segments.remove(0);
        }

        return path;
    }
}

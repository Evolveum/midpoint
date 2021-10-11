/*
 * Copyright (c) 2013-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.PropertyLimitations;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyAccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class AccessChecker {

    public static final String OPERATION_NAME = AccessChecker.class.getName()+".accessCheck";
    private static final Trace LOGGER = TraceManager.getTrace(AccessChecker.class);

    public void checkAdd(ProvisioningContext ctx, PrismObject<ShadowType> shadow, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createMinorSubresult(OPERATION_NAME);
        ResourceAttributeContainer attributeCont = ShadowUtil.getAttributesContainer(shadow);

        for (ResourceAttribute<?> attribute: attributeCont.getAttributes()) {
            RefinedAttributeDefinition attrDef = ctx.getObjectClassDefinition().findAttributeDefinition(attribute.getElementName());
            // Need to check model layer, not schema. Model means IDM logic which can be overridden in schemaHandling,
            // schema layer is the original one.
            if (attrDef == null) {
                String msg = "No definition for attribute "+attribute.getElementName()+" in "+ctx.getObjectClassDefinition();
                result.recordFatalError(msg);
                throw new SchemaException(msg);
            }
            PropertyLimitations limitations = attrDef.getLimitations(LayerType.MODEL);
            if (limitations == null) {
                continue;
            }
            // We cannot throw error here. At least not now. Provisioning will internally use ignored attributes
            // e.g. for simulated capabilities. This is not a problem for normal operations, but it is a problem
            // for delayed operations (e.g. consistency) that are passing through this code again.
            // TODO: we need to figure a way how to avoid this loop
//            if (limitations.isIgnore()) {
//                String message = "Attempt to create shadow with ignored attribute "+attribute.getName();
//                LOGGER.error(message);
//                throw new SchemaException(message);
//            }
            PropertyAccessType access = limitations.getAccess();
            if (access == null) {
                continue;
            }
            if (access.isAdd() == null || !access.isAdd()) {
                String message = "Attempt to add shadow with non-createable attribute "+attribute.getElementName();
                LOGGER.error(message);
                result.recordFatalError(message);
                throw new SecurityViolationException(message);
            }
        }
        result.recordSuccess();
    }

    public void checkModify(ProvisioningContext ctx, PrismObject<ShadowType> shadow,
            Collection<? extends ItemDelta> modifications,
            OperationResult parentResult) throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

        RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();

        OperationResult result = parentResult.createMinorSubresult(OPERATION_NAME);
        for (ItemDelta<?,?> modification: modifications) {
            if (!(modification instanceof PropertyDelta<?>)) {
                continue;
            }
            PropertyDelta<?> attrDelta = (PropertyDelta<?>)modification;
            if (!SchemaConstants.PATH_ATTRIBUTES.equivalent(attrDelta.getParentPath())) {
                // Not an attribute
                continue;
            }
            QName attrName = attrDelta.getElementName();
            LOGGER.trace("Checking attribute {} definition present in {}", attrName, objectClassDefinition);
            RefinedAttributeDefinition attrDef = objectClassDefinition.findAttributeDefinition(attrName);
            if (attrDef == null) {
                throw new SchemaException("Cannot find definition of attribute "+attrName+" in "+objectClassDefinition);
            }
            PropertyLimitations limitations = attrDef.getLimitations(LayerType.MODEL);
            if (limitations == null) {
                continue;
            }
            // We cannot throw error here. At least not now. Provisioning will internally use ignored attributes
            // e.g. for simulated capabilities. This is not a problem for normal operations, but it is a problem
            // for delayed operations (e.g. consistency) that are passing through this code again.
            // TODO: we need to figure a way how to avoid this loop
//            if (limitations.isIgnore()) {
//                String message = "Attempt to create shadow with ignored attribute "+attribute.getName();
//                LOGGER.error(message);
//                throw new SchemaException(message);
//            }
            PropertyAccessType access = limitations.getAccess();
            if (access == null) {
                continue;
            }
            if (access.isModify() == null || !access.isModify()) {
                String message = "Attempt to modify non-updateable attribute "+attrName;
                LOGGER.error(message);
                result.recordFatalError(message);
                throw new SecurityViolationException(message);
            }
        }
        result.recordSuccess();

    }

    public void filterGetAttributes(ResourceAttributeContainer attributeContainer, RefinedObjectClassDefinition objectClassDefinition, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(OPERATION_NAME);


        for (ResourceAttribute<?> attribute: attributeContainer.getAttributes()) {
            QName attrName = attribute.getElementName();
            RefinedAttributeDefinition attrDef = objectClassDefinition.findAttributeDefinition(attrName);
            if (attrDef == null) {
                String message = "Unknown attribute " + attrName + " in objectclass " + objectClassDefinition;
                result.recordFatalError(message);
                throw new SchemaException(message);
            }
            // Need to check model layer, not schema. Model means IDM logic which can be overridden in schemaHandling,
            // schema layer is the original one.
            PropertyLimitations limitations = attrDef.getLimitations(LayerType.MODEL);
            if (limitations == null) {
                continue;
            }
            // We cannot throw error here. At least not now. Provisioning will internally use ignored attributes
            // e.g. for simulated capabilities. This is not a problem for normal operations, but it is a problem
            // for delayed operations (e.g. consistency) that are passing through this code again.
            // TODO: we need to figure a way how to avoid this loop
//            if (limitations.isIgnore()) {
//                String message = "Attempt to create shadow with ignored attribute "+attribute.getName();
//                LOGGER.error(message);
//                throw new SchemaException(message);
//            }
            PropertyAccessType access = limitations.getAccess();
            if (access == null) {
                continue;
            }
            if (access.isRead() == null || !access.isRead()) {
                LOGGER.trace("Removing non-readable attribute {}", attrName);
                attributeContainer.remove(attribute);
            }
        }
        result.recordSuccess();
    }



}

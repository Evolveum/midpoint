/*
 * Copyright (c) 2013-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;

import com.evolveum.midpoint.schema.processor.*;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author Radovan Semancik
 *
 */
@Component
class AccessChecker {

    private static final String OP_ACCESS_CHECK = AccessChecker.class.getName() + ".accessCheck";

    private static final Trace LOGGER = TraceManager.getTrace(AccessChecker.class);

    void checkAddAccess(ProvisioningContext ctx, ShadowType shadow, OperationResult parentResult)
            throws SecurityViolationException, SchemaException {
        OperationResult result = parentResult.createMinorSubresult(OP_ACCESS_CHECK);
        try {
            ResourceAttributeContainer attributeCont = ShadowUtil.getAttributesContainer(shadow);

            for (ResourceAttribute<?> attribute : attributeCont.getAttributes()) {
                PropertyLimitations limitations =
                        ctx.findAttributeDefinitionRequired(attribute.getElementName())
                                .getLimitations(LayerType.MODEL);
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
                if (!limitations.canAdd()) {
                    throw new SecurityViolationException(
                            "Attempt to add shadow with non-creatable attribute " + attribute.getElementName());
                }
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    void checkModifyAccess(
            ProvisioningContext ctx,
            Collection<? extends ItemDelta<?, ?>> modifications,
            OperationResult parentResult)
            throws SecurityViolationException, SchemaException {

        ResourceObjectDefinition resourceObjectDefinition = ctx.getObjectDefinitionRequired();

        OperationResult result = parentResult.createMinorSubresult(OP_ACCESS_CHECK);
        try {
            for (ItemDelta<?, ?> modification : modifications) {
                if (!(modification instanceof PropertyDelta<?>)) {
                    continue;
                }
                PropertyDelta<?> attrDelta = (PropertyDelta<?>) modification;
                if (!SchemaConstants.PATH_ATTRIBUTES.equivalent(attrDelta.getParentPath())) {
                    // Not an attribute
                    continue;
                }
                QName attrName = attrDelta.getElementName();
                LOGGER.trace("Checking attribute {} definition present in {}", attrName, resourceObjectDefinition);
                ResourceAttributeDefinition<?> attrDef = resourceObjectDefinition.findAttributeDefinitionRequired(attrName);
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
                if (!limitations.canModify()) {
                    String message = "Attempt to modify non-updateable attribute " + attrName;
                    LOGGER.error(message);
                    result.recordFatalError(message);
                    throw new SecurityViolationException(message);
                }
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    void filterGetAttributes(
            ResourceAttributeContainer attributeContainer,
            ResourceObjectDefinition objectDefinition,
            OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(OP_ACCESS_CHECK);
        List<ResourceAttribute<?>> attributesToRemove = new ArrayList<>();
        try {
            for (ResourceAttribute<?> attribute : attributeContainer.getAttributes()) {
                QName attrName = attribute.getElementName();
                ResourceAttributeDefinition<?> attrDef = objectDefinition.findAttributeDefinition(attrName);
                if (attrDef == null) { // TODO
                    String message = "Unknown attribute " + attrName + " in objectclass " + objectDefinition;
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
                if (!limitations.canRead()) {
                    attributesToRemove.add(attribute);
                }
            }
            for (ResourceAttribute<?> attributeToRemove : attributesToRemove) {
                LOGGER.trace("Removing non-readable attribute {}", attributeToRemove);
                attributeContainer.remove(attributeToRemove);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }
}

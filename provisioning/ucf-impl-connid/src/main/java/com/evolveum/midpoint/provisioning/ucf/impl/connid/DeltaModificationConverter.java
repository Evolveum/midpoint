/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.util.*;

import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeDeltaBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class DeltaModificationConverter extends AbstractModificationConverter {

    private final Set<AttributeDelta> attributesDeltas = new HashSet<>();

    DeltaModificationConverter(
            @NotNull Collection<Operation> changes,
            @NotNull ResourceSchema resourceSchema,
            @NotNull ResourceObjectDefinition objectDefinition,
            String connectorDescription,
            ConnectorOperationOptions options,
            @NotNull ConnIdObjectConvertor objectConvertor) {
        super(changes, resourceSchema, objectDefinition, connectorDescription, options, objectConvertor);
    }

    Set<AttributeDelta> getAttributesDeltas() {
        return attributesDeltas;
    }

    @Override
    protected <V extends PrismValue> void collect(
            String connIdAttrName,
            ItemDelta<V, ?> delta,
            PlusMinusZero isInModifiedAuxiliaryClass,
            CollectorValuesConverter<V> valuesConverter) throws SchemaException {
        var deltaBuilder = new AttributeDeltaBuilder();
        deltaBuilder.setName(connIdAttrName);
        if (delta.isAdd()) {
            var connIdAttributeValues = valuesConverter.covertAttributeValuesToConnId(delta.getValuesToAdd(), delta.getElementName());
            if (delta.getDefinition().isMultiValue()) {
                deltaBuilder.addValueToAdd(connIdAttributeValues);
            } else {
                // Force "update" for single-valued attributes instead of "add". This is saving one
                // read in some cases. It should also make no substantial difference in such case.
                // But it is working around some connector bugs.
                deltaBuilder.addValueToReplace(connIdAttributeValues);
            }
        }
        if (delta.isDelete()) {
            if (delta.getDefinition().isMultiValue() || isInModifiedAuxiliaryClass == PlusMinusZero.MINUS) {
                deltaBuilder.addValueToRemove(
                        valuesConverter.covertAttributeValuesToConnId(delta.getValuesToDelete(), delta.getElementName()));
            } else {
                // Force "update" for single-valued attributes instead of "add". This is saving one
                // read in some cases.
                // Update attribute to no values. This will efficiently clean up the attribute.
                // It should also make no substantial difference in such case.
                // But it is working around some connector bugs.
                // update with EMPTY value. The connIdAttributeValues is NOT used in this branch
                // Explicitly replace with empty list. Passing null here would mean "no replace in this delta".
                deltaBuilder.addValueToReplace(Collections.EMPTY_LIST);
            }
        }
        if (delta.isReplace()) {
            var connIdAttributeValues =
                    valuesConverter.covertAttributeValuesToConnId(delta.getValuesToReplace(), delta.getElementName());
            if (isInModifiedAuxiliaryClass == PlusMinusZero.PLUS) {
                deltaBuilder.addValueToAdd(connIdAttributeValues);
            } else {
                deltaBuilder.addValueToReplace(connIdAttributeValues);
            }
        }
        attributesDeltas.add(deltaBuilder.build());
    }

    @Override
    protected <T> void collectReplace(String connIdAttrName, T connIdAttrValue) {
        if (connIdAttrValue == null) {
            // Explicitly replace with empty list. Passing null here would mean "no replace in this delta".
            attributesDeltas.add(AttributeDeltaBuilder.build(connIdAttrName, Collections.EMPTY_LIST));
        } else {
            attributesDeltas.add(AttributeDeltaBuilder.build(connIdAttrName, connIdAttrValue));
        }
    }

    @Override
    protected void collectPassword(PropertyDelta<ProtectedStringType> passwordDelta) throws SchemaException {
        if (isSelfPasswordChange(passwordDelta)) {
            AttributeDeltaBuilder deltaBuilder = new AttributeDeltaBuilder();
            deltaBuilder.setName(OperationalAttributes.PASSWORD_NAME);

            PrismProperty<ProtectedStringType> newPasswordProperty = passwordDelta.getPropertyNewMatchingPath();
            GuardedString newPasswordGs = passwordToGuardedString(newPasswordProperty.getRealValue(), "new password");
            deltaBuilder.addValueToAdd(newPasswordGs);

            ProtectedStringType oldPasswordPs = passwordDelta.getEstimatedOldValues().iterator().next().getRealValue();
            GuardedString oldPasswordGs = passwordToGuardedString(oldPasswordPs, "old password");
            deltaBuilder.addValueToRemove(oldPasswordGs);

            attributesDeltas.add(deltaBuilder.build());
        } else {
            super.collectPassword(passwordDelta);
        }
    }

    private boolean isSelfPasswordChange(PropertyDelta<ProtectedStringType> delta) {
        // We need runAs option, otherwise this is no self-service but an administrator setting the password.
        if (options == null || options.getRunAsIdentification() == null) {
            return false;
        }

        Collection<PrismPropertyValue<ProtectedStringType>> estimatedOldValues = delta.getEstimatedOldValues();
        return estimatedOldValues != null && !estimatedOldValues.isEmpty();
    }

    @Override
    protected void debugDumpOutput(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "attributesDelta", attributesDeltas, indent + 1);
    }
}

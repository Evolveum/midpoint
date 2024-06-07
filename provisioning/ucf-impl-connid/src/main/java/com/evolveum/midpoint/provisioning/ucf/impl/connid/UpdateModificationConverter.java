/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 */
public class UpdateModificationConverter extends AbstractModificationConverter {

    UpdateModificationConverter(
            @NotNull Collection<Operation> changes,
            @NotNull ResourceSchema resourceSchema,
            @NotNull ResourceObjectDefinition objectDefinition,
            String connectorDescription,
            ConnectorOperationOptions options,
            @NotNull ConnIdObjectConvertor objectConvertor) {
        super(changes, resourceSchema, objectDefinition, connectorDescription, options, objectConvertor);
    }

    private final Set<Attribute> attributesToAdd = new HashSet<>();
    private final Set<Attribute> attributesToUpdate = new HashSet<>();
    private final Set<Attribute> attributesToRemove = new HashSet<>();

    Set<Attribute> getAttributesToAdd() {
        return attributesToAdd;
    }

    Set<Attribute> getAttributesToUpdate() {
        return attributesToUpdate;
    }

    Set<Attribute> getAttributesToRemove() {
        return attributesToRemove;
    }

    @Override
    protected <V extends PrismValue> void collect(String connIdAttrName, ItemDelta<V, ?> delta,
            PlusMinusZero isInModifiedAuxiliaryClass, CollectorValuesConverter<V> valuesConverter)
            throws SchemaException {

        if (delta.isAdd()) {
            List<Object> connIdAttributeValues = valuesConverter.covertAttributeValuesToConnId(delta.getValuesToAdd(), delta.getElementName());
            if (delta.getDefinition().isMultiValue()) {
                attributesToAdd.add(AttributeBuilder.build(connIdAttrName, connIdAttributeValues));
            } else {
                // Force "update" for single-valued attributes instead of "add". This is saving one
                // read in some cases. It should also make no substantial difference in such case.
                // But it is working around some connector bugs.
                attributesToUpdate.add(AttributeBuilder.build(connIdAttrName, connIdAttributeValues));
            }
        }
        if (delta.isDelete()) {
            if (delta.getDefinition().isMultiValue() || isInModifiedAuxiliaryClass == PlusMinusZero.MINUS) {
                List<Object> connIdAttributeValues = valuesConverter.covertAttributeValuesToConnId(delta.getValuesToDelete(), delta.getElementName());
                attributesToRemove.add(AttributeBuilder.build(connIdAttrName, connIdAttributeValues));
            } else {
                // Force "update" for single-valued attributes instead of "add". This is saving one
                // read in some cases.
                // Update attribute to no values. This will efficiently clean up the attribute.
                // It should also make no substantial difference in such case.
                // But it is working around some connector bugs.
                // update with EMPTY value. The connIdAttributeValues is NOT used in this branch
                attributesToUpdate.add(AttributeBuilder.build(connIdAttrName));
            }
        }
        if (delta.isReplace()) {
            List<Object> connIdAttributeValues = valuesConverter.covertAttributeValuesToConnId(delta.getValuesToReplace(), delta.getElementName());
            if (isInModifiedAuxiliaryClass == PlusMinusZero.PLUS) {
                attributesToAdd.add(AttributeBuilder.build(connIdAttrName, connIdAttributeValues));
            } else {
                attributesToUpdate.add(AttributeBuilder.build(connIdAttrName, connIdAttributeValues));
            }
        }
    }

    @Override
    protected <T> void collectReplace(String connIdAttrName, T connIdAttrValue) {
        if (connIdAttrValue == null) {
            attributesToUpdate.add(AttributeBuilder.build(connIdAttrName));
        } else {
            attributesToUpdate.add(AttributeBuilder.build(connIdAttrName, connIdAttrValue));
        }
    }

    @Override
    protected void debugDumpOutput(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "attributesToAdd", attributesToAdd, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "attributesToUpdate", attributesToUpdate, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "attributesToRemove", attributesToRemove, indent + 1);
    }
}

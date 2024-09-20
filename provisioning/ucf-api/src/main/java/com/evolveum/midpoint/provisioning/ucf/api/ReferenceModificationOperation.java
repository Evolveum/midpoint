/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyModificationOperationType;

/**
 * Represents modification of a shadow reference attribute.
 */
public class ReferenceModificationOperation extends Operation {

    @NotNull private final ReferenceDelta referenceDelta;

    public ReferenceModificationOperation(@NotNull ReferenceDelta referenceDelta) {
        this.referenceDelta = referenceDelta;
    }

    @NotNull public ReferenceDelta getReferenceDelta() {
        return referenceDelta;
    }

    @Override
    public @NotNull ItemDelta<?, ?> getItemDelta() {
        return getReferenceDelta();
    }

    @Override
    public boolean isRename(@NotNull ResourceObjectDefinition objDef) {
        return false;
    }

    @Override
    public boolean isAttributeDelta() {
        return true;
    }

    @Override
    public @Nullable ShadowSimpleAttributeDefinition<?> getAttributeDefinitionIfApplicable(@NotNull ResourceObjectDefinition objDef) {
        return null;
    }

    public void swallowValue(@NotNull ShadowReferenceAttributeValue value, boolean toPlusSet) {
        if (toPlusSet) {
            referenceDelta.addValueToAdd(value);
        } else {
            referenceDelta.addValueToDelete(value);
        }
    }

    // Let's ignore the type safety for now

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReferenceModificationOperation that = (ReferenceModificationOperation) o;
        return Objects.equals(referenceDelta, that.referenceDelta);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(referenceDelta);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "ReferenceModificationOperation", indent);
        DebugUtil.debugDumpWithLabel(sb, "delta", referenceDelta, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return referenceDelta.toString();
    }

    @Override
    public PropertyModificationOperationType asBean(PrismContext prismContext) throws SchemaException {
        PropertyModificationOperationType bean = new PropertyModificationOperationType();
        bean.getDelta().addAll(DeltaConvertor.toItemDeltaTypes(referenceDelta));
        return bean;
    }

    public @NotNull ItemName getItemName() {
        return referenceDelta.getElementName();
    }
}

/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;

import com.evolveum.midpoint.prism.delta.PropertyDelta;

import com.evolveum.midpoint.prism.delta.ReferenceDelta;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ObjectTreeDelta<O extends ObjectType> extends ContainerTreeDelta<O> {

    private String oid;

    private PrismObject<O> objectToAdd;

    public ObjectTreeDelta( PrismObjectDefinition<O> definition) {
        super(definition);
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public PrismObject<O> getObjectToAdd() {
        return objectToAdd;
    }

    public void setObjectToAdd(PrismObject<O> objectToAdd) {
        this.objectToAdd = objectToAdd;
    }

    @Override
    public void setValues(@NotNull List<ContainerTreeDeltaValue<O>> values) {
        if (values.size() != 1) {
            throw new IllegalArgumentException("Object tree delta must have exactly one value");
        }

        super.setValues(values);
    }

    @Override
    protected String debugDumpShortName() {
        return "OTD";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.debugDumpWithLabel(sb, debugDumpShortName(), DebugUtil.formatElementName(getItemName()), indent);
        DebugUtil.debugDumpWithLabel(sb, "oid", oid, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "type", DebugUtil.formatElementName(getTypeName()), indent + 1);

        ContainerTreeDeltaValue<O> value = getSingleValue();
        if (value != null) {
            sb.append(DebugUtil.debugDump(value, indent + 1));
        }

        return sb.toString();
    }

    public static <O extends ObjectType> ObjectTreeDelta<O> from(ObjectDelta<O> delta) {
        PrismObjectDefinition<O> def = PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(delta.getObjectTypeClass());
        // todo fix definition
        ObjectTreeDelta<O> result = new ObjectTreeDelta<>(def);
        result.setOid(delta.getOid());
        result.setObjectToAdd(delta.getObjectToAdd());

        // todo fix value to add, modification type somehow
        ObjectTreeDeltaValue<O> value = new ObjectTreeDeltaValue<>(null, null);
        result.getValues().add(value);

        delta.getModifications().forEach(modification -> {
            if (modification instanceof ContainerDelta<?> containerDelta) {
                value.getDeltas().add(ContainerTreeDelta.from(containerDelta));
            } else if (modification instanceof PropertyDelta propertyDelta) {
                value.getDeltas().add(PropertyTreeDelta.from(propertyDelta));
            } else if (modification instanceof ReferenceDelta referenceDelta) {
                value.getDeltas().add(ReferenceTreeDelta.from(referenceDelta));
            }
        });

        return result;
    }
}

/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.delta;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ObjectTreeDelta<O extends ObjectType> extends ContainerTreeDelta<O> {

    private String oid;

    private PrismObject<O> objectToAdd;

    public ObjectTreeDelta(PrismObjectDefinition<O> definition) {
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
    public ContainerTreeDeltaValue<O> createNewValue() {
        return new ObjectTreeDeltaValue<>();
    }

    @Override
    public ItemPath getPath() {
        return ItemPath.EMPTY_PATH;
    }

    public static <O extends ObjectType> ObjectTreeDelta<O> fromItemDelta(ObjectDelta<O> delta) {
        SchemaRegistry registry = PrismContext.get().getSchemaRegistry();
        PrismObjectDefinition<O> def = registry.findObjectDefinitionByCompileTimeClass(delta.getObjectTypeClass());

        ObjectTreeDelta<O> treeDelta = new ObjectTreeDelta<>(def);
        treeDelta.setOid(delta.getOid());
        treeDelta.setObjectToAdd(delta.getObjectToAdd());  // todo this feels funky, probably should end up in value?

        // todo fix value to add, modification type somehow
        ObjectTreeDeltaValue<O> value = new ObjectTreeDeltaValue<>();
        value.setOid(delta.getOid());
        treeDelta.addValue(value);

        delta.getModifications().forEach(modification -> {
            if (modification instanceof ContainerDelta containerDelta) {
                ContainerTreeDelta<?> ctd = treeDelta.findOrCreateItemDelta(containerDelta.getPath(), ContainerTreeDelta.class);

                addItemDeltaValues(containerDelta, ctd);
            } else if (modification instanceof PropertyDelta propertyDelta) {
                PropertyTreeDelta<?> ptd = treeDelta.findOrCreateItemDelta(propertyDelta.getPath(), PropertyTreeDelta.class);

                addItemDeltaValues(propertyDelta, ptd);
            } else if (modification instanceof ReferenceDelta referenceDelta) {
                ReferenceTreeDelta rtd = treeDelta.findOrCreateItemDelta(referenceDelta.getPath(), ReferenceTreeDelta.class);

                addItemDeltaValues(referenceDelta, rtd);
            }
        });

        return treeDelta;
    }

    private static <PV extends PrismValue, V extends ItemTreeDeltaValue<PV, ?>, ID extends ItemDelta<PV, ?>, ITD extends ItemTreeDelta<PV, ?, ?, V>> void addItemDeltaValues(
            ID delta, ITD treeDelta) {

        if (delta == null) {
            return;
        }

        addDeltaValues(treeDelta, delta.getValuesToAdd(), ModificationType.ADD);
        addDeltaValues(treeDelta, delta.getValuesToReplace(), ModificationType.REPLACE);
        addDeltaValues(treeDelta, delta.getValuesToDelete(), ModificationType.DELETE);
    }

    private static <PV extends PrismValue, V extends ItemTreeDeltaValue<PV, ?>, ID extends ItemDelta<PV, ?>, ITD extends ItemTreeDelta<PV, ?, ?, V>> void addDeltaValues(
            ITD treeDelta, Collection<PV> values, ModificationType modificationType) {
        if (values == null) {
            return;
        }

        for (PV value : values) {
            V treeDeltaValue = treeDelta.createNewValue();
            treeDeltaValue.setValue(value);
            treeDeltaValue.setModificationType(modificationType);

            treeDelta.addValue(treeDeltaValue);
        }
    }

    public ObjectDelta<O> toObjectDelta() throws SchemaException {
        ObjectDelta<O> delta = PrismContext.get().deltaFor(getDefinition().getCompileTimeClass())
                .asObjectDelta(getOid());
        PrismObject<O> object = getObjectToAdd();
        if (object != null) {
            delta.setChangeType(ChangeType.ADD);
            delta.setObjectToAdd(getObjectToAdd());
        }

        ContainerTreeDeltaValue<O> value = getSingleValue();
        if (value != null) {
            delta.getModifications().addAll((Collection) value.getModifications(true));
        }

        return delta;
    }
}

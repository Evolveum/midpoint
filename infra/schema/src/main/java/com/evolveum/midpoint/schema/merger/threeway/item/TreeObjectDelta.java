/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;

import com.evolveum.midpoint.prism.delta.PropertyDelta;

import com.evolveum.midpoint.prism.delta.ReferenceDelta;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class TreeObjectDelta<O extends ObjectType> extends TreeContainerDelta<O> {

    private String oid;

    private PrismObject<O> objectToAdd;

    public TreeObjectDelta( PrismObjectDefinition<O> definition) {
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

    public static <O extends ObjectType> TreeObjectDelta<O> from(ObjectDelta<O> delta) {
        // todo fix definition
        TreeObjectDelta<O> result = new TreeObjectDelta<>(null);
        result.setOid(delta.getOid());
        result.setObjectToAdd(delta.getObjectToAdd());

        // todo fix value to add, modification type somehow
        TreeObjectDeltaValue<O> value = new TreeObjectDeltaValue<>(null, null);
        result.getValues().add(value);

        delta.getModifications().forEach(modification -> {
            if (modification instanceof ContainerDelta containerDelta) {
//                value.
            } else if (modification instanceof PropertyDelta propertyDelta) {

            } else if (modification instanceof ReferenceDelta referenceDelta) {
                // todo
            }
        });

        return result;
    }
}

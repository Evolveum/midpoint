/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katka
 *
 */
public class PrismObjectWrapperImpl<O extends ObjectType> extends PrismContainerWrapperImpl<O> implements PrismObjectWrapper<O> {

    private static final long serialVersionUID = 1L;

    public PrismObjectWrapperImpl(PrismObject<O> item, ItemStatus status) {
        super(null, item, status);
    }

    @Override
    public ObjectDelta<O> getObjectDelta() throws SchemaException {
        ObjectDelta<O> objectDelta = getPrismContext().deltaFor(getObject().getCompileTimeClass())
                .asObjectDelta(getObject().getOid());

        Collection<ItemDelta> deltas = new ArrayList<>();
        for (ItemWrapper<?, ?> itemWrapper : getValue().getItems()) {
            Collection<ItemDelta> delta = itemWrapper.getDelta();
            if (delta == null || delta.isEmpty()) {
                continue;
            }
            // objectDelta.addModification(delta);
            deltas.addAll(delta);
        }

        switch (getStatus()) {
            case ADDED:
                objectDelta.setChangeType(ChangeType.ADD);
                PrismObject<O> clone = (PrismObject<O>) getOldItem().clone();
                // cleanupEmptyContainers(clone);
                for (ItemDelta d : deltas) {
                    d.applyTo(clone);
                }
                objectDelta.setObjectToAdd(clone);
                break;
            case NOT_CHANGED:
                objectDelta.mergeModifications(deltas);
                break;
            case DELETED:
                objectDelta.setChangeType(ChangeType.DELETE);
                break;
        }

        return objectDelta;
    }

    @Override
    @Deprecated
    public String getOid() {
        return ((PrismObject<O>) getItem()).getOid();
    }

    @Override
    public PrismObject<O> getObject() {
        return (PrismObject<O>) getItem();
    }

    @Override
    public PrismObject<O> getObjectOld() {
        return (PrismObject<O>) getOldItem();
    }

    @Override
    public PrismObjectValueWrapper<O> getValue() {
        return (PrismObjectValueWrapper<O>) getValues().iterator().next();
    }

    @Override
    public String getDisplayName() {
        return "properties";
    }

    @Override
    public PrismObject<O> getObjectApplyDelta() throws SchemaException {
        PrismObject<O> oldObject = getObjectOld().clone();

        Collection<ItemDelta> deltas = new ArrayList<>();
        for (ItemWrapper<?, ?> itemWrapper : getValue().getItems()) {
            Collection<ItemDelta> delta = itemWrapper.getDelta();
            if (delta == null || delta.isEmpty()) {
                continue;
            }
            // objectDelta.addModification(delta);
            deltas.addAll(delta);
        }

        for (ItemDelta delta : deltas) {
            delta.applyTo(oldObject);
        }

        return oldObject;
    }

    @Override
    public boolean isImmutable() {
        // TODO
        return false;
    }

    @Override
    public void freeze() {
        // TODO
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        // TODO
        return false;
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        // TODO
    }

//    @Override
//    public boolean isVisible(PrismContainerValueWrapper parent, ItemVisibilityHandler visibilityHandler) {
//        return true;
//    }

    @Override
    public boolean isExpanded() {
        return super.isExpanded();
    }
}

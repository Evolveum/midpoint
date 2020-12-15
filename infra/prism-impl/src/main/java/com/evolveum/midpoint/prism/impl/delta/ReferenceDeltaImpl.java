/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.delta;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.impl.PrismReferenceValueImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 */
public class ReferenceDeltaImpl extends ItemDeltaImpl<PrismReferenceValue, PrismReferenceDefinition> implements ReferenceDelta {

    public ReferenceDeltaImpl(PrismReferenceDefinition itemDefinition, PrismContext prismContext) {
        super(itemDefinition, prismContext);
    }

    public ReferenceDeltaImpl(ItemPath propertyPath, PrismReferenceDefinition itemDefinition, PrismContext prismContext) {
        super(propertyPath, itemDefinition, prismContext);
    }

    public ReferenceDeltaImpl(ItemPath parentPath, QName name, PrismReferenceDefinition itemDefinition, PrismContext prismContext) {
        super(parentPath, name, itemDefinition, prismContext);
    }

    @Override
    public Class<PrismReference> getItemClass() {
        return PrismReference.class;
    }

    @Override
    public void setDefinition(PrismReferenceDefinition definition) {
        if (!(definition instanceof PrismReferenceDefinition)) {
            throw new IllegalArgumentException("Cannot apply " + definition + " to reference delta");
        }
        super.setDefinition(definition);
    }

    @Override
    public void applyDefinition(PrismReferenceDefinition definition) throws SchemaException {
        if (!(definition instanceof PrismReferenceDefinition)) {
            throw new IllegalArgumentException("Cannot apply definition " + definition + " to reference delta " + this);
        }
        super.applyDefinition(definition);
    }

    @Override
    public boolean isApplicableToType(Item item) {
        return item instanceof PrismReference;
    }

    @Override
    public ReferenceDeltaImpl clone() {
        ReferenceDeltaImpl clone = new ReferenceDeltaImpl(getPath(), getDefinition(), getPrismContext());
        copyValues(clone);
        return clone;
    }

    protected void copyValues(ReferenceDeltaImpl clone) {
        super.copyValues(clone);
    }

    /**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method.
     */

    public static ReferenceDeltaImpl createModificationReplace(ItemPath path, PrismObjectDefinition<?> objectDefinition, String oid) {
        return createModificationReplace(path, objectDefinition, new PrismReferenceValueImpl(oid));
    }

    public static <O extends Objectable> ReferenceDeltaImpl createModificationReplace(ItemPath path, Class<O> type, PrismContext ctx, String oid) {
        PrismObjectDefinition<O> objectDefinition = ctx.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
        return createModificationReplace(path, objectDefinition, oid == null ? null : new PrismReferenceValueImpl(oid));
    }

    public static ReferenceDeltaImpl createModificationReplace(ItemPath path, PrismObjectDefinition<?> objectDefinition,
            PrismReferenceValue refValue) {
        PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(path, PrismReferenceDefinition.class);
        ReferenceDeltaImpl referenceDelta = new ReferenceDeltaImpl(path, referenceDefinition, objectDefinition.getPrismContext());             // hoping the prismContext is there
        if (refValue == null) {
            referenceDelta.setValueToReplace();
        } else {
            referenceDelta.setValueToReplace(refValue);
        }
        return referenceDelta;
    }

    public static ReferenceDeltaImpl createModificationReplace(ItemPath path, PrismObjectDefinition<?> objectDefinition,
            Collection<PrismReferenceValue> refValues) {
        PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(path, PrismReferenceDefinition.class);
        ReferenceDeltaImpl referenceDelta = new ReferenceDeltaImpl(path, referenceDefinition, objectDefinition.getPrismContext());             // hoping the prismContext is there
        referenceDelta.setValuesToReplace(refValues);
        return referenceDelta;
    }

    public static Collection<? extends ItemDelta<?, ?>> createModificationAddCollection(ItemName propertyName,
            PrismObjectDefinition<?> objectDefinition, PrismReferenceValue refValue) {
        return List.of(createModificationAdd(propertyName, objectDefinition, refValue));
    }

    public static ReferenceDeltaImpl createModificationAdd(ItemPath path, PrismObjectDefinition<?> objectDefinition,
            String oid) {
        return createModificationAdd(path, objectDefinition, new PrismReferenceValueImpl(oid));
    }

    public static ReferenceDeltaImpl createModificationAdd(ItemPath path, PrismObjectDefinition<?> objectDefinition,
            PrismReferenceValue refValue) {
        PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(path, PrismReferenceDefinition.class);
        ReferenceDeltaImpl referenceDelta = new ReferenceDeltaImpl(path, referenceDefinition, objectDefinition.getPrismContext());             // hoping the prismContext is there
        referenceDelta.addValueToAdd(refValue);
        return referenceDelta;
    }

    public static ReferenceDeltaImpl createModificationAdd(ItemPath path, PrismObjectDefinition<?> objectDefinition,
            Collection<PrismReferenceValue> refValues) {
        PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(path, PrismReferenceDefinition.class);
        ReferenceDeltaImpl referenceDelta = new ReferenceDeltaImpl(path, referenceDefinition, objectDefinition.getPrismContext());             // hoping the prismContext is there
        referenceDelta.addValuesToAdd(refValues);
        return referenceDelta;
    }

    public static <T extends Objectable> ReferenceDeltaImpl createModificationAdd(Class<T> type, ItemName refName, PrismContext prismContext,
            PrismReferenceValue refValue) {
        PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
        return createModificationAdd(refName, objectDefinition, refValue);
    }

    public static <T extends Objectable> Collection<? extends ItemDelta<?, ?>> createModificationAddCollection(
            Class<T> type, ItemName refName, PrismContext prismContext, String targetOid) {
        PrismReferenceValue refValue = new PrismReferenceValueImpl(targetOid);
        return createModificationAddCollection(type, refName, prismContext, refValue);
    }

    public static <T extends Objectable> Collection<? extends ItemDelta<?, ?>> createModificationAddCollection(
            Class<T> type, ItemName refName, PrismContext prismContext, PrismReferenceValue refValue) {
        return List.of(createModificationAdd(type, refName, prismContext, refValue));
    }

    public static <T extends Objectable> ReferenceDeltaImpl createModificationAdd(
            Class<T> type, ItemName refName, PrismContext prismContext, PrismObject<?> refTarget) {
        PrismReferenceValue refValue = prismContext.itemFactory().createReferenceValue(refTarget);
        return createModificationAdd(type, refName, prismContext, refValue);
    }

    public static <T extends Objectable> Collection<? extends ItemDelta<?, ?>> createModificationAddCollection(
            Class<T> type, ItemName refName, PrismContext prismContext, PrismObject<?> refTarget) {
        return List.of(createModificationAdd(type, refName, prismContext, refTarget));
    }

    public static Collection<? extends ItemDelta<?, ?>> createModificationDeleteCollection(QName propertyName,
            PrismObjectDefinition<?> objectDefinition, PrismReferenceValue refValue) {
        return List.of(createModificationDelete(propertyName, objectDefinition, refValue));
    }

    public static ReferenceDeltaImpl createModificationDelete(ItemPath path, PrismObjectDefinition<?> objectDefinition,
            Collection<PrismReferenceValue> refValues) {
        PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(path, PrismReferenceDefinition.class);
        ReferenceDeltaImpl referenceDelta = new ReferenceDeltaImpl(path, referenceDefinition, objectDefinition.getPrismContext());             // hoping the prismContext is there
        referenceDelta.addValuesToDelete(refValues);
        return referenceDelta;
    }

    public static ReferenceDeltaImpl createModificationDelete(QName refName, PrismObjectDefinition<?> objectDefinition,
            String oid) {
        return createModificationDelete(refName, objectDefinition, new PrismReferenceValueImpl(oid));
    }

    public static ReferenceDeltaImpl createModificationDelete(QName refName, PrismObjectDefinition<?> objectDefinition,
            PrismObject<?> refTarget, PrismContext prismContext) {
        PrismReferenceValue refValue = prismContext.itemFactory().createReferenceValue(refTarget);
        return createModificationDelete(refName, objectDefinition, refValue);
    }

    public static ReferenceDeltaImpl createModificationDelete(QName refName, PrismObjectDefinition<?> objectDefinition,
            PrismReferenceValue refValue) {
        PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(ItemName.fromQName(refName), PrismReferenceDefinition.class);
        ReferenceDeltaImpl referenceDelta = new ReferenceDeltaImpl(ItemName.fromQName(refName), referenceDefinition, objectDefinition.getPrismContext());              // hoping the prismContext is there
        referenceDelta.addValueToDelete(refValue);
        return referenceDelta;
    }

    public static <T extends Objectable> ReferenceDeltaImpl createModificationDelete(Class<T> type, QName refName, PrismContext prismContext,
            PrismReferenceValue refValue) {
        PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
        return createModificationDelete(refName, objectDefinition, refValue);
    }

    public static <T extends Objectable> Collection<? extends ItemDelta<?, ?>> createModificationDeleteCollection(
            Class<T> type, QName refName, PrismContext prismContext, PrismReferenceValue refValue) {
        return List.of(createModificationDelete(type, refName, prismContext, refValue));
    }

    public static <T extends Objectable> ReferenceDeltaImpl createModificationDelete(Class<T> type, QName refName, PrismContext prismContext,
            PrismObject<?> refTarget) {
        PrismReferenceValue refValue = prismContext.itemFactory().createReferenceValue(refTarget);
        return createModificationDelete(type, refName, prismContext, refValue);
    }

    public static <T extends Objectable> Collection<? extends ItemDelta<?, ?>> createModificationDeleteCollection(Class<T> type, QName refName, PrismContext prismContext,
            PrismObject<?> refTarget) {
        return List.of(createModificationDelete(type, refName, prismContext, refTarget));
    }
}

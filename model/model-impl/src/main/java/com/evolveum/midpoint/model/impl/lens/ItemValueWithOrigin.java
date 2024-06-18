/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.construction.ResourceObjectConstruction;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

/**
 * Value of an item along with the information where it came from: {@link #producer} and {@link #construction}.
 *
 * @author semancik
 */
public class ItemValueWithOrigin<V extends PrismValue, D extends ItemDefinition<?>> implements DebugDumpable {

    private V itemValue;

    /** What produced the {@link #itemValue}. This is typically a mapping. */
    private final PrismValueDeltaSetTripleProducer<V, D> producer;

    /** This is used to provide some useful information, like origin object, validity, and so on (why it's tied to ROC?) */
    private final ResourceObjectConstruction<?, ?> construction;

    public ItemValueWithOrigin(V itemValue,
            PrismValueDeltaSetTripleProducer<V, D> producer, ResourceObjectConstruction<?, ?> construction) {
        this.itemValue = itemValue;
        this.producer = producer;
        this.construction = construction;
    }

    // the same as above, but with correct name
    public V getItemValue() {
        return itemValue;
    }

    // use with care
    public void setItemValue(V value) {
        this.itemValue = value;
    }

    public PrismValueDeltaSetTripleProducer<V, D> getProducer() {
        return producer;
    }

    public boolean isMappingStrong() {
        return producer != null && producer.isStrong();
    }

    public boolean isMappingWeak() {
        return producer != null && producer.isWeak();
    }

    String getMappingIdentifier() {
        return producer != null ? producer.getIdentifier() : null;
    }

    public ResourceObjectConstruction<?, ?> getConstruction() {
        return construction;
    }

    public ObjectType getSource() {
        return construction != null ? construction.getSource() : null;
    }

    public boolean isValid() {
        return construction == null || construction.isValid();
    }

    boolean wasValid() {
        return construction == null || construction.getWasValid();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ItemValueWithOrigin<V, D> clone() {
        //noinspection unchecked
        return new ItemValueWithOrigin<>(
                itemValue != null ? (V) itemValue.clone() : null,
                producer != null ? producer.clone() : null,
                construction);
    }

    public static <V extends PrismValue, D extends ItemDefinition<?>> DeltaSetTriple<ItemValueWithOrigin<V, D>>
    createOutputTriple(PrismValueDeltaSetTripleProducer<V, D> mapping) {
        PrismValueDeltaSetTriple<V> outputTriple = mapping.getOutputTriple();
        if (outputTriple == null) {
            return null;
        }
        Collection<ItemValueWithOrigin<V,D>> zeroIvwoSet = convertSet(outputTriple.getZeroSet(), mapping);
        Collection<ItemValueWithOrigin<V,D>> plusIvwoSet = convertSet(outputTriple.getPlusSet(), mapping);
        Collection<ItemValueWithOrigin<V,D>> minusIvwoSet = convertSet(outputTriple.getMinusSet(), mapping);
        return PrismContext.get().deltaFactory().createDeltaSetTriple(zeroIvwoSet, plusIvwoSet, minusIvwoSet);
    }

    private static <V extends PrismValue, D extends ItemDefinition<?>> @NotNull Collection<ItemValueWithOrigin<V,D>> convertSet(
            @NotNull Collection<V> valueSet, PrismValueDeltaSetTripleProducer<V, D> mapping) {
        Collection<ItemValueWithOrigin<V,D>> ivwoSet = new ArrayList<>(valueSet.size());
        for (V value: valueSet) {
            ivwoSet.add(
                    new ItemValueWithOrigin<>(value, mapping, null));
        }
        return ivwoSet;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ItemValueWithOrigin:\n");
        DebugUtil.debugDumpWithLabel(sb, "itemValue", itemValue, indent +1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabelToString(sb, "producer", producer, indent +1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabelToString(sb, "construction", construction, indent +1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ItemValueWithOrigin(" + itemValue + ", M=" + producer + ", C=" + construction + ")";
    }

    public boolean isStrong() {
        return producer != null && producer.isStrong();
    }

    public boolean isNormal() {
        return producer != null && producer.isNormal();
    }

    public boolean isWeak() {
        return producer != null && producer.isWeak();
    }

    boolean isSourceless() {
        return producer != null && producer.isSourceless();
    }

    @Experimental
    public boolean isPushChanges() {
        return producer != null && producer.isPushChanges();
    }
}

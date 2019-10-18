/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.extensions;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.util.Cloner;
import com.evolveum.midpoint.util.Processor;
import com.evolveum.midpoint.util.Transformer;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 */
public class AbstractDelegatedPrismValueDeltaSetTriple<V extends PrismValue> implements PrismValueDeltaSetTriple<V> {

    protected PrismValueDeltaSetTriple<V> inner;

    public AbstractDelegatedPrismValueDeltaSetTriple(PrismValueDeltaSetTriple<V> inner) {
        this.inner = inner;
    }

    public AbstractDelegatedPrismValueDeltaSetTriple(PrismContext prismContext) {
        inner = prismContext.deltaFactory().createPrismValueDeltaSetTriple();
    }

    public AbstractDelegatedPrismValueDeltaSetTriple(Collection<V> zeroSet, Collection<V> plusSet,
            Collection<V> minusSet, PrismContext prismContext) {
        inner = prismContext.deltaFactory().createPrismValueDeltaSetTriple(zeroSet, plusSet, minusSet);
    }

    @Override
    public <O extends PrismValue> void distributeAs(V myMember,
            PrismValueDeltaSetTriple<O> otherTriple, O otherMember) {
        inner.distributeAs(myMember, otherTriple, otherMember);
    }

    @Override
    public Class<V> getValueClass() {
        return inner.getValueClass();
    }

    @Override
    public Class<?> getRealValueClass() {
        return inner.getRealValueClass();
    }

    @Override
    public boolean isRaw() {
        return inner.isRaw();
    }

    @Override
    public void applyDefinition(ItemDefinition itemDefinition) throws SchemaException {
        inner.applyDefinition(itemDefinition);
    }

    @Override
    public void setOriginType(OriginType sourceType) {
        inner.setOriginType(sourceType);
    }

    @Override
    public void setOriginObject(Objectable originObject) {
        inner.setOriginObject(originObject);
    }

    @Override
    public void removeEmptyValues(boolean allowEmptyValues) {
        inner.removeEmptyValues(allowEmptyValues);
    }

    @Override
    public PrismValueDeltaSetTriple<V> clone() {
        return inner.clone();
    }

    @Override
    public void checkConsistence() {
        inner.checkConsistence();
    }

    @Override
    public void accept(Visitor visitor) {
        inner.accept(visitor);
    }

    @Override
    public void checkNoParent() {
        inner.checkNoParent();
    }

    public static <T> DeltaSetTriple<T> diff(Collection<T> valuesOld,
            Collection<T> valuesNew, PrismContext prismContext) {
        return DeltaSetTriple.diff(valuesOld, valuesNew, prismContext);
    }

    @Override
    @NotNull
    public Collection<V> getZeroSet() {
        return inner.getZeroSet();
    }

    @Override
    @NotNull
    public Collection<V> getPlusSet() {
        return inner.getPlusSet();
    }

    @Override
    @NotNull
    public Collection<V> getMinusSet() {
        return inner.getMinusSet();
    }

    @Override
    public boolean hasPlusSet() {
        return inner.hasPlusSet();
    }

    @Override
    public boolean hasZeroSet() {
        return inner.hasZeroSet();
    }

    @Override
    public boolean hasMinusSet() {
        return inner.hasMinusSet();
    }

    @Override
    public boolean isZeroOnly() {
        return inner.isZeroOnly();
    }

    @Override
    public void addToPlusSet(V item) {
        inner.addToPlusSet(item);
    }

    @Override
    public void addToMinusSet(V item) {
        inner.addToMinusSet(item);
    }

    @Override
    public void addToZeroSet(V item) {
        inner.addToZeroSet(item);
    }

    @Override
    public void addAllToPlusSet(Collection<V> items) {
        inner.addAllToPlusSet(items);
    }

    @Override
    public void addAllToMinusSet(Collection<V> items) {
        inner.addAllToMinusSet(items);
    }

    @Override
    public void addAllToZeroSet(Collection<V> items) {
        inner.addAllToZeroSet(items);
    }

    @Override
    public Collection<V> getSet(PlusMinusZero whichSet) {
        return inner.getSet(whichSet);
    }

    @Override
    public void addAllToSet(PlusMinusZero destination, Collection<V> items) {
        inner.addAllToSet(destination, items);
    }

    @Override
    public void addToSet(PlusMinusZero destination, V item) {
        inner.addToSet(destination, item);
    }

    @Override
    public boolean presentInPlusSet(V item) {
        return inner.presentInPlusSet(item);
    }

    @Override
    public boolean presentInMinusSet(V item) {
        return inner.presentInMinusSet(item);
    }

    @Override
    public boolean presentInZeroSet(V item) {
        return inner.presentInZeroSet(item);
    }

    @Override
    public void clearPlusSet() {
        inner.clearPlusSet();
    }

    @Override
    public void clearMinusSet() {
        inner.clearMinusSet();
    }

    @Override
    public void clearZeroSet() {
        inner.clearZeroSet();
    }

    @Override
    public int size() {
        return inner.size();
    }

    @Override
    public Collection<V> union() {
        return inner.union();
    }

    @Override
    public V getAnyValue() {
        return inner.getAnyValue();
    }

    @Override
    public Collection<V> getAllValues() {
        return inner.getAllValues();
    }

    @Override
    public Stream<V> stream() {
        return inner.stream();
    }

    @Override
    @NotNull
    public Collection<V> getNonNegativeValues() {
        return inner.getNonNegativeValues();
    }

    @Override
    @NotNull
    public Collection<V> getNonPositiveValues() {
        return inner.getNonPositiveValues();
    }

    @Override
    public void merge(DeltaSetTriple<V> triple) {
        inner.merge(triple);
    }

    @Override
    public DeltaSetTriple<V> clone(Cloner<V> cloner) {
        return inner.clone(cloner);
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public void foreach(Processor<V> processor) {
        inner.foreach(processor);
    }

    @Override
    public void simpleAccept(SimpleVisitor<V> visitor) {
        inner.simpleAccept(visitor);
    }

    @Override
    public <X> void transform(DeltaSetTriple<X> transformTarget,
            Transformer<V, X> transformer) {
        inner.transform(transformTarget, transformer);
    }

    @Override
    public void debugDumpSets(StringBuilder sb, Consumer<V> dumper, int indent) {
        inner.debugDumpSets(sb, dumper, indent);
    }

    @Override
    public String toHumanReadableString() {
        return inner.toHumanReadableString();
    }

    @Override
    public String debugDump(int indent) {
        return inner.debugDump(indent);
    }

    @Override
    public void shortDump(StringBuilder sb) {
        inner.shortDump(sb);
    }

}

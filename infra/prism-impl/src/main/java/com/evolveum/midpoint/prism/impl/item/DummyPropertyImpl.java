/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.item;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.PartiallyResolvedItem;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class DummyPropertyImpl<T> extends DummyItem<PrismPropertyValue<T>, PrismPropertyDefinition<T>, PrismProperty<T>> implements PrismProperty<T> {
    private static final long serialVersionUID = 1L;

    public DummyPropertyImpl(PrismProperty<T> realProperty, @NotNull ItemPath path) {
        super(realProperty, path);
    }

    public <X> List<PrismPropertyValue<X>> getValues(Class<X> type) {
        return delegate().getValues(type);
    }

    public PrismPropertyValue<T> getValue() {
        return delegate().getValue();
    }

    @NotNull
    public Collection<T> getRealValues() {
        return delegate().getRealValues();
    }

    public <X> Collection<X> getRealValues(Class<X> type) {
        return delegate().getRealValues(type);
    }

    public T getAnyRealValue() {
        return delegate().getAnyRealValue();
    }

    public T getRealValue() {
        return delegate().getRealValue();
    }

    public <X> X getRealValue(Class<X> type) {
        return delegate().getRealValue(type);
    }

    public <X> X[] getRealValuesArray(Class<X> type) {
        return delegate().getRealValuesArray(type);
    }

    public <X> PrismPropertyValue<X> getValue(Class<X> type) {
        return delegate().getValue(type);
    }

    public void setValue(PrismPropertyValue<T> value) {
        delegate().setValue(value);
    }

    public void setRealValue(T realValue) {
        delegate().setRealValue(realValue);
    }

    public void setRealValues(T... realValues) {
        delegate().setRealValues(realValues);
    }

    public void addValues(Collection<PrismPropertyValue<T>> pValuesToAdd) {
        delegate().addValues(pValuesToAdd);
    }

    public void addValue(PrismPropertyValue<T> pValueToAdd) {
        delegate().addValue(pValueToAdd);
    }

    public void addRealValue(T valueToAdd) {
        delegate().addRealValue(valueToAdd);
    }

    @Override
    public void addRealValueSkipUniquenessCheck(T valueToAdd) {
        delegate().addRealValueSkipUniquenessCheck(valueToAdd);
    }

    public void addRealValues(T... valuesToAdd) {
        delegate().addRealValues(valuesToAdd);
    }

    public String getHelp() {
        return delegate().getHelp();
    }

    public boolean deleteValues(Collection<PrismPropertyValue<T>> pValuesToDelete) {
        return delegate().deleteValues(pValuesToDelete);
    }

    public boolean deleteValue(PrismPropertyValue<T> pValueToDelete) {
        return delegate().deleteValue(pValueToDelete);
    }

    public void replaceValues(Collection<PrismPropertyValue<T>> valuesToReplace) {
        delegate().replaceValues(valuesToReplace);
    }

    public boolean hasRealValue(PrismPropertyValue<T> value) {
        return delegate().hasRealValue(value);
    }

    public Class<T> getValueClass() {
        return delegate().getValueClass();
    }

    public PropertyDelta<T> createDelta() {
        return delegate().createDelta();
    }

    public PropertyDelta<T> createDelta(ItemPath path) {
        return delegate().createDelta(path);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> PartiallyResolvedItem<IV, ID> findPartial(
            ItemPath path) {
        return delegate().findPartial(path);
    }

    public PropertyDelta<T> diff(PrismProperty<T> other) {
        return delegate().diff(other);
    }

    public PropertyDelta<T> diff(PrismProperty<T> other, ParameterizedEquivalenceStrategy strategy) {
        return delegate().diff(other, strategy);
    }

    public PrismProperty<T> clone() {
        return delegate().clone();
    }

    @Override
    public PrismProperty<T> createImmutableClone() {
        return delegate().createImmutableClone();
    }

    public PrismProperty<T> cloneComplex(CloneStrategy strategy) {
        return delegate().cloneComplex(strategy);
    }

    public String toHumanReadableString() {
        return delegate().toHumanReadableString();
    }

}

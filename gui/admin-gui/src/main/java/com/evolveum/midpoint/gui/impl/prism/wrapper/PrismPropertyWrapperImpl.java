/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import org.jetbrains.annotations.NotNull;

/**
 * @author katka
 *
 */
public class PrismPropertyWrapperImpl<T> extends ItemWrapperImpl<PrismProperty<T>, PrismPropertyValueWrapper<T>> implements PrismPropertyWrapper<T> {

    private static final long serialVersionUID = 1L;

    private LookupTableType predefinedValues;

    public PrismPropertyWrapperImpl(PrismContainerValueWrapper<?> parent, PrismProperty<T> item, ItemStatus status) {
        super(parent, item, status);
    }

    @Override
    public Collection<? extends DisplayableValue<T>> getAllowedValues() {
        return getItemDefinition().getAllowedValues();
    }

    @Override
    public T defaultValue() {
        return getItemDefinition().defaultValue();
    }

    @Override
    @Deprecated
    public QName getValueType() {
        return getItemDefinition().getValueType();
    }

    @Override
    public Boolean isIndexed() {
        return getItemDefinition().isIndexed();
    }

    @Override
    public QName getMatchingRuleQName() {
        return getItemDefinition().getMatchingRuleQName();
    }


    @Override
    public PropertyDelta<T> createEmptyDelta(ItemPath path) {
        return getItemDefinition().createEmptyDelta(path);
    }

    @NotNull
    @Override
    public PrismPropertyDefinition<T> clone() {
        return getItemDefinition().clone();
    }

    @Override
    public MutablePrismPropertyDefinition<T> toMutable() {
        return getItemDefinition().toMutable();
    }

    @NotNull
    @Override
    public PrismProperty<T> instantiate() {
        return getItemDefinition().instantiate();
    }

    @NotNull
    @Override
    public PrismProperty<T> instantiate(QName name) {
        return getItemDefinition().instantiate(name);
    }

    @Override
    public LookupTableType getPredefinedValues() {
        return predefinedValues;
    }

    public void setPredefinedValues(LookupTableType predefinedValues) {
        this.predefinedValues = predefinedValues;
    }

    @Override
    public boolean isEmpty() {
        if (super.isEmpty()) return true;
        List<PrismPropertyValue<T>> pVals = getItem().getValues();
        boolean allEmpty = true;
        for (PrismPropertyValue<T> pVal : pVals) {
            if (pVal.getRealValue() != null) {
                allEmpty = false;
                break;
            }
        }

        return allEmpty;
    }

    public PrismPropertyDefinition<T> getItemDefinition() {
        return super.getItemDefinition();
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
}

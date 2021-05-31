/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.deleg;

import java.util.Collection;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;

public interface PropertyDefinitionDelegator<T> extends ItemDefinitionDelegator<PrismProperty<T>>, PrismPropertyDefinition<T> {

    @Override
    PrismPropertyDefinition<T> delegate();

    @Override
    default Collection<? extends DisplayableValue<T>> getAllowedValues() {
        return delegate().getAllowedValues();
    }

    @Override
    default T defaultValue() {
        return delegate().defaultValue();
    }

    @Deprecated
    @Override
    default QName getValueType() {
        return delegate().getValueType();
    }

    @Override
    default Boolean isIndexed() {
        return delegate().isIndexed();
    }

    @Override
    default boolean isAnyType() {
        return delegate().isAnyType();
    }

    @Override
    default QName getMatchingRuleQName() {
        return delegate().getMatchingRuleQName();
    }

    @Override
    default PropertyDelta<T> createEmptyDelta(ItemPath path) {
        return delegate().createEmptyDelta(path);
    }

    @Override
    default @NotNull PrismProperty<T> instantiate() {
        return delegate().instantiate();
    }

    @Override
    default @NotNull PrismProperty<T> instantiate(QName name) {
        return delegate().instantiate(name);
    }

    @Override
    default Class<T> getTypeClass() {
        return delegate().getTypeClass();
    }
}

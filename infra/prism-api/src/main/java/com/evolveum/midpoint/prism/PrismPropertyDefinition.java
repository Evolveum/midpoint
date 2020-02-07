/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * @author mederly
 */
public interface PrismPropertyDefinition<T> extends ItemDefinition<PrismProperty<T>> {

    Collection<? extends DisplayableValue<T>> getAllowedValues();

    T defaultValue();

    /**
     * Returns QName of the property value type.
     * <p>
     * The returned type is either XSD simple type or complex type. It may not
     * be defined in the same schema (especially if it is standard XSD simple
     * type).
     *
     * @return QName of the property value type
     *
     * NOTE: This is very strange property. Isn't it the same as typeName().
     * It is even not used in midPoint. Marking as deprecated.
     */
    @Deprecated
    QName getValueType();

    Boolean isIndexed();

    default boolean isAnyType() {
        return DOMUtil.XSD_ANYTYPE.equals(getTypeName());
    }

    QName getMatchingRuleQName();

    @Override
    PropertyDelta<T> createEmptyDelta(ItemPath path);

    @NotNull
    @Override
    PrismProperty<T> instantiate();

    @NotNull
    @Override
    PrismProperty<T> instantiate(QName name);

    @NotNull
    @Override
    PrismPropertyDefinition<T> clone();

    @Override
    Class<T> getTypeClass();

    @Override
    MutablePrismPropertyDefinition<T> toMutable();
}

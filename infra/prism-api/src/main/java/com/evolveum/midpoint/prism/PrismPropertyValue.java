/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import java.io.Serializable;

/**
 * @author lazyman
 */
public interface PrismPropertyValue<T> extends DebugDumpable, Serializable, PrismValue {

    void setValue(T value);

    T getValue();

    XNode getRawElement();

    void setRawElement(XNode rawElement);

    @Nullable
    ExpressionWrapper getExpression();

    void setExpression(@Nullable ExpressionWrapper expression);

    <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path);

    PrismPropertyValue<T> clone();

    @Override
    PrismPropertyValue<T> cloneComplex(CloneStrategy strategy);

    boolean equals(PrismPropertyValue<?> other, ParameterizedEquivalenceStrategy strategy, MatchingRule<T> matchingRule);

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

    String debugDump(int indent, boolean detailedDump);

    /**
     * Returns JAXBElement corresponding to the this value.
     * Name of the element is the name of parent property; its value is the real value of the property.
     *
     * @return Created JAXBElement.
     */
    JAXBElement<T> toJaxbElement();

    @Override
    Class<?> getRealClass();

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    T getRealValue();

}

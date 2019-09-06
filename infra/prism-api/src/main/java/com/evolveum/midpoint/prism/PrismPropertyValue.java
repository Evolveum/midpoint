/*
 * Copyright (c) 2010-2018 Evolveum and contributors
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

	boolean isRaw();

	@Nullable
	ExpressionWrapper getExpression();

	void setExpression(@Nullable ExpressionWrapper expression);

	@Override
	void applyDefinition(ItemDefinition definition) throws SchemaException;

	@Override
	void applyDefinition(ItemDefinition definition, boolean force) throws SchemaException;

	@Override
	void revive(PrismContext prismContext) throws SchemaException;

	void recompute(PrismContext prismContext);

	Object find(ItemPath path);

	<IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path);

    @Override
    void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope);

	boolean isEmpty();

    PrismPropertyValue<T> clone();
	
	@Override
	PrismPropertyValue<T> cloneComplex(CloneStrategy strategy);

	boolean equals(PrismPropertyValue<?> other, ParameterizedEquivalenceStrategy strategy, MatchingRule<T> matchingRule);

	@Override
	boolean equals(Object obj);

	@Override
	int hashCode();

	@Override
	String debugDump();

	@Override
	String debugDump(int indent);

	String debugDump(int indent, boolean detailedDump);

	@Override
	String toString();

	String toHumanReadableString();

	/**
     * Returns JAXBElement corresponding to the this value.
     * Name of the element is the name of parent property; its value is the real value of the property.
     *
     * @return Created JAXBElement.
     */
	JAXBElement<T> toJaxbElement();

	@Override
	Class<?> getRealClass();

	@Nullable
	@Override
	<T> T getRealValue();

}

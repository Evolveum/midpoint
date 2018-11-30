/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.XNodeImpl;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * @author lazyman
 */
public interface PrismPropertyValue<T> extends DebugDumpable, Serializable, PrismValue {
	
	static <T> PrismPropertyValue<T> createRaw(XNodeImpl rawElement) {
		PrismPropertyValue<T> pval = new PrismPropertyValueImpl<T>();
		pval.setRawElement(rawElement);
		return pval;
	}

    void setValue(T value);

	T getValue();

	static <T> Collection<T> getValues(Collection<PrismPropertyValue<T>> pvals) {
    	Collection<T> realValues = new ArrayList<>(pvals.size());
		for (PrismPropertyValue<T> pval: pvals) {
			realValues.add(pval.getValue());
		}
		return realValues;
    }

    XNodeImpl getRawElement();

	void setRawElement(XNodeImpl rawElement);

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

	static boolean containsRealValue(Collection<PrismPropertyValue<?>> collection, PrismPropertyValue<?> value) {
		for (PrismPropertyValue<?> colVal: collection) {
			if (value.equalsRealValue(colVal)) {
				return true;
			}
		}
		return false;
	}

	static boolean containsValue(Collection<PrismPropertyValue> collection, PrismPropertyValue value, Comparator comparator) {
		for (PrismPropertyValue<?> colVal: collection) {
			if (comparator.compare(colVal, value) == 0) {
				return true;
			}
		}
		return false;
	}

	static <T> Collection<PrismPropertyValue<T>> createCollection(Collection<T> realValueCollection) {
		Collection<PrismPropertyValue<T>> pvalCol = new ArrayList<>(realValueCollection.size());
		for (T realValue: realValueCollection) {
			PrismPropertyValue<T> pval = new PrismPropertyValueImpl<>(realValue);
			pvalCol.add(pval);
		}
		return pvalCol;
	}

	static <T> Collection<PrismPropertyValue<T>> createCollection(T[] realValueArray) {
		Collection<PrismPropertyValue<T>> pvalCol = new ArrayList<>(realValueArray.length);
		for (T realValue: realValueArray) {
			PrismPropertyValue<T> pval = new PrismPropertyValueImpl<T>(realValue);
			pvalCol.add(pval);
		}
		return pvalCol;
	}

	@Override
	boolean equalsComplex(PrismValue other, boolean ignoreMetadata, boolean isLiteral);

	boolean equalsComplex(PrismPropertyValue<?> other, boolean ignoreMetadata, boolean isLiteral, MatchingRule<T> matchingRule);

	boolean match(PrismValue otherValue);

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

	static <T> Collection<PrismPropertyValue<T>> wrap(@NotNull Collection<T> realValues) {
		return realValues.stream()
				.map(val -> new PrismPropertyValueImpl<>(val))
				.collect(Collectors.toList());
	}
}

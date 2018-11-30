/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.ListXNodeImpl;
import com.evolveum.midpoint.prism.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.xnode.XNodeImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * Property is a specific characteristic of an object. It may be considered
 * object "attribute" or "field". For example User has fullName property that
 * contains string value of user's full name.
 * <p>
 * Properties may be single-valued or multi-valued
 * <p>
 * Properties may contain primitive types or complex types (defined by XSD
 * schema)
 * <p>
 * Property values are unordered, implementation may change the order of values
 * <p>
 * Duplicate values of properties should be silently removed by implementations,
 * but clients must be able tolerate presence of duplicate values.
 * <p>
 * Operations that modify the objects work with the granularity of properties.
 * They add/remove/replace the values of properties, but do not "see" inside the
 * property.
 * <p>
 * Property is mutable.
 *
 * @author Radovan Semancik
 */
public interface PrismProperty<T> extends Item<PrismPropertyValue<T>,PrismPropertyDefinition<T>> {

	/**
     * Returns applicable property definition.
     * <p>
     * May return null if no definition is applicable or the definition is not
     * know.
     *
     * @return applicable property definition
     */
	PrismPropertyDefinition<T> getDefinition();

	/**
     * Sets applicable property definition.
     *
	 * TODO remove (method in Item is sufficient)
     * @param definition the definition to set
     */
	void setDefinition(PrismPropertyDefinition<T> definition);

	PrismPropertyValue<T> getValue();

	/**
     * Type override, also for compatibility.
     */
	<X> List<PrismPropertyValue<X>> getValues(Class<X> type);

	@NotNull
    @Override
	Collection<T> getRealValues();

	/**
     * Type override, also for compatibility.
     */

	<X> Collection<X> getRealValues(Class<X> type);

	T getAnyRealValue();

	PrismPropertyValue<T> getAnyValue();

	@Override
	T getRealValue();

	/**
     * Type override, also for compatibility.
     */
	<X> X getRealValue(Class<X> type);

	/**
     * Type override, also for compatibility.
     */
	<X> X[] getRealValuesArray(Class<X> type);

	/**
     * Type override, also for compatibility.
     */
	<X> PrismPropertyValue<X> getValue(Class<X> type);

	/**
     * Means as a short-hand for setting just a value for single-valued attributes.
     * Will remove all existing values.
     */
	void setValue(PrismPropertyValue<T> value);

	void setRealValue(T realValue);

	void setRealValues(T... realValues);

	void addValues(Collection<PrismPropertyValue<T>> pValuesToAdd);

	void addValue(PrismPropertyValue<T> pValueToAdd);

	void addRealValue(T valueToAdd);

	void addRealValues(T... valuesToAdd);

	boolean deleteValues(Collection<PrismPropertyValue<T>> pValuesToDelete);

	boolean deleteValue(PrismPropertyValue<T> pValueToDelete);

	void replaceValues(Collection<PrismPropertyValue<T>> valuesToReplace);

	boolean hasValue(PrismPropertyValue<T> value);

	boolean hasRealValue(PrismPropertyValue<T> value);

	@Override
	PrismPropertyValue<T> getPreviousValue(PrismValue value);

	@Override
	PrismPropertyValue<T> getNextValue(PrismValue value);

	Class<T> getValueClass();

	@Override
	PropertyDelta<T> createDelta();

	@Override
	PropertyDelta<T> createDelta(ItemPath path);

	@Override
	Object find(ItemPath path);

	@Override
	<IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path);

	PropertyDelta<T> diff(PrismProperty<T> other);

	PropertyDelta<T> diff(PrismProperty<T> other, boolean ignoreMetadata, boolean isLiteral);

	@Override
	PrismProperty<T> clone();

	@Override
	PrismProperty<T> cloneComplex(CloneStrategy strategy);

	@Override
	String toString();

	@Override
	String debugDump(int indent);

	String toHumanReadableString();

	static <T> PrismProperty<T> createRaw(@NotNull XNodeImpl node, @NotNull QName itemName, PrismContext prismContext)
			throws SchemaException {
		Validate.isTrue(!(node instanceof RootXNodeImpl));
		PrismProperty<T> property = new PrismPropertyImpl<T>(itemName, prismContext);
		if (node instanceof ListXNodeImpl) {
			for (XNodeImpl subnode : (ListXNodeImpl) node) {
				property.add(PrismPropertyValue.createRaw(subnode));
			}
		} else {
			property.add(PrismPropertyValue.createRaw(node));
		}
		return property;
	}

	static <T> T getRealValue(PrismProperty<T> property) {
    	return property != null ? property.getRealValue() : null;
	}
}

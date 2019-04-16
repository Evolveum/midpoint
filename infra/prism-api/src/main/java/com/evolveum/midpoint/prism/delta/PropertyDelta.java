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

package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.Collection;

/**
 * Relative difference (delta) of a property values.
 * <p>
 * This class describes what values are to be added, removed or replaced in the property.
 * The delta can be either add+delete or replace, but not both. It either describes what
 * values to add and delete from the property (add+delete) or what is the new set of values
 * (replace). Add+delete deltas can be merged without a loss. There are ideal for multi-value
 * properties. If replace deltas are merged, only the last value will be present. These are
 * better suited for single-value properties.
 *
 * @author Radovan Semancik
 * @see ObjectDelta
 */
public interface PropertyDelta<T extends Object> extends ItemDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>> {

    PrismPropertyDefinition<T> getPropertyDefinition();

	void setPropertyDefinition(PrismPropertyDefinition<T> propertyDefinition);

	@Override
	void setDefinition(PrismPropertyDefinition<T> definition);

	@Override
	void applyDefinition(PrismPropertyDefinition<T> definition) throws SchemaException;

	@Override
	Class<PrismProperty> getItemClass();

	/**
     * Returns all values regardless of whether they are added or removed or replaced.
     * Useful for iterating over all the changed values.
     */
	<T> Collection<PrismPropertyValue<T>> getValues(Class<T> type);

	T getAnyRealValue();

	<P extends PrismProperty> P instantiateEmptyProperty();

	boolean isApplicableToType(Item item);

	@Override
	PropertyDelta<T> clone();

    boolean isRealValueToAdd(PrismPropertyValue<?> value);

	boolean isRealValueToDelete(PrismPropertyValue<?> value);

	/**
     * Returns the "new" state of the property - the state that would be after the delta
     * is applied.
     */
	PrismProperty<T> getPropertyNewMatchingPath() throws SchemaException;

	/**
     * Returns the "new" state of the property - the state that would be after the delta
     * is applied.
     */
	PrismProperty<T> getPropertyNewMatchingPath(PrismProperty<T> propertyOld) throws SchemaException;

	@Override
	PropertyDelta<T> narrow(PrismObject<? extends Objectable> object, boolean assumeMissingItems);

	PropertyDelta<T> narrow(PrismObject<? extends Objectable> object, final MatchingRule<T> matchingRule,
			boolean assumeMissingItems);

	boolean isRedundant(PrismObject<? extends Objectable> object, final MatchingRule<T> matchingRule, boolean assumeMissingItems);

    // convenience method
    void setRealValuesToReplace(T... newValues);

	void addRealValuesToAdd(T... newValues);

	void addRealValuesToDelete(T... newValues);

	void addRealValuesToAdd(Collection<T> newValues);

	void addRealValuesToDelete(Collection<T> values);
}

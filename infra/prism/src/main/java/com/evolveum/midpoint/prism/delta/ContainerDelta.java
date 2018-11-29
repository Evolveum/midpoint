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

package com.evolveum.midpoint.prism.delta;

import java.util.*;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;

public interface ContainerDelta<V extends Containerable> extends ItemDelta<PrismContainerValue<V>,PrismContainerDefinition<V>>, PrismContainerable<V> {

	@Override
	Class<PrismContainer> getItemClass();

	/**
     * Returns all values regardless of whether they are added or removed or replaced.
     * Useful for iterating over all the changed values.
     */
	<T extends Containerable> Collection<PrismContainerValue<T>> getValues(Class<T> type);

	@Override
	void setDefinition(PrismContainerDefinition<V> definition);

	@Override
	void applyDefinition(PrismContainerDefinition<V> definition) throws SchemaException;

	@Override
	boolean hasCompleteDefinition();

	@Override
	Class<V> getCompileTimeClass();

	boolean isApplicableToType(Item item);

	@Override
	ItemDelta<?,?> getSubDelta(ItemPath path);

	/**
	 * Post processing of delta to expand missing values from the object. E.g. a delete deltas may
	 * be "id-only" so they contain only id of the value to delete. In such case locate the full value
	 * in the object and fill it into the delta.
	 * This method may even delete id-only values that are no longer present in the object.
	 */
	<O extends Objectable> void expand(PrismObject<O> object, Trace logger) throws SchemaException;

	@Override
	void checkConsistence(boolean requireDefinition, boolean prohibitRaw, ConsistencyCheckScope scope);

    @Override
    ContainerDelta<V> clone();

}

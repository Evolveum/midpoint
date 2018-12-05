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
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *
 */
public interface DeltaFactory {

	interface Property {

		<T> PropertyDelta<T> createAddDelta(PrismObjectDefinition<? extends Objectable> objectDefinition,
				ItemName propertyName, T... realValues);

		<T> PropertyDelta<T> createDeleteDelta(PrismObjectDefinition<? extends Objectable> objectDefinition,
				ItemName propertyName, T... realValues);

		<T> PropertyDelta<T> create(PrismPropertyDefinition<T> propertyDefinition);

		<T> PropertyDelta<T> create(ItemPath path, PrismPropertyDefinition<T> definition);

		<O extends Objectable, T> PropertyDelta<T> createReplaceDelta(PrismContainerDefinition<O> containerDefinition,
				QName propertyName, T... realValues);

		<O extends Objectable, T> PropertyDelta<T> createReplaceDelta(PrismContainerDefinition<O> containerDefinition,
				QName propertyName, PrismPropertyValue<T>... pValues);

		<O extends Objectable> PropertyDelta createReplaceEmptyDelta(PrismObjectDefinition<O> objectDefinition,
				ItemPath propertyPath);

		<T> PropertyDelta<T> create(ItemPath itemPath, QName name, PrismPropertyDefinition<T> propertyDefinition);

		<O extends Objectable, T> PropertyDelta<T> createReplaceDeltaOrEmptyDelta(PrismObjectDefinition<O> objectDefinition,
				QName propertyName, T realValue);

		<O extends Objectable,T> PropertyDelta<T> createDelta(ItemPath propertyPath, PrismObjectDefinition<O> objectDefinition);

		<O extends Objectable,T> PropertyDelta<T> createDelta(ItemPath propertyPath, Class<O> compileTimeClass,
				PrismContext prismContext);

		<T> PropertyDelta<T> createModificationReplaceProperty(ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition,
				T... propertyValues);

		<T> PropertyDelta<T> createModificationReplaceProperty(ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition,
				Collection<T> propertyValues);

		<T> PropertyDelta<T> createModificationReplaceProperty(ItemPath path, PrismPropertyDefinition propertyDefinition,
				T... propertyValues);

		<T> PropertyDelta<T> createModificationAddProperty(ItemPath propertyPath, PrismPropertyDefinition propertyDefinition,
				T... propertyValues);

		<T> PropertyDelta<T> createModificationAddProperty(ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition,
				T... propertyValues);

		<T> PropertyDelta<T> createModificationDeleteProperty(ItemPath propertyPath, PrismPropertyDefinition propertyDefinition,
				T... propertyValues);

		<T> PropertyDelta<T> createModificationDeleteProperty(ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition,
				T... propertyValues);

		Collection<? extends ItemDelta> createModificationReplacePropertyCollection(QName propertyName,
				PrismObjectDefinition<?> objectDefinition, java.lang.Object... propertyValues);
	}

	interface Reference {

		ReferenceDelta create(ItemPath path, PrismReferenceDefinition definition);

		ReferenceDelta createModificationReplace(QName name, PrismObjectDefinition<? extends Objectable> objectDefinition, PrismReferenceValue referenceValue);
	}

	interface Container {
		<C extends Containerable> ContainerDelta<C> create(ItemPath path, PrismContainerDefinition<C> definition);

	}

	interface Object {
		<O extends Objectable> ObjectDelta<O> create(Class<O> type, ChangeType changeType);

	}

	Property property();
	Reference reference();
	Container container();
	Object object();

	<T> DeltaSetTriple<T> createDeltaSetTriple();

	<V extends PrismValue> PrismValueDeltaSetTriple<V> createPrismValueDeltaSetTriple();

}

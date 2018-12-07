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

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xnode.XNode;

import javax.xml.namespace.QName;

/**
 *  Factory for items (property, reference, container, object) and item values.
 *
 *  Eliminates the need of calls like "new PrismPropertyValue(...)" in midPoint 3.x.
 */
public interface ItemFactory {

	PrismValue createValue(Object realValue);

	<T> PrismProperty<T> createProperty(QName itemName);

	<T> PrismProperty<T> createProperty(QName itemName, PrismPropertyDefinition<T> definition);

	<T> PrismPropertyValue<T> createPropertyValue();

	<T> PrismPropertyValue<T> createPropertyValue(T content);

	<T> PrismPropertyValue<T> createPropertyValue(XNode rawContent);

	<T> PrismPropertyValue<T> createPropertyValue(T value, OriginType originType, Objectable originObject);

	PrismReference createReference(QName name);

	PrismReference createReference(QName name, PrismReferenceDefinition definition);

	PrismReferenceValue createReferenceValue();

	PrismReferenceValue createReferenceValue(PrismObject<?> target);

	PrismReferenceValue createReferenceValue(String targetOid);

	PrismReferenceValue createReferenceValue(String oid, OriginType originType, Objectable originObject);

	PrismReferenceValue createReferenceValue(String oid, QName targetType);

	PrismContainer createContainer(QName name);

	<C extends Containerable> PrismContainer<C> createContainer(QName name, PrismContainerDefinition<C> definition);

	<O extends Objectable> PrismObject<O> createObject(QName name, PrismObjectDefinition<O> definition);

	// TODO is this needed?
	<O extends Objectable> PrismObjectValue<O> createObjectValue(O objectable);

	// TODO is this needed?
	<C extends Containerable> PrismContainerValue<C> createContainerValue(C containerable);

	<C extends Containerable> PrismContainerValue<C> createContainerValue();
}

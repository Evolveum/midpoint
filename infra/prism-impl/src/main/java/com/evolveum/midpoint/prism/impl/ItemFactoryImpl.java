/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.item.DummyContainerImpl;
import com.evolveum.midpoint.prism.impl.item.DummyPropertyImpl;
import com.evolveum.midpoint.prism.impl.item.DummyReferenceImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 *
 */
public class ItemFactoryImpl implements ItemFactory {

	@NotNull private final PrismContextImpl prismContext;

	public ItemFactoryImpl(PrismContextImpl prismContext) {
		this.prismContext = prismContext;
	}

	@Override
	public <T> PrismProperty<T> createProperty(QName itemName) {
		return new PrismPropertyImpl<>(itemName, prismContext);
	}

	@Override
	public <T> PrismProperty<T> createProperty(QName itemName, PrismPropertyDefinition<T> definition) {
		return new PrismPropertyImpl<>(itemName, definition, prismContext);
	}

	@Override
	public <T> PrismPropertyValue<T> createPropertyValue() {
		return new PrismPropertyValueImpl<>(null, prismContext);
	}

	@Override
	public <T> PrismPropertyValue<T> createPropertyValue(T realValue) {
		return new PrismPropertyValueImpl<>(realValue, prismContext);
	}

	@Override
	public <T> PrismPropertyValue<T> createPropertyValue(XNode rawValue) {
		PrismPropertyValueImpl<T> rv = new PrismPropertyValueImpl<>(null, prismContext);
		rv.setRawElement(rawValue);
		return rv;
	}

	@Override
	public <T> PrismPropertyValue<T> createPropertyValue(T value, OriginType originType, Objectable originObject) {
		return new PrismPropertyValueImpl<>(value, prismContext, originType, originObject, null);
	}

	@Override
	public PrismReference createReference(QName name) {
		return new PrismReferenceImpl(name, null, prismContext);
	}

	@Override
	public PrismReference createReference(QName name, PrismReferenceDefinition definition) {
		return new PrismReferenceImpl(name, definition, prismContext);
	}

	@Override
	public PrismReferenceValue createReferenceValue() {
		return new PrismReferenceValueImpl(null);
	}

	@Override
	public PrismReferenceValue createReferenceValue(PrismObject<?> target) {
		PrismReferenceValue rv = new PrismReferenceValueImpl(target.getOid());
		rv.setPrismContext(prismContext);
		rv.setObject(target);
		if (target.getDefinition() != null) {
			rv.setTargetType(target.getDefinition().getTypeName());
		}
		return rv;
	}

	@Override
	public PrismReferenceValue createReferenceValue(String targetOid) {
		PrismReferenceValue rv = new PrismReferenceValueImpl(targetOid);
		rv.setPrismContext(prismContext);
		return rv;
	}

	@Override
	public PrismReferenceValue createReferenceValue(String oid, OriginType originType, Objectable originObject) {
		PrismReferenceValue rv = new PrismReferenceValueImpl(oid, originType, originObject);
		rv.setPrismContext(prismContext);
		return rv;
	}

	@Override
	public PrismReferenceValue createReferenceValue(String oid, QName targetType) {
		PrismReferenceValue rv = new PrismReferenceValueImpl(oid, targetType);
		rv.setPrismContext(prismContext);
		return rv;
	}

	@Override
	public PrismValue createValue(Object realValue) {
		if (realValue instanceof Containerable) {
			return ((Containerable) realValue).asPrismContainerValue();
		} else if (realValue instanceof Referencable) {
			return ((Referencable) realValue).asReferenceValue();
		} else {
			return createPropertyValue(realValue);
		}
	}

	@Override
	public PrismContainer createContainer(QName name) {
		return new PrismContainerImpl(name, prismContext);
	}

	@Override
	public <C extends Containerable> PrismContainer<C> createContainer(QName name, PrismContainerDefinition<C> definition) {
		return new PrismContainerImpl<>(name, definition, prismContext);
	}

	@Override
	public <O extends Objectable> PrismObject<O> createObject(QName name, PrismObjectDefinition<O> definition) {
		return new PrismObjectImpl<>(name, definition, prismContext);
	}

	@Override
	public <O extends Objectable> PrismObjectValue<O> createObjectValue(O objectable) {
		return new PrismObjectValueImpl<>(objectable, prismContext);
	}

	@Override
	public <C extends Containerable> PrismContainerValue<C> createContainerValue(C containerable) {
		return new PrismContainerValueImpl<>(containerable, prismContext);
	}

	@Override
	public <C extends Containerable> PrismContainerValue<C> createContainerValue() {
		return new PrismContainerValueImpl<>(prismContext);
	}

	@Override
	public <V extends PrismValue,D extends ItemDefinition> Item<V,D> createDummyItem(Item<V,D> itemOld, D definition, ItemPath path) throws SchemaException {
		Item<V,D> itemMid;
		if (itemOld == null) {
			itemMid = definition.instantiate();
		} else {
			itemMid = itemOld.clone();
		}
		if (itemMid instanceof PrismProperty<?>) {
			return (Item<V,D>) new DummyPropertyImpl<>((PrismProperty<?>)itemMid, path);
		} else if (itemMid instanceof PrismReference) {
			return (Item<V,D>) new DummyReferenceImpl((PrismReference)itemMid, path);
		} else if (itemMid instanceof PrismContainer<?>) {
			return (Item<V,D>) new DummyContainerImpl<>((PrismContainer<?>)itemMid, path);
		} else {
			throw new IllegalStateException("Unknown type "+itemMid.getClass());
		}
	}
}

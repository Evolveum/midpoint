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

import com.evolveum.midpoint.prism.xnode.XNode;
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
	public <T> PrismProperty<T> createPrismProperty(QName itemName) {
		return new PrismPropertyImpl<>(itemName, prismContext);
	}

	@Override
	public <T> PrismProperty<T> createPrismProperty(QName itemName, PrismPropertyDefinition<T> definition) {
		return new PrismPropertyImpl<>(itemName, definition, prismContext);
	}

	@Override
	public <T> PrismPropertyValue<T> createPrismPropertyValue() {
		return new PrismPropertyValueImpl<>(null, prismContext);
	}

	@Override
	public <T> PrismPropertyValue<T> createPrismPropertyValue(T realValue) {
		return new PrismPropertyValueImpl<>(realValue, prismContext);
	}

	@Override
	public <T> PrismPropertyValue<T> createPrismPropertyValue(XNode rawValue) {
		PrismPropertyValueImpl<T> rv = new PrismPropertyValueImpl<>(null, prismContext);
		rv.setRawElement(rawValue);
		return rv;
	}

	@Override
	public <T> PrismPropertyValue<T> createPrismPropertyValue(T value, OriginType originType, Objectable originObject) {
		return new PrismPropertyValueImpl<>(value, prismContext, originType, originObject, null);
	}

	@Override
	public PrismReference createPrismReference(QName name) {
		return new PrismReferenceImpl(name, null, prismContext);
	}

	@Override
	public PrismReference createPrismReference(QName name, PrismReferenceDefinition definition) {
		return new PrismReferenceImpl(name, definition, prismContext);
	}

	@Override
	public PrismReferenceValue createPrismReferenceValue() {
		return new PrismReferenceValueImpl(null);
	}

	@Override
	public PrismReferenceValue createPrismReferenceValue(PrismObject<?> target) {
		PrismReferenceValue rv = new PrismReferenceValueImpl(target.getOid());
		rv.setPrismContext(prismContext);
		rv.setObject(target);
		if (target.getDefinition() != null) {
			rv.setTargetType(target.getDefinition().getTypeName());
		}
		return rv;
	}

	@Override
	public PrismReferenceValue createPrismReferenceValue(String targetOid) {
		PrismReferenceValue rv = new PrismReferenceValueImpl(targetOid);
		rv.setPrismContext(prismContext);
		return rv;
	}

	@Override
	public PrismReferenceValue createPrismReferenceValue(String oid, OriginType originType, Objectable originObject) {
		PrismReferenceValue rv = new PrismReferenceValueImpl(oid, originType, originObject);
		rv.setPrismContext(prismContext);
		return rv;
	}

	@Override
	public PrismReferenceValue createPrismReferenceValue(String oid, QName targetType) {
		PrismReferenceValue rv = new PrismReferenceValueImpl(oid, targetType);
		rv.setPrismContext(prismContext);
		return rv;
	}

	@Override
	public PrismValue createPrismValue(Object realValue) {
		if (realValue instanceof Containerable) {
			return ((Containerable) realValue).asPrismContainerValue();
		} else if (realValue instanceof Referencable) {
			return ((Referencable) realValue).asReferenceValue();
		} else {
			return createPrismPropertyValue(realValue);
		}
	}

	@Override
	public PrismContainer createPrismContainer(QName name) {
		return new PrismContainerImpl(name, prismContext);
	}

	@Override
	public <C extends Containerable> PrismContainer<C> createPrismContainer(QName name, PrismContainerDefinition<C> definition) {
		return new PrismContainerImpl<>(name, definition, prismContext);
	}

	@Override
	public <O extends Objectable> PrismObject<O> createPrismObject(QName name, PrismObjectDefinition<O> definition) {
		return new PrismObjectImpl<>(name, definition, prismContext);
	}

	@Override
	public <O extends Objectable> PrismObjectValue<O> createPrismObjectValue(O objectable) {
		return new PrismObjectValueImpl<>(objectable, prismContext);
	}

	@Override
	public <C extends Containerable> PrismContainerValue<C> createPrismContainerValue(C containerable) {
		return new PrismContainerValueImpl<>(containerable, prismContext);
	}

	@Override
	public <C extends Containerable> PrismContainerValue<C> createPrismContainerValue() {
		return new PrismContainerValueImpl<>(prismContext);
	}
}

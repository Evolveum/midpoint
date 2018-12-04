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
}

/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.prism.marshaller;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public class PrismSerializerImpl<T> implements PrismSerializer<T> {

	@NotNull private final SerializerTarget<T> target;
	private final QName itemName;
	private final ItemDefinition itemDefinition;
	private final SerializationContext context;

	//region Setting up =============================================================================================
	public PrismSerializerImpl(@NotNull SerializerTarget<T> target, QName itemName, ItemDefinition itemDefinition,
			SerializationContext context) {
		this.target = target;
		this.itemName = itemName;
		this.itemDefinition = itemDefinition;
		this.context = context;
	}

	@NotNull
	@Override
	public PrismSerializerImpl<T> context(SerializationContext context) {
		return new PrismSerializerImpl<>(this.target, itemName, itemDefinition, context);
	}

	@NotNull
	@Override
	public PrismSerializerImpl<T> root(QName elementName) {
		return new PrismSerializerImpl<>(this.target, elementName, itemDefinition, this.context);
	}

	@NotNull
	@Override
	public PrismSerializer<T> definition(ItemDefinition itemDefinition) {
		return new PrismSerializerImpl<>(this.target, itemName, itemDefinition, this.context);
	}

	@NotNull
	@Override
	public PrismSerializerImpl<T> options(SerializationOptions options) {
		SerializationContext context;
		if (this.context != null) {
			context = this.context.clone();
			context.setOptions(options);
		} else {
			context = new SerializationContext(options);
		}
		return new PrismSerializerImpl<>(target, itemName, itemDefinition, context);
	}
	//endregion

	//region Serialization =============================================================================================

	@NotNull
	@Override
	public T serialize(@NotNull Item<?, ?> item) throws SchemaException {
		RootXNode xroot = getMarshaller().marshalItemAsRoot(item, itemName, itemDefinition, context);
		return target.write(xroot, context);
	}

	@NotNull
	@Override
	public T serialize(@NotNull PrismValue value) throws SchemaException {
		QName nameToUse;
		if (itemName != null) {
			nameToUse = itemName;
		} else if (itemDefinition != null) {
			nameToUse = itemDefinition.getName();
		} else if (value.getParent() != null) {
			nameToUse = value.getParent().getElementName();
		} else {
			// TODO derive from the value type itself? Not worth the effort.
			throw new IllegalArgumentException("Item name nor definition is not known for " + value);
		}
		return serialize(value, nameToUse);
	}

	@NotNull
	@Override
	public T serialize(@NotNull PrismValue value, QName itemName) throws SchemaException {
		RootXNode xroot = getMarshaller().marshalPrismValueAsRoot(value, itemName, itemDefinition, context);
		return target.write(xroot, context);
	}

	@NotNull
	@Override
	public T serialize(@NotNull RootXNode xnode) throws SchemaException {
		return target.write(xnode, context);
	}

	@Override
	public T serializeRealValue(Object value) throws SchemaException {
		return serializeRealValue(value, itemName);
	}

	@Override
	public T serializeRealValue(Object realValue, QName itemName) throws SchemaException {
		PrismValue prismValue;
		if (realValue instanceof Containerable) {
			prismValue = ((Containerable) realValue).asPrismContainerValue();
		} else {
			prismValue = new PrismPropertyValue<>(realValue);
		}
		return serialize(prismValue, itemName);
	}

	@Override
	public T serialize(JAXBElement<?> value) throws SchemaException {
		return serializeRealValue(value.getValue(), value.getName());		// TODO declared type?
	}

	@Override
	public T serializeAnyData(Object value) throws SchemaException {
		return serializeAnyData(value, itemName);
	}

	@Override
	public T serializeAnyData(Object value, QName itemName) throws SchemaException {
		RootXNode xnode = getMarshaller().marshalAnyData(value, itemName, itemDefinition, context);
		return target.write(xnode, context);
	}

	@NotNull
	private PrismMarshaller getMarshaller() {
		return target.prismContext.getPrismMarshaller();
	}
	//endregion

}

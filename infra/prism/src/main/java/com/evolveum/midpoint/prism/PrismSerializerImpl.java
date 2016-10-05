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

package com.evolveum.midpoint.prism;

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
	private final QName elementName;
	private final SerializationContext context;

	public PrismSerializerImpl(@NotNull SerializerTarget<T> target, QName elementName, SerializationContext context) {
		this.target = target;
		this.elementName = elementName;
		this.context = context;
	}

	@Override
	public PrismSerializerImpl<T> context(SerializationContext context) {
		return new PrismSerializerImpl<>(this.target, elementName, context);
	}

	@Override
	public PrismSerializerImpl<T> root(QName elementName) {
		return new PrismSerializerImpl<>(this.target, elementName, this.context);
	}

	@Override
	public PrismSerializerImpl<T> options(SerializationOptions options) {
		SerializationContext context;
		if (this.context != null) {
			context = this.context.clone();
			context.setOptions(options);
		} else {
			context = new SerializationContext(options);
		}
		return new PrismSerializerImpl<>(this.target, this.elementName, context);
	}

	@Override
	public <O extends Objectable> T serialize(PrismObject<O> object) throws SchemaException {
		RootXNode xroot = target.parserHelpers.xnodeProcessor.serializeObject(object, false, context);			// TODO serialize composite objects?
		if (elementName != null) {
			xroot.setRootElementName(elementName);		// TODO what about the type?
		}
		return target.serialize(xroot, context);
	}

	@Override
	public T serialize(PrismValue value) throws SchemaException {
		return serialize(value, elementName);
	}

	@Override
	public T serialize(PrismValue value, QName rootElementName) throws SchemaException {
		RootXNode xroot = target.parserHelpers.xnodeProcessor.serializeItemValueAsRoot(value, elementName);	// TODO context
		return target.serialize(xroot, context);
	}

	@Override
	public T serialize(RootXNode xnode) throws SchemaException {
		return target.serialize(xnode, context);
	}

	@Override
	public T serializeAtomicValue(Object value) throws SchemaException {
		return serializeAtomicValue(value, elementName);
	}

	@Override
	public T serializeAtomicValue(Object value, QName rootElementName) throws SchemaException {
		RootXNode xnode = target.parserHelpers.xnodeProcessor.serializeAtomicValue(value, rootElementName, context);
		return target.serialize(xnode, context);
	}

	@Override
	public T serializeAtomicValue(JAXBElement<?> value) throws SchemaException {
		RootXNode xnode = target.parserHelpers.xnodeProcessor.serializeAtomicValue(value);	// TODO context
		return target.serialize(xnode, context);
	}

	@Override
	public T serializeAnyData(Object value) throws SchemaException {
		return serializeAnyData(value, elementName);
	}

	@Override
	public T serializeAnyData(Object value, QName rootName) throws SchemaException {
		RootXNode xnode = target.parserHelpers.xnodeProcessor.serializeAnyData(value, rootName, context);
		return target.serialize(xnode, context);
	}


}

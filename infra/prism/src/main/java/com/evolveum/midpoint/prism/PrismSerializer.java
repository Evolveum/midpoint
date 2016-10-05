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
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface PrismSerializer<T> {

	PrismSerializer<T> context(SerializationContext context);
	PrismSerializer<T> root(QName elementName);
	PrismSerializer<T> options(SerializationOptions options);

	<O extends Objectable> T serialize(PrismObject<O> object) throws SchemaException;
	<C extends Containerable> T serialize(PrismValue value) throws SchemaException;
	T serialize(PrismValue value, QName rootName) throws SchemaException;

	@Deprecated
	T serialize(RootXNode xnode) throws SchemaException;

	/**
	 * Serializes an atomic value - i.e. something that fits into a prism property (if such a property would exist).
	 *
	 * value Value to be serialized.
	 * elementName Element name to be used.
	 *
	 * BEWARE, currently works only for values that can be processed via PrismBeanConvertor - i.e. not for special
	 * cases like PolyStringType, ProtectedStringType, etc.
	 */

	T serializeAtomicValue(JAXBElement<?> value) throws SchemaException;
	T serializeAtomicValue(Object value) throws SchemaException;
	T serializeAtomicValue(Object value, QName rootName) throws SchemaException;
	T serializeAnyData(Object value) throws SchemaException;
	T serializeAnyData(Object value, QName rootName) throws SchemaException;
}

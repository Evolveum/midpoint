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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * Takes care of serializing prism objects and other beans, i.e. converts java form to
 * lexical representation (XML/JSON/YAML strings, DOM tree) or intermediate one (XNode).
 *
 * General post-conditions:
 * 1. All type QNames will be resolvable (i.e. they will be part of static schema) - TODO think again about this; think also about allowing 'system-wide' parts of dynamic schema ...
 * 2. If root(..) is configured, it will be set regardless of the object type and content.
 * 3. ... TODO ...
 *
 * @author mederly
 */
public interface PrismSerializer<T> {

	/**
	 * Sets the name of the root element. Can be done either here or during call to serialize(..) methods.
	 *
	 * @param elementName Name of the root element
	 * @return Serializer with the root element name set.
	 */
	@NotNull
	PrismSerializer<T> root(QName elementName);

	/**
	 * Sets the item definition to be used during serialization.
	 * (Not much used.)
	 * @param itemDefinition
	 * @return
	 */
	@NotNull
	PrismSerializer<T> definition(ItemDefinition itemDefinition);

	/**
	 * Sets the context for the serialization operation, containing e.g. serialization options.
	 *
	 * @param context Context to be set.
	 * @return Serializer with the context set.
	 */
	@NotNull
	PrismSerializer<T> context(@Nullable SerializationContext context);

	/**
	 * Sets the serialization options (part of the context).
	 *
	 * @param options Options to be set.
	 * @return Serializer with the options set.
	 */
	@NotNull
	PrismSerializer<T> options(@Nullable SerializationOptions options);

	/**
	 * Serializes given prism item.
	 *
	 * @param item Item to be serialized.
	 * @return String/RootXNode representation of the item.
	 */
	@NotNull
	T serialize(@NotNull Item<?, ?> item) throws SchemaException;

	/**
	 * Serializes given prism value (property, reference, or container).
	 * Name of the root element is derived in the following way:
	 * 1. if explicit name is set (
	 * @param value Value to be serialized.
	 * @return String/RootXNode representation of the value.
	 */
	@NotNull
	T serialize(@NotNull PrismValue value) throws SchemaException;

	/**
	 * Serializes given prism value (property, reference, or container).
	 * @param value Value to be serialized.
	 * @param rootName Name of the root element. (Overrides other means of deriving the name.)
	 * @return String/RootXNode representation of the value.
	 */
	@NotNull
	T serialize(@NotNull PrismValue value, QName rootName) throws SchemaException;

	@NotNull
	T serialize(@NotNull RootXNode xnode) throws SchemaException;

	T serialize(JAXBElement<?> value) throws SchemaException;
	T serializeRealValue(Object value) throws SchemaException;
	T serializeRealValue(Object value, QName rootName) throws SchemaException;
	T serializeAnyData(Object value) throws SchemaException;
	T serializeAnyData(Object value, QName rootName) throws SchemaException;
}

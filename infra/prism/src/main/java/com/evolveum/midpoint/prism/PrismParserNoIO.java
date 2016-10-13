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

import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.List;

/**
 * The same as PrismParser but has no IOException on parseXYZ methods. It is used when parsing from strings or DOM structures
 * where no IOExceptions occur.
 *
 * For methods' descriptions please see the parent interface (PrismParser).
 *
 * @author mederly
 */
public interface PrismParserNoIO extends PrismParser {

	@NotNull
	PrismParserNoIO language(@Nullable String language);
	@NotNull
	PrismParserNoIO xml();
	@NotNull
	PrismParserNoIO json();
	@NotNull
	PrismParserNoIO yaml();
	@NotNull
	PrismParserNoIO context(@NotNull ParsingContext context);
	@NotNull
	PrismParserNoIO strict();
	@NotNull
	PrismParserNoIO compat();

	@NotNull
	<O extends Objectable> PrismObject<O> parse() throws SchemaException;
	@NotNull
	List<PrismObject<? extends Objectable>> parseObjects() throws SchemaException;
	@NotNull
	<C extends Containerable> PrismContainer<C> parseContainer(@NotNull Class<C> clazz) throws SchemaException;
	@NotNull
	<C extends Containerable> PrismContainer<C> parseContainer(@NotNull PrismContainerDefinition<C> definition) throws SchemaException;
	<T> T parseAtomicValue(QName typeName) throws SchemaException;
	Object parseAnyData() throws SchemaException;
	<T> T parseAnyValue() throws SchemaException;
	<T> JAXBElement<T> parseAnyValueAsJAXBElement() throws SchemaException;

	XNode parseToXNode() throws SchemaException;
}

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
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
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
	PrismParserNoIO definition(ItemDefinition<?> itemDefinition);
	@NotNull
	PrismParserNoIO name(QName itemName);
	@NotNull
	PrismParserNoIO type(QName typeName);
	@NotNull
	PrismParserNoIO type(Class<?> typeClass);

	@NotNull
	<O extends Objectable> PrismObject<O> parse() throws SchemaException;
	<IV extends PrismValue, ID extends ItemDefinition> Item<IV,ID> parseItem() throws SchemaException;
	<IV extends PrismValue> IV parseItemValue() throws SchemaException;
	<T> T parseRealValue(Class<T> clazz) throws SchemaException;
	<T> T parseRealValue() throws SchemaException;
	<T> JAXBElement<T> parseRealValueToJaxbElement() throws SchemaException;
	RootXNode parseToXNode() throws SchemaException;
	Object parseItemOrRealValue() throws SchemaException;

	// auxiliary methods
	@NotNull
	List<PrismObject<? extends Objectable>> parseObjects() throws SchemaException;

}

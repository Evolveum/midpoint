/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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

	void parseObjectsIteratively(@NotNull ObjectHandler handler) throws SchemaException;
}

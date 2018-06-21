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

package com.evolveum.midpoint.prism.marshaller;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.List;

/**
 * @author mederly
 */
public class PrismParserImplIO extends PrismParserImpl {

	public PrismParserImplIO(ParserSource source, String language, ParsingContext context, PrismContextImpl prismContext,
			ItemDefinition<?> itemDefinition, QName itemName, QName typeName, Class<?> typeClass) {
		super(source, language, context, prismContext, itemDefinition, itemName, typeName, typeClass);
	}

	@NotNull
	@Override
	public <O extends Objectable> PrismObject<O> parse() throws SchemaException, IOException {
		return doParse();
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition> Item<IV, ID> parseItem() throws SchemaException, IOException {
		return doParseItem();
	}

	@Override
	public <IV extends PrismValue> IV parseItemValue() throws SchemaException, IOException {
		return doParseItemValue();
	}

	@Override
	public <T> T parseRealValue(Class<T> clazz) throws IOException, SchemaException {
		return doParseRealValue(clazz);
	}

	@Override
	public <T> T parseRealValue() throws IOException, SchemaException {
		return doParseRealValue();
	}

	@Override
	public <T> JAXBElement<T> parseRealValueToJaxbElement() throws IOException, SchemaException {
		return doParseAnyValueAsJAXBElement();
	}

	@Override
	public RootXNode parseToXNode() throws IOException, SchemaException {
		return doParseToXNode();
	}

	@NotNull
	@Override
	public List<PrismObject<? extends Objectable>> parseObjects() throws SchemaException, IOException {
		return doParseObjects();
	}

	@Override
	public void parseObjectsIteratively(@NotNull ObjectHandler handler) throws SchemaException, IOException {
		doParseObjectsIteratively(handler);
	}

	@Override
	public Object parseItemOrRealValue() throws IOException, SchemaException {
		return doParseItemOrRealValue();
	}
}

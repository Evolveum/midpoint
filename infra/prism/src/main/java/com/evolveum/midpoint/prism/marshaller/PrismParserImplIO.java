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
import com.evolveum.midpoint.prism.lex.LexicalHelpers;
import com.evolveum.midpoint.prism.xnode.XNode;
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

	public PrismParserImplIO(ParserSource source, String language, ParsingContext context, LexicalHelpers helpers) {
		super(source, language, context, helpers);
	}

	@NotNull
	@Override
	public <O extends Objectable> PrismObject<O> parse() throws SchemaException, IOException {
		return doParse();
	}

	@NotNull
	@Override
	public List<PrismObject<? extends Objectable>> parseObjects() throws SchemaException, IOException {
		return doParseObjects();
	}

	@NotNull
	@Override
	public <C extends Containerable> PrismContainer<C> parseContainer(@NotNull Class<C> clazz) throws SchemaException, IOException {
		return doParseContainer(clazz);
	}

	@NotNull
	@Override
	public <C extends Containerable> PrismContainer<C> parseContainer(@NotNull PrismContainerDefinition<C> definition) throws SchemaException, IOException {
		return doParseContainer(definition);
	}

	@Override
	public <T> T parseAtomicValue(QName typeName) throws IOException, SchemaException {
		return doParseAtomicValue(typeName);
	}

	@Override
	public Object parseAnyData() throws IOException, SchemaException {
		return doParseAnyData();
	}

	@Override
	public <T> T parseAnyValue() throws IOException, SchemaException {
		return doParseAnyValue();
	}

	@Override
	public <T> JAXBElement<T> parseAnyValueAsJAXBElement() throws IOException, SchemaException {
		return doParseAnyValueAsJAXBElement();
	}

	@Override
	public XNode parseToXNode() throws IOException, SchemaException {
		return doParseToXNode();
	}
}

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
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.List;

/**
 * @author mederly
 */
public class PrismParserImplNoIO extends PrismParserImpl implements PrismParserNoIO {

	public PrismParserImplNoIO(ParserSource source, String language, ParsingContext context,
			LexicalHelpers helpers) {
		super(source, language, context, helpers);
	}

	@NotNull
	@Override
	public PrismParserNoIO language(@Nullable String language) {
		return (PrismParserNoIO) super.language(language);
	}

	@NotNull
	@Override
	public PrismParserNoIO xml() {
		return (PrismParserNoIO) super.xml();
	}

	@NotNull
	@Override
	public PrismParserNoIO json() {
		return (PrismParserNoIO) super.json();
	}

	@NotNull
	@Override
	public PrismParserNoIO yaml() {
		return (PrismParserNoIO) super.yaml();
	}

	@NotNull
	@Override
	public PrismParserNoIO context(@NotNull ParsingContext context) {
		return (PrismParserNoIO) super.context(context);
	}

	@NotNull
	@Override
	public PrismParserNoIO strict() {
		return (PrismParserNoIO) super.strict();
	}

	@NotNull
	@Override
	public PrismParserNoIO compat() {
		return (PrismParserNoIO) super.compat();
	}

	@NotNull
	@Override
	public <O extends Objectable> PrismObject<O> parse() throws SchemaException {
		try {
			return doParse();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@NotNull
	@Override
	public List<PrismObject<? extends Objectable>> parseObjects() throws SchemaException {
		try {
			return doParseObjects();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@NotNull
	@Override
	public <C extends Containerable> PrismContainer<C> parseContainer(@NotNull Class<C> clazz) throws SchemaException {
		try {
			return doParseContainer(clazz);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@NotNull
	@Override
	public <C extends Containerable> PrismContainer<C> parseContainer(@NotNull PrismContainerDefinition<C> definition)
			throws SchemaException {
		try {
			return doParseContainer(definition);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public <T> T parseAtomicValue(QName typeName) throws SchemaException {
		try {
			return doParseAtomicValue(typeName);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Object parseAnyData() throws SchemaException {
		try {
			return doParseAnyData();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public <T> T parseAnyValue() throws SchemaException {
		try {
			return doParseAnyValue();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public <T> JAXBElement<T> parseAnyValueAsJAXBElement() throws SchemaException {
		try {
			return doParseAnyValueAsJAXBElement();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public XNode parseToXNode() throws SchemaException {
		try {
			return doParseToXNode();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}

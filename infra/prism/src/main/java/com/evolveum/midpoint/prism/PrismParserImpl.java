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

import com.evolveum.midpoint.prism.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.lex.LexicalHelpers;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
public abstract class PrismParserImpl implements PrismParser {

	@NotNull private final ParserSource source;
	private final String language;
	@NotNull private final ParsingContext context;
	@NotNull private final LexicalHelpers helpers;

	public PrismParserImpl(@NotNull ParserSource source, String language, @NotNull ParsingContext context, @NotNull LexicalHelpers helpers) {
		this.source = source;
		this.language = language;
		this.context = context;
		this.helpers = helpers;
	}

	private PrismParser create(ParserSource source, String language, @NotNull ParsingContext context, LexicalHelpers helpers) {
		return source.isIO() ?
				new PrismParserImplIO(source, language, context, helpers) :
				new PrismParserImplNoIO(source, language, context, helpers);
	}

	@Override
	public PrismParser language(String language) {
		return create(this.source, language, this.context, this.helpers);
	}

	@Override
	public PrismParser xml() {
		return language(PrismContext.LANG_XML);
	}

	@Override
	public PrismParser json() {
		return language(PrismContext.LANG_JSON);
	}

	@Override
	public PrismParser yaml() {
		return language(PrismContext.LANG_YAML);
	}

	@Override
	public PrismParser context(@NotNull ParsingContext context) {
		return create(this.source, this.language, context, this.helpers);
	}

	@Override
	public PrismParser strict() {
		return create(this.source, this.language, context.clone().strict(), this.helpers);
	}

	@Override
	public PrismParser compat() {
		return create(this.source, this.language, context.clone().compat(), this.helpers);
	}

	protected <O extends Objectable> PrismObject<O> doParse() throws SchemaException, IOException {
		LexicalProcessor lexicalProcessor = getParser();
		XNode xnode = lexicalProcessor.parse(source, context);
		return helpers.xnodeProcessor.parseObject(xnode, context);
	}

	private LexicalProcessor getParser() throws IOException {
		LexicalProcessor lexicalProcessor;
		if (language != null) {
			lexicalProcessor = helpers.lexicalProcessorRegistry.parserFor(language);
		} else {
			lexicalProcessor = helpers.lexicalProcessorRegistry.findParser(source);
		}
		return lexicalProcessor;
	}

	protected List<PrismObject<? extends Objectable>> doParseObjects() throws IOException, SchemaException {
		LexicalProcessor lexicalProcessor = getParser();
		Collection<XNode> xnodes = lexicalProcessor.parseCollection(source, context);
		List<PrismObject<? extends Objectable>> objects = new ArrayList<>();
		for (XNode xnode : xnodes) {
			PrismObject<? extends Objectable> object = helpers.xnodeProcessor.parseObject(xnode, context);
			objects.add(object);
		}
		return objects;
	}

	protected <C extends Containerable> PrismContainer<C> doParseContainer(Class<C> clazz) throws SchemaException, IOException {
		LexicalProcessor lexicalProcessor = getParser();
		XNode xnode = lexicalProcessor.parse(source, context);
		return helpers.xnodeProcessor.parseContainer(xnode, clazz, context);
	}

	protected <C extends Containerable> PrismContainer<C> doParseContainer(PrismContainerDefinition<C> definition) throws SchemaException, IOException {
		LexicalProcessor lexicalProcessor = getParser();
		XNode xnode = lexicalProcessor.parse(source, context);
		return helpers.xnodeProcessor.parseContainer(xnode, definition, context);
	}

	protected <T> T doParseAtomicValue(QName typeName) throws IOException, SchemaException {
		LexicalProcessor lexicalProcessor = getParser();
		XNode xnode = lexicalProcessor.parse(source, context);
		return helpers.xnodeProcessor.parseAtomicValue(xnode, typeName, context);
	}

	protected Object doParseAnyData() throws IOException, SchemaException {
		LexicalProcessor lexicalProcessor = getParser();
		XNode xnode = lexicalProcessor.parse(source, context);
		return helpers.xnodeProcessor.parseAnyData(xnode, context);
	}

	protected <T> T doParseAnyValue() throws IOException, SchemaException {
		LexicalProcessor lexicalProcessor = getParser();
		XNode xnode = lexicalProcessor.parse(source, context);
		return helpers.xnodeProcessor.parseAnyValue(xnode, context);
	}

	protected <T> JAXBElement<T> doParseAnyValueAsJAXBElement() throws IOException, SchemaException {
		LexicalProcessor lexicalProcessor = getParser();
		XNode xnode = lexicalProcessor.parse(source, context);
		return helpers.xnodeProcessor.parseAnyValueAsJAXBElement(xnode, context);
	}

	protected XNode doParseToXNode() throws IOException, SchemaException {
		return getParser().parse(source, context);
	}
}

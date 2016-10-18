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
import com.evolveum.midpoint.prism.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
abstract class PrismParserImpl implements PrismParser {

	@NotNull private final ParserSource source;
	private final String language;
	@NotNull private final ParsingContext context;
	@NotNull private final PrismContextImpl prismContext;

	private final ItemDefinition<?> itemDefinition;
	private final QName itemName;
	private final QName typeName;
	private final Class<?> typeClass;

	//region Parameters ====================================================================================

	PrismParserImpl(@NotNull ParserSource source, String language, @NotNull ParsingContext context,
			@NotNull PrismContextImpl prismContext, ItemDefinition<?> itemDefinition, QName itemName, QName typeName, Class<?> typeClass) {
		this.source = source;
		this.language = language;
		this.context = context;
		this.prismContext = prismContext;
		this.itemDefinition = itemDefinition;
		this.itemName = itemName;
		this.typeName = typeName;
		this.typeClass = typeClass;
	}

	private PrismParser create(ParserSource source, @Nullable String language, @NotNull ParsingContext context, PrismContextImpl prismContext,
			ItemDefinition<?> itemDefinition, QName itemName, QName typeName, Class<?> typeClass) {
		return source.throwsIOException() ?
				new PrismParserImplIO(source, language, context, prismContext, itemDefinition, itemName, typeName, typeClass) :
				new PrismParserImplNoIO(source, language, context, prismContext, itemDefinition, itemName, typeName, typeClass);
	}

	@NotNull
	@Override
	public PrismParser language(@Nullable String language) {
		return create(source, language, context, prismContext, itemDefinition, itemName, typeName, typeClass);
	}

	@NotNull
	@Override
	public PrismParser xml() {
		return language(PrismContext.LANG_XML);
	}

	@NotNull
	@Override
	public PrismParser json() {
		return language(PrismContext.LANG_JSON);
	}

	@NotNull
	@Override
	public PrismParser yaml() {
		return language(PrismContext.LANG_YAML);
	}

	@NotNull
	@Override
	public PrismParser context(@NotNull ParsingContext context) {
		return create(source, language, context, prismContext, itemDefinition, itemName, typeName, typeClass);
	}

	@NotNull
	@Override
	public PrismParser strict() {
		return create(source, language, context.clone().strict(), prismContext, itemDefinition, itemName, typeName, typeClass);
	}

	@NotNull
	@Override
	public PrismParser compat() {
		return create(source, language, context.clone().compat(), prismContext, itemDefinition, itemName, typeName, typeClass);
	}

	@NotNull
	@Override
	public PrismParser definition(ItemDefinition<?> itemDefinition) {
		return create(source, language, context, prismContext, itemDefinition, itemName, typeName, typeClass);
	}

	@NotNull
	@Override
	public PrismParser name(QName itemName) {
		return create(source, language, context, prismContext, itemDefinition, itemName, typeName, typeClass);
	}

	@NotNull
	@Override
	public PrismParser type(QName typeName) {
		return create(source, language, context, prismContext, itemDefinition, itemName, typeName, typeClass);
	}

	@NotNull
	@Override
	public PrismParser type(Class<?> typeClass) {
		return create(source, language, context, prismContext, itemDefinition, itemName, typeName, typeClass);
	}
	//endregion

	//region Parsing methods ====================================================================================

	@NotNull
	<O extends Objectable> PrismObject<O> doParse() throws SchemaException, IOException {
		RootXNode xnode = getLexicalProcessor().read(source, context);
		return prismContext.getPrismUnmarshaller().parseObject(xnode, itemDefinition, itemName, typeName, typeClass, context);
	}

	<IV extends PrismValue, ID extends ItemDefinition> Item<IV, ID> doParseItem() throws IOException, SchemaException {
		RootXNode xnode = getLexicalProcessor().read(source, context);
		return doParseItem(xnode);
	}

	private <IV extends PrismValue, ID extends ItemDefinition> Item<IV, ID> doParseItem(RootXNode xnode) throws IOException, SchemaException {
		return (Item) prismContext.getPrismUnmarshaller().parseItem(xnode, itemDefinition, itemName, typeName, typeClass, context);
	}

	<IV extends PrismValue> IV doParseItemValue() throws IOException, SchemaException {
		RootXNode root = getLexicalProcessor().read(source, context);
		return doParseItemValue(root);
	}

	private <IV extends PrismValue> IV doParseItemValue(RootXNode root) throws IOException, SchemaException {
		Item<IV,?> item = doParseItem(root);
		return getSingleValue(item);
	}

	@Nullable
	private <IV extends PrismValue> IV getSingleValue(Item<IV, ?> item) {
		if (item.isEmpty()) {
			return null;
		} else if (item.size() == 1) {
			return item.getValues().get(0);
		} else {
			throw new IllegalStateException("Expected one item value, got " + item.getValues().size()
					+ " while parsing " + item);
		}
	}

	<T> T doParseRealValue(Class<T> clazz) throws IOException, SchemaException {
		RootXNode root = getLexicalProcessor().read(source, context);
		return doParseRealValue(clazz, root);
	}

	@SuppressWarnings("unchecked")
	private <T> T doParseRealValue(Class<T> clazz, RootXNode root) throws IOException, SchemaException {
		if (clazz == null) {
			ItemInfo info = ItemInfo.determine(itemDefinition, root.getRootElementName(), itemName, null,
					root.getTypeQName(), typeName, null, ItemDefinition.class, context, prismContext.getSchemaRegistry());
			if (info.getItemDefinition() instanceof PrismContainerDefinition) {
				clazz = ((PrismContainerDefinition) info.getItemDefinition()).getCompileTimeClass();
			}
			if (clazz == null && info.getTypeName() != null) {
				clazz = (Class) prismContext.getSchemaRegistry().determineClassForType(info.getTypeName());
			}
			if (clazz == null) {
				throw new IllegalArgumentException("Couldn't determine type for " + root);
			}
		}

		if (Containerable.class.isAssignableFrom(clazz)) {
			PrismValue prismValue = doParseItemValue(root);
			if (prismValue == null) {
				return null;
			} else if (prismValue instanceof PrismPropertyValue) {
				return (T) ((PrismPropertyValue) prismValue).getValue();
			} else if (prismValue instanceof PrismContainerValue) {
				return (T) ((PrismContainerValue) prismValue).asContainerable();
			} else if (prismValue instanceof PrismReferenceValue) {
				return (T) prismValue;			// TODO ok?
			} else {
				throw new IllegalStateException("Unsupported value: " + prismValue.getClass());
			}
		} else {
			return prismContext.getBeanConverter().unmarshall(root, clazz, context);
		}
	}

	@SuppressWarnings("unchecked")
	<T> T doParseRealValue() throws IOException, SchemaException {
		return (T) doParseRealValue(typeClass);
	}

	@SuppressWarnings("unchecked")
	<T> JAXBElement<T> doParseAnyValueAsJAXBElement() throws IOException, SchemaException {
		RootXNode root = getLexicalProcessor().read(source, context);
		T real = (T) doParseRealValue(Object.class, root);
		return real != null ?
				new JAXBElement<>(root.getRootElementName(), (Class<T>) real.getClass(), real) :
				null;
	}

	RootXNode doParseToXNode() throws IOException, SchemaException {
		return getLexicalProcessor().read(source, context);
	}

	@NotNull
	List<PrismObject<? extends Objectable>> doParseObjects() throws IOException, SchemaException {
		List<RootXNode> xnodes = getLexicalProcessor().readObjects(source, context);
		List<PrismObject<? extends Objectable>> objects = new ArrayList<>();
		for (RootXNode xnode : xnodes) {
			PrismObject<? extends Objectable> object = prismContext.getPrismUnmarshaller()
					.parseObject(xnode, null, null, null, PrismObjectDefinition.class, context);
			objects.add(object);
		}
		return objects;
	}

	@Deprecated
	Object doParseAnyData() throws IOException, SchemaException {
		RootXNode xnode = getLexicalProcessor().read(source, context);
		return prismContext.getPrismUnmarshaller().parseAnyData(xnode, context);
	}

	@NotNull
	private LexicalProcessor<?> getLexicalProcessor() throws IOException {
		if (language != null) {
			return prismContext.getLexicalProcessorRegistry().processorFor(language);
		} else {
			return prismContext.getLexicalProcessorRegistry().findProcessor(source);
		}
	}
	//endregion
}

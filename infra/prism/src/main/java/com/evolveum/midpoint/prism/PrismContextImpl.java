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

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.marshaller.*;
import com.evolveum.midpoint.prism.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.lex.LexicalHelpers;
import com.evolveum.midpoint.prism.lex.LexicalProcessorRegistry;
import com.evolveum.midpoint.prism.lex.dom.DomLexicalProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismMonitor;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author semancik
 *
 */
public class PrismContextImpl implements PrismContext {

	private static final Trace LOGGER = TraceManager.getTrace(PrismContextImpl.class);
    
    private static boolean allowSchemalessSerialization = true;
    
	private SchemaRegistry schemaRegistry;
	private XNodeProcessor xnodeProcessor;
	private PrismBeanConverter beanConverter;
	private SchemaDefinitionFactory definitionFactory;
	private PolyStringNormalizer defaultPolyStringNormalizer;
	private LexicalProcessorRegistry lexicalProcessorRegistry;
	private LexicalHelpers lexicalHelpers;
	private PrismMonitor monitor = null;
	
	@Autowired
	private Protector defaultProtector;
	
	// We need to keep this because of deprecated methods and various hacks
	private JaxbDomHack jaxbDomHack;

    //region Standard overhead
	private PrismContextImpl() {
		// empty
	}

	public static PrismContextImpl create(SchemaRegistry schemaRegistry) {
		PrismContextImpl prismContext = new PrismContextImpl();
		prismContext.schemaRegistry = schemaRegistry;
		schemaRegistry.setPrismContext(prismContext);

		prismContext.xnodeProcessor = new XNodeProcessor(prismContext);
		PrismBeanInspector inspector = new PrismBeanInspector(prismContext);
		prismContext.beanConverter = new PrismBeanConverter(prismContext, inspector);

		prismContext.lexicalProcessorRegistry = new LexicalProcessorRegistry(schemaRegistry);
		prismContext.jaxbDomHack = new JaxbDomHack((DomLexicalProcessor) prismContext.lexicalProcessorRegistry.parserFor(PrismContext.LANG_XML), prismContext);

		prismContext.lexicalHelpers = new LexicalHelpers(prismContext.lexicalProcessorRegistry, prismContext.xnodeProcessor, prismContext.beanConverter);
		
		return prismContext;
	}
	
	public static PrismContextImpl createEmptyContext(SchemaRegistry schemaRegistry){
		PrismContextImpl prismContext = new PrismContextImpl();
		prismContext.schemaRegistry = schemaRegistry;
		schemaRegistry.setPrismContext(prismContext);

		return prismContext;
	}

	@Override
	public void initialize() throws SchemaException, SAXException, IOException {
		schemaRegistry.initialize();
		if (defaultPolyStringNormalizer == null) {
			defaultPolyStringNormalizer = new PrismDefaultPolyStringNormalizer();
		}
	}

	public static boolean isAllowSchemalessSerialization() {
		return allowSchemalessSerialization;
	}

	public static void setAllowSchemalessSerialization(boolean allowSchemalessSerialization) {
		PrismContextImpl.allowSchemalessSerialization = allowSchemalessSerialization;
	}

	@Override
	public SchemaRegistry getSchemaRegistry() {
		return schemaRegistry;
	}

	public void setSchemaRegistry(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}

	@Override
	public XNodeProcessor getXnodeProcessor() {
		return xnodeProcessor;
	}

	/**
	 * WARNING! This is not really public method. It should NOT not used outside the prism implementation.
	 */
	@Override
	public DomLexicalProcessor getParserDom() {
		return (DomLexicalProcessor) lexicalHelpers.lexicalProcessorRegistry.parserFor(LANG_XML);
	}

	@Override
	public PrismBeanConverter getBeanConverter() {
		return beanConverter;
	}

	@Override
	public JaxbDomHack getJaxbDomHack() {
		return jaxbDomHack;
	}

    @Override
	public SchemaDefinitionFactory getDefinitionFactory() {
		if (definitionFactory == null) {
			definitionFactory = new SchemaDefinitionFactory();
		}
		return definitionFactory;
	}

	public void setDefinitionFactory(SchemaDefinitionFactory definitionFactory) {
		this.definitionFactory = definitionFactory;
	}

	@Override
	public PolyStringNormalizer getDefaultPolyStringNormalizer() {
		return defaultPolyStringNormalizer;
	}

	public void setDefaultPolyStringNormalizer(PolyStringNormalizer defaultPolyStringNormalizer) {
		this.defaultPolyStringNormalizer = defaultPolyStringNormalizer;
	}

	private LexicalProcessor getParser(String language) {
		return lexicalProcessorRegistry.parserFor(language);
	}

	private LexicalProcessor getParserNotNull(String language) {
		LexicalProcessor lexicalProcessor = getParser(language);
		if (lexicalProcessor == null) {
			throw new SystemException("No parser for language '"+language+"'");
		}
		return lexicalProcessor;
	}

	@Override
	public Protector getDefaultProtector() {
		return defaultProtector;
	}
	
	public void setDefaultProtector(Protector defaultProtector) {
		this.defaultProtector = defaultProtector;
	}

    @Override
	public PrismMonitor getMonitor() {
		return monitor;
	}

	@Override
	public void setMonitor(PrismMonitor monitor) {
		this.monitor = monitor;
	}

    //endregion

	//region Parsing
	@NotNull
	@Override
	public PrismParser parserFor(@NotNull File file) {
		return new PrismParserImplIO(new ParserFileSource(file), null, ParsingContext.createDefault(), lexicalHelpers);
	}

	@NotNull
	@Override
	public PrismParser parserFor(@NotNull InputStream stream) {
		return new PrismParserImplIO(new ParserInputStreamSource(stream), null, ParsingContext.createDefault(), lexicalHelpers);
	}

	@NotNull
	@Override
	public PrismParserNoIO parserFor(@NotNull String data) {
		return new PrismParserImplNoIO(new ParserStringSource(data), null, ParsingContext.createDefault(), lexicalHelpers);
	}

	@NotNull
	@Deprecated
	@Override
	public PrismParserNoIO parserFor(@NotNull Element data) {
		return new PrismParserImplNoIO(new ParserElementSource(data), null, ParsingContext.createDefault(), lexicalHelpers);
	}

	@Deprecated
	@Override
	public <T extends Objectable> PrismObject<T> parseObject(File file) throws SchemaException, IOException {
		return parserFor(file).parse();
	}

	@Deprecated
	@Override
	public <T extends Objectable> PrismObject<T> parseObject(String dataString) throws SchemaException {
		return parserFor(dataString).parse();
	}
    //endregion

    //region adopt(...) methods
    /**
	 * Set up the specified object with prism context instance and schema definition.
	 */
	@Override
	public <T extends Objectable> void adopt(PrismObject<T> object, Class<T> declaredType) throws SchemaException {
		object.revive(this);
		getSchemaRegistry().applyDefinition(object, declaredType, false);
	}
	
	@Override
	public <T extends Objectable> void adopt(PrismObject<T> object) throws SchemaException {
		adopt(object, object.getCompileTimeClass());
	}

	@Override
	public void adopt(Objectable objectable) throws SchemaException {
		adopt(objectable.asPrismObject(), objectable.getClass());
	}

    @Override
	public void adopt(Containerable containerable) throws SchemaException {
        containerable.asPrismContainerValue().revive(this);
    }

    @Override
	public void adopt(PrismContainerValue value) throws SchemaException {
        value.revive(this);
    }

    @Override
	public <T extends Objectable> void adopt(ObjectDelta<T> delta) throws SchemaException {
		delta.revive(this);
		getSchemaRegistry().applyDefinition(delta, delta.getObjectTypeClass(), false);
	}
	
	@Override
	public <C extends Containerable, O extends Objectable> void adopt(C containerable, Class<O> type, ItemPath path) throws SchemaException {
		PrismContainerValue<C> prismContainerValue = containerable.asPrismContainerValue();
		adopt(prismContainerValue, type, path);
	}

	@Override
	public <C extends Containerable, O extends Objectable> void adopt(PrismContainerValue<C> prismContainerValue, Class<O> type,
			ItemPath path) throws SchemaException {
		prismContainerValue.revive(this);
		getSchemaRegistry().applyDefinition(prismContainerValue, type, path, false);
	}
	
	@Override
	public <C extends Containerable, O extends Objectable> void adopt(PrismContainerValue<C> prismContainerValue, QName typeName,
			ItemPath path) throws SchemaException {
		prismContainerValue.revive(this);
		getSchemaRegistry().applyDefinition(prismContainerValue, typeName, path, false);
	}
    //endregion

    //region Serializing objects, containers, atomic values (properties)
	@Override
	public <O extends Objectable> String serializeObjectToString(PrismObject<O> object, String language) throws SchemaException {
		return serializerFor(language).serialize(object);
	}

	@NotNull
	@Override
	public PrismSerializer<String> serializerFor(@NotNull String language) {
		return new PrismSerializerImpl<>(new SerializerStringTarget(lexicalHelpers, language), null, null);
	}

	@NotNull
	@Override
	public PrismSerializer<String> xmlSerializer() {
		return serializerFor(LANG_XML);
	}

	@NotNull
	@Override
	public PrismSerializer<String> jsonSerializer() {
		return serializerFor(LANG_JSON);
	}

	@NotNull
	@Override
	public PrismSerializer<String> yamlSerializer() {
		return serializerFor(LANG_YAML);
	}

	@NotNull
	@Override
	public PrismSerializer<Element> domSerializer() {
		return new PrismSerializerImpl<>(new SerializerDomTarget(lexicalHelpers), null, null);
	}

	@NotNull
	@Override
	public PrismSerializer<XNode> xnodeSerializer() {
		return new PrismSerializerImpl<>(new SerializerXNodeTarget(lexicalHelpers), null, null);
	}

    @Override
	public boolean canSerialize(Object value) {
        return xnodeProcessor.canSerialize(value);
    }

    //endregion

    /**
     * A bit of hack: serializes any Item into a RawType.
     * Currently used for serializing script output, until a better method is devised.
     * @return
     */
    @Override
	public RawType toRawType(Item item) throws SchemaException {
        RootXNode rootXNode = xnodeProcessor.serializeItemAsRoot(item);
        return new RawType(rootXNode, this);
    }

    @NotNull
	@Override
	public <T extends Objectable> PrismObject<T> createObject(@NotNull Class<T> clazz) throws SchemaException {
        PrismObjectDefinition<T> definition = schemaRegistry.findObjectDefinitionByCompileTimeClass(clazz);
        if (definition == null) {
            throw new SchemaException("Definition for prism object holding " + clazz + " couldn't be found");
        }
        return definition.instantiate();
    }

	@NotNull
	@Override
	public <T extends Objectable> T createObjectable(@NotNull Class<T> clazz) throws SchemaException {
		return createObject(clazz).asObjectable();
	}
}

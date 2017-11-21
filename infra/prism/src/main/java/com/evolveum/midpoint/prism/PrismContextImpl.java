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

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.marshaller.*;
import com.evolveum.midpoint.prism.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.lex.LexicalProcessorRegistry;
import com.evolveum.midpoint.prism.lex.dom.DomLexicalProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.prism.util.PrismMonitor;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
	private static boolean extraValidation = false;										// TODO replace by something serious

	@NotNull private final SchemaRegistryImpl schemaRegistry;
	@NotNull private final LexicalProcessorRegistry lexicalProcessorRegistry;
	@NotNull private final PolyStringNormalizer defaultPolyStringNormalizer;			// TODO make non-final when needed
	@NotNull private final PrismUnmarshaller prismUnmarshaller;
	@NotNull private final PrismMarshaller prismMarshaller;
	@NotNull private final BeanMarshaller beanMarshaller;
	@NotNull private final BeanUnmarshaller beanUnmarshaller;
	private ParsingMigrator parsingMigrator;
	private PrismMonitor monitor = null;

	private SchemaDefinitionFactory definitionFactory;

	@Autowired		// TODO is this really applied?
	private Protector defaultProtector;

	// We need to keep this because of deprecated methods and various hacks
	@NotNull private final JaxbDomHack jaxbDomHack;

	private QName defaultRelation;

	private QName objectsElementName;

	static {
		PrismPrettyPrinter.initialize();
	}

	//region Standard overhead
	private PrismContextImpl(@NotNull SchemaRegistryImpl schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		schemaRegistry.setPrismContext(this);
		this.lexicalProcessorRegistry = new LexicalProcessorRegistry(schemaRegistry);
		this.prismUnmarshaller = new PrismUnmarshaller(this);
		PrismBeanInspector inspector = new PrismBeanInspector(this);
		this.beanMarshaller = new BeanMarshaller(this, inspector);
		this.beanUnmarshaller = new BeanUnmarshaller(this, inspector);
		this.prismMarshaller = new PrismMarshaller(beanMarshaller);
		this.jaxbDomHack = new JaxbDomHack(lexicalProcessorRegistry.domProcessor(), this);

		defaultPolyStringNormalizer = new PrismDefaultPolyStringNormalizer();
	}

	public static PrismContextImpl create(@NotNull SchemaRegistryImpl schemaRegistry) {
		return new PrismContextImpl(schemaRegistry);
	}

	public static PrismContextImpl createEmptyContext(@NotNull SchemaRegistryImpl schemaRegistry) {
		return new PrismContextImpl(schemaRegistry);
	}

	@Override
	public void initialize() throws SchemaException, SAXException, IOException {
		schemaRegistry.initialize();
	}

	public static boolean isAllowSchemalessSerialization() {
		return allowSchemalessSerialization;
	}

	public static void setAllowSchemalessSerialization(boolean allowSchemalessSerialization) {
		PrismContextImpl.allowSchemalessSerialization = allowSchemalessSerialization;
	}

	public static boolean isExtraValidation() {
		return extraValidation;
	}

	public static void setExtraValidation(boolean extraValidation) {
		PrismContextImpl.extraValidation = extraValidation;
	}

	@Override
	public XmlEntityResolver getEntityResolver() {
		return schemaRegistry.getEntityResolver();
	}

	@NotNull
	@Override
	public SchemaRegistry getSchemaRegistry() {
		return schemaRegistry;
	}

	/**
	 * WARNING! This is not really public method. It should NOT not used outside the prism implementation.
	 */
	public PrismUnmarshaller getPrismUnmarshaller() {
		return prismUnmarshaller;
	}

	@NotNull
	public PrismMarshaller getPrismMarshaller() {
		return prismMarshaller;
	}

	/**
	 * WARNING! This is not really public method. It should NOT not used outside the prism implementation.
	 */
	public DomLexicalProcessor getParserDom() {
		return lexicalProcessorRegistry.domProcessor();
	}

	/**
	 * WARNING! This is not really public method. It should NOT not used outside the prism implementation.
	 */
	@NotNull
	public BeanMarshaller getBeanMarshaller() {
		return beanMarshaller;
	}

	@NotNull
	public BeanUnmarshaller getBeanUnmarshaller() {
		return beanUnmarshaller;
	}

	@NotNull
	@Override
	public JaxbDomHack getJaxbDomHack() {
		return jaxbDomHack;
	}

	@NotNull
	public SchemaDefinitionFactory getDefinitionFactory() {
		if (definitionFactory == null) {
			definitionFactory = new SchemaDefinitionFactory();
		}
		return definitionFactory;
	}

	public void setDefinitionFactory(SchemaDefinitionFactory definitionFactory) {
		this.definitionFactory = definitionFactory;
	}

	@NotNull
	@Override
	public PolyStringNormalizer getDefaultPolyStringNormalizer() {
		return defaultPolyStringNormalizer;
	}

	private LexicalProcessor getParser(String language) {
		return lexicalProcessorRegistry.processorFor(language);
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

	@Override
	public QName getDefaultRelation() {
		return defaultRelation;
	}

	public void setDefaultRelation(QName defaultRelation) {
		this.defaultRelation = defaultRelation;
	}

	@Override
	public QName getObjectsElementName() {
		return objectsElementName;
	}

	public void setObjectsElementName(QName objectsElementName) {
		this.objectsElementName = objectsElementName;
	}

	//endregion

	//region Parsing
	@NotNull
	@Override
	public PrismParser parserFor(@NotNull File file) {
		return new PrismParserImplIO(new ParserFileSource(file), null, ParsingContext.createDefault(), this, null, null, null, null);
	}

	@NotNull
	@Override
	public PrismParser parserFor(@NotNull InputStream stream) {
		return new PrismParserImplIO(new ParserInputStreamSource(stream), null, ParsingContext.createDefault(), this, null, null, null, null);
	}

	@NotNull
	@Override
	public PrismParserNoIO parserFor(@NotNull String data) {
		return new PrismParserImplNoIO(new ParserStringSource(data), null, ParsingContext.createDefault(), this, null, null, null, null);
	}

	@NotNull
	@Override
	public PrismParserNoIO parserFor(@NotNull RootXNode xnode) {
		return new PrismParserImplNoIO(new ParserXNodeSource(xnode), null, ParsingContext.createDefault(), this, null, null, null, null);
	}

	@NotNull
	@Deprecated
	@Override
	public PrismParserNoIO parserFor(@NotNull Element data) {
		return new PrismParserImplNoIO(new ParserElementSource(data), null, ParsingContext.createDefault(), this, null, null, null, null);
	}

	@NotNull
	@Override
	public String detectLanguage(@NotNull File file) throws IOException {
		return lexicalProcessorRegistry.detectLanguage(file);
	}

	@Override
	public ParsingMigrator getParsingMigrator() {
		return parsingMigrator;
	}

	@Override
	public void setParsingMigrator(ParsingMigrator parsingMigrator) {
		this.parsingMigrator = parsingMigrator;
	}
	//endregion

    //region adopt(...) methods
    /**
	 * Set up the specified object with prism context instance and schema definition.
	 */
	@Override
	public <C extends Containerable> void adopt(PrismContainer<C> container, Class<C> declaredType) throws SchemaException {
		container.revive(this);
		getSchemaRegistry().applyDefinition(container, declaredType, false);
	}

	@Override
	public <C extends Containerable> void adopt(PrismContainer<C> container) throws SchemaException {
		adopt(container, container.getCompileTimeClass());
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
		return new PrismSerializerImpl<>(new SerializerStringTarget(this, language), null, null, null, this);
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
		return new PrismSerializerImpl<>(new SerializerDomTarget(this), null, null, null, this);
	}

	@NotNull
	@Override
	public PrismSerializer<RootXNode> xnodeSerializer() {
		return new PrismSerializerImpl<>(new SerializerXNodeTarget(this), null, null, null, this);
	}

    @Override
	public boolean canSerialize(Object value) {
        return prismMarshaller.canSerialize(value);
    }

    //endregion

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

	@NotNull
	@Override
	public <O extends Objectable> PrismObject<O> createKnownObject(@NotNull Class<O> clazz) {
		try {
			return createObject(clazz);
		} catch (SchemaException e) {
			throw new SystemException("Unexpected SchemaException while instantiating " + clazz + ": " + e.getMessage(), e);
		}
	}

	@NotNull
	@Override
	public <O extends Objectable> O createKnownObjectable(@NotNull Class<O> clazz) {
		return createKnownObject(clazz).asObjectable();
	}

	@NotNull
	public LexicalProcessorRegistry getLexicalProcessorRegistry() {
		return lexicalProcessorRegistry;
	}
}

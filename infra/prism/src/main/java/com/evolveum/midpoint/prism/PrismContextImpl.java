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
import com.evolveum.midpoint.prism.marshaller.JaxbDomHack;
import com.evolveum.midpoint.prism.marshaller.PrismBeanConverter;
import com.evolveum.midpoint.prism.marshaller.PrismBeanInspector;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessor;
import com.evolveum.midpoint.prism.parser.Parser;
import com.evolveum.midpoint.prism.parser.ParserHelpers;
import com.evolveum.midpoint.prism.parser.ParserRegistry;
import com.evolveum.midpoint.prism.parser.dom.DomParser;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
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
	private ParserRegistry parserRegistry;
	private ParserHelpers parserHelpers;
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

		prismContext.parserRegistry = new ParserRegistry(schemaRegistry);
		prismContext.jaxbDomHack = new JaxbDomHack((DomParser) prismContext.parserRegistry.parserFor(PrismContext.LANG_XML), prismContext);

		prismContext.parserHelpers = new ParserHelpers(prismContext.parserRegistry, prismContext.xnodeProcessor, prismContext.beanConverter);
		
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
	public DomParser getParserDom() {
		return (DomParser) parserHelpers.parserRegistry.parserFor(LANG_XML);
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

	private Parser getParser(String language) {
		return parserRegistry.parserFor(language);
	}

	private Parser getParserNotNull(String language) {
		Parser parser = getParser(language);
		if (parser == null) {
			throw new SystemException("No parser for language '"+language+"'");
		}
		return parser;
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
	@Override
	public PrismParser parserFor(File file) {
		return new PrismParserImplIO(new ParserFileSource(file), null, ParsingContext.createDefault(), parserHelpers);
	}

	@Override
	public PrismParser parserFor(InputStream stream) {
		return new PrismParserImplIO(new ParserInputStreamSource(stream), null, ParsingContext.createDefault(), parserHelpers);
	}

	@Override
	public PrismParserNoIO parserFor(String data) {
		return new PrismParserImplNoIO(new ParserStringSource(data), null, ParsingContext.createDefault(), parserHelpers);
	}

	@Deprecated
	@Override
	public PrismParserNoIO parserFor(Element data) {
		return new PrismParserImplNoIO(new ParserElementSource(data), null, ParsingContext.createDefault(), parserHelpers);
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

	@Override
	public PrismSerializer<String> serializerFor(String language) {
		return new PrismSerializerImpl(parserHelpers, language);
	}

	@Override
	public PrismSerializer<String> xmlSerializer() {
		return serializerFor(LANG_XML);
	}

	@Override
	public PrismSerializer<String> jsonSerializer() {
		return serializerFor(LANG_JSON);
	}

	@Override
	public PrismSerializer<String> yamlSerializer() {
		return serializerFor(LANG_YAML);
	}

	@Override
	public PrismSerializer<Element> domSerializer() {
		return new PrismDomSerializerImpl();
	}

	@Override
	public PrismSerializer<XNode> xnodeSerializer() {
		return new PrismXNodeSerializerImpl();
	}

	@Override
	public <O extends Objectable> String serializeObjectToString(PrismObject<O> object, String language,
			SerializationOptions options) throws SchemaException {
		Parser parser = getParserNotNull(language);
		RootXNode xroot = xnodeProcessor.serializeObject(object);
		return parser.serializeToString(xroot, SerializationContext.forOptions(options));
	}


	@Override
	public String serializeXNodeToString(RootXNode root, String language) throws SchemaException {
		Parser parser = getParserNotNull(language);
		return parser.serializeToString(root, null);
	}


	@Override
	public String serializeAtomicValue(Object value, QName elementName, String language,
			SerializationOptions serializationOptions) throws SchemaException {
		Parser parser = getParserNotNull(language);
		SerializationContext sc = new SerializationContext(serializationOptions);
		RootXNode xnode = xnodeProcessor.serializeAtomicValue(value, elementName, sc);
		return parser.serializeToString(xnode, sc);
	}

    @Override
	public String serializeAtomicValue(JAXBElement<?> element, String language) throws SchemaException {
        Parser parser = getParserNotNull(language);
        RootXNode xnode = xnodeProcessor.serializeAtomicValue(element);
        return parser.serializeToString(xnode, null, null);
    }


    /**
     * Serializes any data - i.e. either Item or an atomic value.
     * Does not support PrismValues: TODO: implement that!
     *
     * @param object
     * @param language
     * @return
     * @throws SchemaException
     */

    @Override
	public String serializeAnyData(Object object, String language) throws SchemaException {
        Parser parser = getParserNotNull(language);
        RootXNode xnode = xnodeProcessor.serializeAnyData(object, null);
        return parser.serializeToString(xnode, null);
    }

    @Override
	public String serializeAnyData(Object object, QName defaultRootElementName, String language) throws SchemaException {
        Parser parser = getParserNotNull(language);
        RootXNode xnode = xnodeProcessor.serializeAnyData(object, defaultRootElementName, null);
        return parser.serializeToString(xnode, null);
    }

    @Override
	public Element serializeAnyDataToElement(Object object, QName defaultRootElementName) throws SchemaException {
        RootXNode xnode = xnodeProcessor.serializeAnyData(object, defaultRootElementName, null);
        return getParserDom().serializeXRootToElement(xnode);
    }
    
    @Override
	public Element serializeAnyDataToElement(Object object, QName defaultRootElementName, SerializationContext ctx) throws SchemaException {
        RootXNode xnode = xnodeProcessor.serializeAnyData(object, defaultRootElementName, ctx);
        return getParserDom().serializeXRootToElement(xnode);
    }

    @Override
	public boolean canSerialize(Object value) {
        return xnodeProcessor.canSerialize(value);
    }


//    public <T> String serializeAtomicValues(QName elementName, String language, T... values) throws SchemaException {
//        Parser parser = getParserNotNull(language);
//        PrismPropertyDefinition<T> definition = schemaRegistry.findPropertyDefinitionByElementName(elementName);
//        if (definition == null) {
//            throw new SchemaException("Prism property with name " + elementName + " couldn't be found");
//        }
//        PrismProperty property = definition.instantiate();
//        for (T value : values) {
//            property.addRealValue(value);
//        }
//        RootXNode xroot = xnodeProcessor.serializeItemAsRoot(property);
//        return parser.serializeToString(xroot);
//    }

    @Override
	@Deprecated
	public <O extends Objectable> Element serializeToDom(PrismObject<O> object) throws SchemaException {
		RootXNode xroot = xnodeProcessor.serializeObject(object);
		return getParserDom().serializeXRootToElement(xroot);
	}

    @Override
	@Deprecated
    public Element serializeValueToDom(PrismValue pval, QName elementName) throws SchemaException {
        RootXNode xroot = xnodeProcessor.serializeItemValueAsRoot(pval, elementName);
        return getParserDom().serializeXRootToElement(xroot);
    }

    @Override
	@Deprecated
    public Element serializeValueToDom(PrismValue pval, QName elementName, Document document) throws SchemaException {
        RootXNode xroot = xnodeProcessor.serializeItemValueAsRoot(pval, elementName);
        return getParserDom().serializeXRootToElement(xroot, document);
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

    @Override
	public <T extends Objectable> PrismObject<T> createObject(Class<T> clazz) throws SchemaException {
        PrismObjectDefinition definition = schemaRegistry.findObjectDefinitionByCompileTimeClass(clazz);
        if (definition == null) {
            throw new SchemaException("Definition for prism object holding " + clazz + " couldn't be found");
        }
        return definition.instantiate();
    }

	@Override
	public <T extends Objectable> T createObjectable(Class<T> clazz) throws SchemaException {
		return createObject(clazz).asObjectable();
	}

	protected ParsingContext newParsingContext() {
		return ParsingContext.createDefault();
	}
}

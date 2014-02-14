/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.parser.DomParser;
import com.evolveum.midpoint.prism.parser.JaxbDomHack;
import com.evolveum.midpoint.prism.parser.Parser;
import com.evolveum.midpoint.prism.parser.PrismBeanConverter;
import com.evolveum.midpoint.prism.parser.XNodeProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.JaxbTestUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class PrismContext {
	
	public static final String LANG_XML = "xml";
	public static final String LANG_JSON = "json";
	public static final String LANG_YAML = "yaml";

    private static final Trace LOGGER = TraceManager.getTrace(PrismContext.class);
    
    private static boolean allowSchemalessSerialization = true;
    
	private SchemaRegistry schemaRegistry;
	private XNodeProcessor xnodeProcessor;
	private PrismBeanConverter beanConverter;
	private SchemaDefinitionFactory definitionFactory;
	private PolyStringNormalizer defaultPolyStringNormalizer;
	private Map<String, Parser> parserMap;
	
	// We need to keep this because of deprecated methods and various hacks
	private DomParser parserDom;
	private JaxbDomHack jaxbDomHack;

	private PrismContext() {
		// empty
	}

	public static PrismContext create(SchemaRegistry schemaRegistry) {
		PrismContext prismContext = new PrismContext();
		prismContext.schemaRegistry = schemaRegistry;
		schemaRegistry.setPrismContext(prismContext);

		prismContext.xnodeProcessor = new XNodeProcessor(prismContext);
		prismContext.beanConverter = new PrismBeanConverter(schemaRegistry);

		prismContext.parserMap = new HashMap<String, Parser>();
		DomParser parserDom = new DomParser(schemaRegistry);
		prismContext.parserMap.put(LANG_XML, parserDom);
		prismContext.parserDom = parserDom;
		
		prismContext.jaxbDomHack = new JaxbDomHack(parserDom, prismContext);
		
		return prismContext;
	}
	
	public static PrismContext createEmptyContext(SchemaRegistry schemaRegistry){
		PrismContext prismContext = new PrismContext();
		prismContext.schemaRegistry = schemaRegistry;
		schemaRegistry.setPrismContext(prismContext);

		return prismContext;
	}

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
		PrismContext.allowSchemalessSerialization = allowSchemalessSerialization;
	}

	public SchemaRegistry getSchemaRegistry() {
		return schemaRegistry;
	}

	public void setSchemaRegistry(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}

	public XNodeProcessor getXnodeProcessor() {
		return xnodeProcessor;
	}

	/**
	 * WARNING! This is not really public method. It should NOT not used outside the prism implementation.
	 */
	public DomParser getParserDom() {
		return parserDom;
	}

	public PrismBeanConverter getBeanConverter() {
		return beanConverter;
	}

	public JaxbDomHack getJaxbDomHack() {
		return jaxbDomHack;
	}

	public SchemaDefinitionFactory getDefinitionFactory() {
		if (definitionFactory == null) {
			definitionFactory = new SchemaDefinitionFactory();
		}
		return definitionFactory;
	}

	public void setDefinitionFactory(SchemaDefinitionFactory definitionFactory) {
		this.definitionFactory = definitionFactory;
	}

	public PolyStringNormalizer getDefaultPolyStringNormalizer() {
		return defaultPolyStringNormalizer;
	}

	public void setDefaultPolyStringNormalizer(PolyStringNormalizer defaultPolyStringNormalizer) {
		this.defaultPolyStringNormalizer = defaultPolyStringNormalizer;
	}

	private Parser getParser(String language) {
		return parserMap.get(language);
	}

	private Parser getParserNotNull(String language) {
		Parser parser = getParser(language);		
		if (parser == null) {
			throw new SystemException("No parser for language '"+language+"'");
		}
		return parser;
	}

	/**
	 * Parses a DOM object and creates a prism from it.
	 */
	@Deprecated
	public <T extends Objectable> PrismObject<T> parseObject(Element objectElement) throws SchemaException {
		RootXNode xroot = parserDom.parseElementAsRoot(objectElement);
		return xnodeProcessor.parseObject(xroot);
	}

	/**
	 * Parses a file and creates a prism from it. Autodetect language.
	 * @throws IOException 
	 */
	public <T extends Objectable> PrismObject<T> parseObject(File file) throws SchemaException, IOException {
		Parser parser = null;
		for (Entry<String,Parser> entry: parserMap.entrySet()) {
			Parser aParser = entry.getValue();
			if (aParser.canParse(file)) {
				parser = aParser;
				break;
			}
		}
		if (parser == null) {
			throw new SystemException("No parser for file '"+file+"' (autodetect)");
		}
		XNode xnode = parser.parse(file);
		return xnodeProcessor.parseObject(xnode);
	}
	/**
	 * Parses a file and creates a prism from it.
	 */
	public <T extends Objectable> PrismObject<T> parseObject(File file, String language) throws SchemaException, IOException {
		Parser parser = getParserNotNull(language);
		XNode xnode = parser.parse(file);
		return xnodeProcessor.parseObject(xnode);
	}

	/**
	 * Parses a string and creates a prism from it. Autodetect language. 
	 * Used mostly for testing, but can also be used for built-in editors, etc.
	 */
	public <T extends Objectable> PrismObject<T> parseObject(String dataString) throws SchemaException {
		Parser parser = null;
		for (Entry<String,Parser> entry: parserMap.entrySet()) {
			Parser aParser = entry.getValue();
			if (aParser.canParse(dataString)) {
				parser = aParser;
				break;
			}
		}
		if (parser == null) {
			throw new SystemException("No parser for data '"+DebugUtil.excerpt(dataString,16)+"' (autodetect)");
		}
		XNode xnode = parser.parse(dataString);
		return xnodeProcessor.parseObject(xnode);
	}
	
	/**
	 * Parses a string and creates a prism from it. Used mostly for testing, but can also be used for built-in editors, etc.
	 */
	public <T extends Objectable> PrismObject<T> parseObject(String dataString, String language) throws SchemaException {
		Parser parser = getParserNotNull(language);
		XNode xnode = parser.parse(dataString);
		return xnodeProcessor.parseObject(xnode);
	}
	
	public <C extends Containerable> PrismContainer<C> parseContainer(File file, Class<C> type, String language) throws SchemaException, IOException {
		Parser parser = getParserNotNull(language);
		XNode xnode = parser.parse(file);
		return xnodeProcessor.parseContainer(xnode, type);
	}
	
	public <C extends Containerable> PrismContainer<C> parseContainer(File file, PrismContainerDefinition<C> containerDef, String language) throws SchemaException, IOException {
		Parser parser = getParserNotNull(language);
		XNode xnode = parser.parse(file);
		return xnodeProcessor.parseContainer(xnode, containerDef);
	}
	
	public <C extends Containerable> PrismContainer<C> parseContainer(String dataString, Class<C> type, String language) throws SchemaException {
		Parser parser = getParserNotNull(language);
		XNode xnode = parser.parse(dataString);
		return xnodeProcessor.parseContainer(xnode, type);
	}
	
	public <C extends Containerable> PrismContainer<C> parseContainer(String dataString, PrismContainerDefinition<C> containerDef, String language) throws SchemaException {
		Parser parser = getParserNotNull(language);
		XNode xnode = parser.parse(dataString);
		return xnodeProcessor.parseContainer(xnode, containerDef);
	}
	
	/**
	 * Set up the specified object with prism context instance and schema definition.
	 */
	public <T extends Objectable> void adopt(PrismObject<T> object, Class<T> declaredType) throws SchemaException {
		object.revive(this);
		getSchemaRegistry().applyDefinition(object, declaredType, false);
	}
	
	public <T extends Objectable> void adopt(PrismObject<T> object) throws SchemaException {
		adopt(object, object.getCompileTimeClass());
	}

	public void adopt(Objectable objectable) throws SchemaException {
		adopt(objectable.asPrismObject(), objectable.getClass());
	}
	
	public <T extends Objectable> void adopt(ObjectDelta<T> delta) throws SchemaException {
		delta.revive(this);
		getSchemaRegistry().applyDefinition(delta, delta.getObjectTypeClass(), false);
	}
	
	public <C extends Containerable, O extends Objectable> void adopt(C containerable, Class<O> type, ItemPath path) throws SchemaException {
		PrismContainerValue<C> prismContainerValue = containerable.asPrismContainerValue();
		adopt(prismContainerValue, type, path);
	}

	public <C extends Containerable, O extends Objectable> void adopt(PrismContainerValue<C> prismContainerValue, Class<O> type, ItemPath path) throws SchemaException {
		prismContainerValue.revive(this);
		getSchemaRegistry().applyDefinition(prismContainerValue, type, path, false);
	}
	
	public <C extends Containerable, O extends Objectable> void adopt(PrismContainerValue<C> prismContainerValue, QName typeName, ItemPath path) throws SchemaException {
		prismContainerValue.revive(this);
		getSchemaRegistry().applyDefinition(prismContainerValue, typeName, path, false);
	}

	public List<PrismObject<? extends Objectable>> parseObjects(File file) {
		throw new UnsupportedOperationException();
	}

	public <O extends Objectable> String serializeObjectToString(PrismObject<O> object, String language) throws SchemaException {
		Parser parser = getParserNotNull(language);
		RootXNode xroot = xnodeProcessor.serializeObject(object);
		return parser.serializeToString(xroot);
	}
	
	public <C extends Containerable> String serializeContainerValueToString(PrismContainerValue<C> cval, QName elementName, String language) throws SchemaException {
		Parser parser = getParserNotNull(language);
		
		RootXNode xroot = xnodeProcessor.serializeContainerValueRoot(cval);
		return parser.serializeToString(xroot);
	}

	@Deprecated
	public <O extends Objectable> Element serializeToDom(PrismObject<O> object) throws SchemaException {
		RootXNode xroot = xnodeProcessor.serializeObject(object);
		return parserDom.serializeToElement(xroot);
	}

    /**
     * Method used to marshal objects to xml in debug messages.
     * @param object
     * @return xml as string
     */
//    public String silentMarshalObject(Object object, Trace logger) {
//        String xml = null;
//        try {
//            QName fakeQName=new QName(PrismConstants.NS_PREFIX + "debug", "debugPrintObject");
//            if (object instanceof Objectable) {
//                xml = prismDomProcessor.serializeObjectToString(((Objectable) object).asPrismObject());
//            } else if (object instanceof Containerable) {
//                Element fakeParent = DOMUtil.createElement(DOMUtil.getDocument(), fakeQName);
//                xml = prismDomProcessor.serializeObjectToString(((Containerable) object).asPrismContainerValue(),
//                        fakeParent);
//            } else {
//                xml = prismJaxbProcessor.marshalElementToString(new JAXBElement<Object>(fakeQName, Object.class, object));
//            }
//        } catch (Exception ex) {
//            Trace log = logger != null ? logger : LOGGER;
//            LoggingUtils.logException(log, "Couldn't marshal element to string {}", ex, object);
//        }
//        return xml;
//    }

}

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

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.parser.DomParser;
import com.evolveum.midpoint.prism.parser.JaxbDomHack;
import com.evolveum.midpoint.prism.parser.JsonParser;
import com.evolveum.midpoint.prism.parser.Parser;
import com.evolveum.midpoint.prism.parser.PrismBeanConverter;
import com.evolveum.midpoint.prism.parser.XNodeProcessor;
import com.evolveum.midpoint.prism.parser.YamlParser;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DebugUtil;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	@Autowired
	private Protector defaultProtector;
	
	// We need to keep this because of deprecated methods and various hacks
	private DomParser parserDom;
	private JaxbDomHack jaxbDomHack;

    //region Standard overhead
	private PrismContext() {
		// empty
	}

	public static PrismContext create(SchemaRegistry schemaRegistry) {
		PrismContext prismContext = new PrismContext();
		prismContext.schemaRegistry = schemaRegistry;
		schemaRegistry.setPrismContext(prismContext);

		prismContext.xnodeProcessor = new XNodeProcessor(prismContext);
		prismContext.beanConverter = new PrismBeanConverter(prismContext);

		prismContext.parserMap = new HashMap<String, Parser>();
		DomParser parserDom = new DomParser(schemaRegistry);
		prismContext.parserMap.put(LANG_XML, parserDom);
		JsonParser parserJson = new JsonParser();
		prismContext.parserMap.put(LANG_JSON, parserJson);
		YamlParser parserYaml = new YamlParser();
		prismContext.parserMap.put(LANG_YAML, parserYaml);
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
	
	public Protector getDefaultProtector() {
		return defaultProtector;
	}
	
	public void setDefaultProtector(Protector defaultProtector) {
		this.defaultProtector = defaultProtector;
	}
	
    //endregion

    //region Parsing Prism objects
	/**
	 * Parses a file and creates a prism from it. Autodetect language.
	 * @throws IOException 
	 */
	public <T extends Objectable> PrismObject<T> parseObject(File file) throws SchemaException, IOException {
		Parser parser = findParser(file);
		XNode xnode = parser.parse(file);
		return xnodeProcessor.parseObject(xnode);
	}
	
	/**
	 * Parses a file and creates a prism from it.
	 */
	public <T extends Objectable> PrismObject<T> parseObject(File file, String language) throws SchemaException, IOException {
        XNode xnode = parseToXNode(file, language);
		return xnodeProcessor.parseObject(xnode);
	}

    /**
     * Parses data from an input stream and creates a prism from it.
     */
    public <T extends Objectable> PrismObject<T> parseObject(InputStream stream, String language) throws SchemaException, IOException {
        XNode xnode = parseToXNode(stream, language);
        return xnodeProcessor.parseObject(xnode);
    }

    /**
	 * Parses a string and creates a prism from it. Autodetect language. 
	 * Used mostly for testing, but can also be used for built-in editors, etc.
	 */
	public <T extends Objectable> PrismObject<T> parseObject(String dataString) throws SchemaException {
		Parser parser = findParser(dataString);
		XNode xnode = parser.parse(dataString);
		return xnodeProcessor.parseObject(xnode);
	}
	
	/**
	 * Parses a string and creates a prism from it. Used mostly for testing, but can also be used for built-in editors, etc.
	 */
	public <T extends Objectable> PrismObject<T> parseObject(String dataString, String language) throws SchemaException {
		XNode xnode = parseToXNode(dataString, language);
		return xnodeProcessor.parseObject(xnode);
	}

    /**
     * Parses a DOM object and creates a prism from it.
     */
    @Deprecated
    public <T extends Objectable> PrismObject<T> parseObject(Element objectElement) throws SchemaException {
        RootXNode xroot = parserDom.parseElementAsRoot(objectElement);
        return xnodeProcessor.parseObject(xroot);
    }

    public List<PrismObject<? extends Objectable>> parseObjects(File file) throws SchemaException, IOException {
        Parser parser = findParser(file);
        Collection<XNode> nodes = parser.parseCollection(file);
        Iterator<XNode> nodesIterator = nodes.iterator();
        List<PrismObject<? extends Objectable>> objects = new ArrayList<PrismObject<? extends Objectable>>();
        while (nodesIterator.hasNext()){
            XNode node = nodesIterator.next();
            PrismObject object = xnodeProcessor.parseObject(node);
            objects.add(object);
        }
        return objects;
    }
    
    public Collection<XNode> parseObjects(InputStream stream, String language) throws SchemaException, IOException {
        Parser parser = getParserNotNull(language);
        Collection<XNode> nodes = parser.parseCollection(stream);
        return nodes;
//        Iterator<XNode> nodesIterator = nodes.iterator();
//        List<PrismObject<? extends Objectable>> objects = new ArrayList<PrismObject<? extends Objectable>>();
//        while (nodesIterator.hasNext()){
//            XNode node = nodesIterator.next();
//            PrismObject object = xnodeProcessor.parseObject(node);
//            objects.add(object);
//        }
//        return objects;
    }
    //endregion

    //region Parsing prism containers
    public <C extends Containerable> PrismContainer<C> parseContainer(File file, Class<C> type, String language) throws SchemaException, IOException {
        XNode xnode = parseToXNode(file, language);
		return xnodeProcessor.parseContainer(xnode, type);
	}

    public <C extends Containerable> PrismContainer<C> parseContainer(File file, PrismContainerDefinition<C> containerDef, String language) throws SchemaException, IOException {
        XNode xnode = parseToXNode(file, language);
		return xnodeProcessor.parseContainer(xnode, containerDef);
	}
	
	public <C extends Containerable> PrismContainer<C> parseContainer(String dataString, Class<C> type, String language) throws SchemaException {
        XNode xnode = parseToXNode(dataString, language);
		return xnodeProcessor.parseContainer(xnode, type);
	}
	
	public <C extends Containerable> PrismContainer<C> parseContainer(String dataString, PrismContainerDefinition<C> containerDef, String language) throws SchemaException {
		XNode xnode = parseToXNode(dataString, language);
		return xnodeProcessor.parseContainer(xnode, containerDef);
	}

    /**
     * Parses prism container, trying to autodetect the definition from the root node name (if present) or from node type.
     * Both single and multivalued containers are supported.
     *
     * @param dataString String to be parsed.
     * @param language Language to be used.
     * @return
     * @throws SchemaException
     */
    public <C extends Containerable> PrismContainer<C> parseContainer(String dataString, String language) throws SchemaException {
        XNode xnode = parseToXNode(dataString, language);
        return xnodeProcessor.parseContainer(xnode);
    }
    //endregion

    //region Parsing atomic values (properties values)
    /**
     * Parses an atomic value - i.e. something that could present a property value, if such a property would exist.
     */
    public <T> T parseAtomicValue(String dataString, QName typeName, String language) throws SchemaException {
        XNode xnode = parseToXNode(dataString, language);
        return xnodeProcessor.parseAtomicValue(xnode, typeName);
    }

    public <T> T parseAtomicValue(String dataString, QName typeName) throws SchemaException {
        XNode xnode = parseToXNode(dataString);
        return xnodeProcessor.parseAtomicValue(xnode, typeName);
    }

    public <T> T parseAtomicValue(File file, QName typeName, String language) throws SchemaException, IOException {
        XNode xnode = parseToXNode(file, language);
        return xnodeProcessor.parseAtomicValue(xnode, typeName);
    }

    public <T> T parseAtomicValue(File file, QName typeName) throws SchemaException, IOException {
        XNode xnode = parseToXNode(file);
        return xnodeProcessor.parseAtomicValue(xnode, typeName);
    }

    //endregion

    //region Parsing anything (without knowing the definition up-front)
    /**
     * Parses (almost) anything: either an item with a definition, or an atomic (i.e. property-like) value.
     * Does not care for schemaless items!
     *
     * CAUTION: EXPERIMENTAL - Avoid using this method if at all possible.
     * Its result is not well defined, namely, whether it returns Item or a value.
     *
     * @param node
     * @return either Item or an unmarshalled bean value
     * @throws SchemaException
     */
    public Object parseAnyData(String dataString, String language) throws SchemaException {
        XNode xnode = parseToXNode(dataString, language);
        return xnodeProcessor.parseAnyData(xnode);
    }

    public Object parseAnyData(File file) throws SchemaException, IOException {
        XNode xnode = parseToXNode(file);
        return xnodeProcessor.parseAnyData(xnode);
    }
    /**
     * Emulates JAXB unmarshal method.
     *
     * TODO
     *
     * @param node
     * @return
     * @throws SchemaException
     */
    public <T> T parseAnyValue(File file) throws SchemaException, IOException {
        XNode xnode = parseToXNode(file);
        return xnodeProcessor.parseAnyValue(xnode);
    }

    public <T> T parseAnyValue(Element element) throws SchemaException {
        XNode xnode = parseToXNode(element);
        return xnodeProcessor.parseAnyValue(xnode);
    }

    public <T> T parseAnyValue(InputStream inputStream, String language) throws SchemaException, IOException {
        XNode xnode = parseToXNode(inputStream, language);
        return xnodeProcessor.parseAnyValue(xnode);
    }

    //endregion

    //region Parsing to XNode
    private XNode parseToXNode(String dataString) throws SchemaException {
        Parser parser = findParser(dataString);
        return parser.parse(dataString);
    }

    private XNode parseToXNode(String dataString, String language) throws SchemaException {
        Parser parser = getParserNotNull(language);
        return parser.parse(dataString);
    }

    private XNode parseToXNode(File file) throws SchemaException, IOException {
        Parser parser = findParser(file);
        return parser.parse(file);
    }

    private XNode parseToXNode(File file, String language) throws SchemaException, IOException {
        Parser parser = getParserNotNull(language);
        return parser.parse(file);
    }

    private XNode parseToXNode(InputStream stream, String language) throws SchemaException, IOException {
        Parser parser = getParserNotNull(language);
        return parser.parse(stream);
    }

    private XNode parseToXNode(Element domElement) throws SchemaException {
        return parserDom.parse(domElement);
    }

    private Parser findParser(File file) throws IOException{
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
        return parser;
    }

    private Parser findParser(String data){
        Parser parser = null;
        for (Entry<String,Parser> entry: parserMap.entrySet()) {
            Parser aParser = entry.getValue();
            if (aParser.canParse(data)) {
                parser = aParser;
                break;
            }
        }
        if (parser == null) {
            throw new SystemException("No parser for data '"+DebugUtil.excerpt(data,16)+"' (autodetect)");
        }
        return parser;
    }
    //endregion

    //region adopt(...) methods
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

    public void adopt(Containerable containerable) throws SchemaException {
        containerable.asPrismContainerValue().revive(this);
    }

    public void adopt(PrismContainerValue value) throws SchemaException {
        value.revive(this);
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
    //endregion

    //region Serializing objects, containers, atomic values (properties)
	public <O extends Objectable> String serializeObjectToString(PrismObject<O> object, String language) throws SchemaException {
		Parser parser = getParserNotNull(language);
		RootXNode xroot = xnodeProcessor.serializeObject(object);
		return parser.serializeToString(xroot);
	}
	
	public <C extends Containerable> String serializeContainerValueToString(PrismContainerValue<C> cval, QName elementName, String language) throws SchemaException {
		Parser parser = getParserNotNull(language);
		
		RootXNode xroot = xnodeProcessor.serializeItemValueAsRoot(cval, elementName);
		//System.out.println("serialized to xnode: " + xroot.debugDump());
		return parser.serializeToString(xroot);
	}

    /**
     * Serializes an atomic value - i.e. something that fits into a prism property (if such a property would exist).
     *
     * @param value Value to be serialized.
     * @param elementName Element name to be used.
     * @param language
     * @return
     * @throws SchemaException
     *
     * BEWARE, currently works only for values that can be processed via PrismBeanConvertor - i.e. not for special
     * cases like PolyStringType, ProtectedStringType, etc.
     */
    public String serializeAtomicValue(Object value, QName elementName, String language) throws SchemaException {
        Parser parser = getParserNotNull(language);
        RootXNode xnode = xnodeProcessor.serializeAtomicValue(value, elementName);
        return parser.serializeToString(xnode);
    }

    public String serializeAtomicValue(JAXBElement<?> element, String language) throws SchemaException {
        Parser parser = getParserNotNull(language);
        RootXNode xnode = xnodeProcessor.serializeAtomicValue(element);
        return parser.serializeToString(xnode);
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

    public String serializeAnyData(Object object, String language) throws SchemaException {
        Parser parser = getParserNotNull(language);
        RootXNode xnode = xnodeProcessor.serializeAnyData(object);
        return parser.serializeToString(xnode);
    }

    public String serializeAnyData(Object object, QName defaultRootElementName, String language) throws SchemaException {
        Parser parser = getParserNotNull(language);
        RootXNode xnode = xnodeProcessor.serializeAnyData(object, defaultRootElementName);
        return parser.serializeToString(xnode);
    }

    public Element serializeAnyDataToElement(Object object, QName defaultRootElementName) throws SchemaException {
        RootXNode xnode = xnodeProcessor.serializeAnyData(object, defaultRootElementName);
        return parserDom.serializeXRootToElement(xnode);
    }

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

    @Deprecated
	public <O extends Objectable> Element serializeToDom(PrismObject<O> object) throws SchemaException {
		RootXNode xroot = xnodeProcessor.serializeObject(object);
		return parserDom.serializeXRootToElement(xroot);
	}

    @Deprecated
    public Element serializeValueToDom(PrismValue pval, QName elementName) throws SchemaException {
        RootXNode xroot = xnodeProcessor.serializeItemValueAsRoot(pval, elementName);
        return parserDom.serializeXRootToElement(xroot);
    }

    @Deprecated
    public Element serializeValueToDom(PrismValue pval, QName elementName, Document document) throws SchemaException {
        RootXNode xroot = xnodeProcessor.serializeItemValueAsRoot(pval, elementName);
        return parserDom.serializeXRootToElement(xroot, document);
    }


    //endregion

    /**
     * A bit of hack: serializes any Item into a RawType.
     * Currently used for serializing script output, until a better method is devised.
     * @param value
     * @return
     */
    public RawType toRawType(Item item) throws SchemaException {
        RootXNode rootXNode = xnodeProcessor.serializeItemAsRoot(item);
        return new RawType(rootXNode, this);
    }

    public <T extends Objectable> PrismObject<T> createObject(Class<T> clazz) {
        PrismObjectDefinition definition = schemaRegistry.findObjectDefinitionByCompileTimeClass(clazz);
        if (definition == null) {
            throw new IllegalStateException("Definition for prism object holding " + clazz + " couldn't be found");
        }
        return definition.instantiate();
    }
}

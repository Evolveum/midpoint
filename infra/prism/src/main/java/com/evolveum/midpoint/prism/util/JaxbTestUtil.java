/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.prism.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;

import com.evolveum.midpoint.prism.schema.SchemaRegistryImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.schema.SchemaDescription;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * DEPRECATED as of May 12, 2014. MidPoint data structures are not to be processed by JAXB any more.
 * (The last drop was the inability to hide RawType from externally-visible model service WSDL.
 * The solution devised, based on @Raw type, prevents JAXB from correctly parsing any structures that
 * contain RawType elements.)
 *
 * So, don't use JaxbTestUtil even for testing purposes. Use PrismTestUtil instead.
 *
 * ===
 * Original description:
 *
 * JAXB testing util. Only for use in tests. DO NOT USE IN PRODUCTION CODE.
 * This util is used to test the ablility of prism JAXB representation to be used by
 * native (Sun) JAXB code.
 *
 * Note: this is what used to be PrismJaxbProcessor. Therefore there may be still a lot of junk to clean up.
 *
 * @author Radovan Semancik
 */
@Deprecated
public class JaxbTestUtil {

    private static final QName DEFAULT_ELEMENT_NAME = new QName("http://midpoint.evolveum.com/xml/ns/test/whatever-1.xsd", "whatever");

    private static final Trace LOGGER = TraceManager.getTrace(JaxbTestUtil.class);

	private PrismContext prismContext;
	private JAXBContext context;

    private static JaxbTestUtil instance;

    public static JaxbTestUtil getInstance() {
        if (instance == null) {
            instance = new JaxbTestUtil();
            instance.prismContext = PrismTestUtil.getPrismContext();
            instance.initialize();
        }
        return instance;
    }

	private JaxbTestUtil() {
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	private SchemaRegistry getSchemaRegistry() {
		return prismContext.getSchemaRegistry();
	}

	public void initialize() {
		StringBuilder sb = new StringBuilder();
		Iterator<Package> iterator = getSchemaRegistry().getCompileTimePackages().iterator();
		while (iterator.hasNext()) {
			Package jaxbPackage = iterator.next();
			sb.append(jaxbPackage.getName());
			if (iterator.hasNext()) {
				sb.append(":");
			}
		}
		String jaxbPaths = sb.toString();
		if (jaxbPaths.isEmpty()) {
			LOGGER.debug("No JAXB paths, skipping creation of JAXB context");
		} else {
			try {
				context = JAXBContext.newInstance(jaxbPaths);
			} catch (JAXBException ex) {
				throw new SystemException("Couldn't create JAXBContext for: " + jaxbPaths, ex);
			}
		}
	}

	public JAXBContext getContext() {
		return context;
	}

	public void setContext(JAXBContext context) {
		this.context = context;
	}

	public boolean isJaxbClass(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("No class, no fun");
		}
		if (clazz.getPackage() == null) {
			// No package: this is most likely a primitive type and definitely
			// not a JAXB class
			return false;
		}
		for (Package jaxbPackage: getSchemaRegistry().getCompileTimePackages()) {
			if (jaxbPackage.equals(clazz.getPackage())) {
				return true;
			}
		}
		return false;
	}

	public boolean canConvert(Class<?> clazz) {
		return isJaxbClass(clazz);
	}

	public boolean canConvert(QName xsdType) {
		SchemaDescription schemaDesc = getSchemaRegistry().findSchemaDescriptionByNamespace(xsdType.getNamespaceURI());
		if (schemaDesc == null) {
			return false;
		}
		if (schemaDesc.getCompileTimeClassesPackage() == null) {
			return false;
		}
		// We may be answering "yes" to a broader set of types that we can really convert.
		// But that does not matter that much. If the type is in the correct namespace
		// then either we can convert it or nobody can.
		return true;
		// Following code is not really correct. There are XSD types that we can convert and there is
		// no complexTypeDefinition for then in our parsed schema. E.g. all the property JAXB types.
//		ComplexTypeDefinition complexTypeDefinition = schema.findComplexTypeDefinition(xsdType);
//		return complexTypeDefinition != null;
	}

	public <T> Class<T> getCompileTimeClass(QName xsdType) {
		SchemaDescription desc = getSchemaRegistry().findSchemaDescriptionByNamespace(xsdType.getNamespaceURI());
		if (desc == null) {
			return null;
		}
		Map<QName, Class<?>> map = desc.getXsdTypeTocompileTimeClassMap();
		if (map == null) {
			return null;
		}
		return (Class<T>) map.get(xsdType);
	}

	public <T> T toJavaValue(Element element, Class<T> typeClass) throws JAXBException {
		QName type = JAXBUtil.getTypeQName(typeClass);
		return (T) toJavaValue(element, type);
	}

	/**
	 * Used to convert property values from DOM
	 */
	public Object toJavaValue(Element element, QName xsdType) throws JAXBException {
		Class<?> declaredType = getCompileTimeClass(xsdType);
		if (declaredType == null) {
			// This may happen if the schema is runtime and there is no associated compile-time class
			throw new SystemException("Cannot determine Java type for "+xsdType);
		}
		JAXBElement<?> jaxbElement = createUnmarshaller().unmarshal(element, declaredType);
		Object object = jaxbElement.getValue();
		return object;
	}

	private Marshaller createMarshaller(Map<String, Object> jaxbProperties) throws JAXBException {
		Marshaller marshaller = context.createMarshaller();
		// set default properties
		marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		DynamicNamespacePrefixMapper namespacePrefixMapper = ((SchemaRegistryImpl) getSchemaRegistry()).getNamespacePrefixMapper().clone();
		namespacePrefixMapper.setAlwaysExplicit(true);
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", namespacePrefixMapper);
		// set custom properties
		if (jaxbProperties != null) {
			for (Entry<String, Object> property : jaxbProperties.entrySet()) {
				marshaller.setProperty(property.getKey(), property.getValue());
			}
		}

		return marshaller;
	}

	/**
	 * Allow for pooling and other fancy stuff. Now it dumb, creates in every call.
	 */
	private Marshaller getMarshaller() throws JAXBException {
		return createMarshaller(null);
	}

	private Unmarshaller createUnmarshaller() throws JAXBException {
		return context.createUnmarshaller();
	}

	/**
	 * Allow for pooling and other fancy stuff. Now it dumb, creates in every call.
	 */
	public Unmarshaller getUnmarshaller() throws JAXBException {
		return createUnmarshaller();
	}

    public String marshalToString(Objectable objectable) throws JAXBException {
        return marshalToString(objectable, new HashMap<>());
    }

	public String marshalToString(Objectable objectable, Map<String, Object> properties) throws JAXBException {
		QName elementQName = determineElementQName(objectable);
		JAXBElement<Object> jaxbElement = new JAXBElement<Object>(elementQName, (Class) objectable.getClass(), objectable);
		return marshalElementToString(jaxbElement, properties);
	}

    public String marshalElementToString(JAXBElement<?> jaxbElement) throws JAXBException {
        return marshalElementToString(jaxbElement, new HashMap<>());
    }

	public String marshalElementToString(JAXBElement<?> jaxbElement, Map<String, Object> properties) throws JAXBException {
		StringWriter writer = new StringWriter();
		Marshaller marshaller = getMarshaller();
        for (Entry<String, Object> entry : properties.entrySet()) {
            marshaller.setProperty(entry.getKey(), entry.getValue());
        }
		marshaller.marshal(jaxbElement, writer);
		return writer.getBuffer().toString();
	}

    public String marshalElementToString(Object element) throws JAXBException {
        return marshalElementToString(element, new HashMap<>());
    }

	/**
	 * Serializes DOM or JAXB element to string
	 */
	public String marshalElementToString(Object element, Map<String, Object> properties) throws JAXBException {
		if (element == null) {
			return null;
		}
		if (element instanceof Element) {
			return DOMUtil.serializeDOMToString((Element) element);
		} else if (element instanceof JAXBElement) {
			return marshalElementToString((JAXBElement<?>)element, properties);
		} else {
			throw new IllegalArgumentException("Unsupported element type "+element.getClass().getName());
		}
	}

	/**
	 * Serializes DOM or JAXB element to string, using specified elementName if needed.
	 */
	public String marshalElementToString(Object element, QName elementName) throws JAXBException {
		if (element == null) {
			return null;
		}
		if (element instanceof Element) {
			return DOMUtil.serializeDOMToString((Element) element);
		} else if (element instanceof JAXBElement) {
			return marshalElementToString((JAXBElement<?>)element);
		} else {
			JAXBElement<Object> jaxbElement = new JAXBElement<>(elementName, Object.class, element);
			return marshalElementToString(jaxbElement);
		}
	}

	public void marshalToDom(Objectable objectable, Node parentNode) throws JAXBException {
		QName elementQName = determineElementQName(objectable);
		JAXBElement<Object> jaxbElement = new JAXBElement<Object>(elementQName, (Class) objectable.getClass(), objectable);
		marshalElementToDom(jaxbElement, parentNode);
	}

	public void marshalElementToDom(JAXBElement<?> jaxbElement, Node parentNode) throws JAXBException {
		getMarshaller().marshal(jaxbElement, parentNode);
	}

	public <T> Element marshalElementToDom(JAXBElement<T> jaxbElement, Document doc) throws JAXBException {
		if (doc == null) {
			doc = DOMUtil.getDocument();
		}

		Element element = doc.createElementNS(jaxbElement.getName().getNamespaceURI(), jaxbElement.getName().getLocalPart());
		marshalElementToDom(jaxbElement, element);

		return (Element) element.getFirstChild();
	}

    public <T> Element marshalObjectToDom(T jaxbObject, QName elementQName) throws JAXBException {
        return marshalObjectToDom(jaxbObject, elementQName, (Document) null);
    }

    public String marshalContainerableToString(Containerable containerable) throws JAXBException {
        return marshalObjectToString(containerable, determineElementQName(containerable));
    }

    public <T> String marshalObjectToString(T jaxbObject, QName elementQName) throws JAXBException {
        JAXBElement<Object> jaxbElement = new JAXBElement<Object>(elementQName, (Class) jaxbObject.getClass(), jaxbObject);
        return marshalElementToString(jaxbElement);
    }

    public <T> Element marshalObjectToDom(T jaxbObject, QName elementQName, Document doc) throws JAXBException {
		if (doc == null) {
			doc = DOMUtil.getDocument();
		}

		JAXBElement<T> jaxbElement = new JAXBElement<>(elementQName, (Class<T>) jaxbObject.getClass(),
            jaxbObject);
		Element element = doc.createElementNS(elementQName.getNamespaceURI(), elementQName.getLocalPart());
		marshalElementToDom(jaxbElement, element);

		return (Element) element.getFirstChild();
	}

	public <T> void marshalObjectToDom(T jaxbObject, QName elementQName, Element parentElement) throws JAXBException {

		JAXBElement<T> jaxbElement = new JAXBElement<>(elementQName, (Class<T>) jaxbObject.getClass(),
            jaxbObject);
		marshalElementToDom(jaxbElement, parentElement);
	}

	public Element toDomElement(Object element) throws JAXBException {
		return toDomElement(element, DOMUtil.getDocument());
	}

	public Element toDomElement(Object jaxbElement, Document doc) throws JAXBException {
		return toDomElement(jaxbElement,doc,false,false,false);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Element toDomElement(Object jaxbElement, Document doc, boolean adopt, boolean clone, boolean deep) throws JAXBException {
		if (jaxbElement == null) {
			return null;
		}
		if (jaxbElement instanceof Element) {
			Element domElement = (Element) jaxbElement;
			if (clone) {
				domElement = (Element) domElement.cloneNode(deep);
			}
			if (domElement.getOwnerDocument().equals(doc)) {
				return domElement;
			}
			if (adopt) {
				doc.adoptNode(domElement);
			}
			return domElement;
		} else if (jaxbElement instanceof JAXBElement) {
			return marshalElementToDom((JAXBElement) jaxbElement, doc);
		} else {
			throw new IllegalArgumentException("Not an element: " + jaxbElement + " ("
					+ jaxbElement.getClass().getName() + ")");
		}
	}


	public <T> JAXBElement<T> unmarshalElement(String xmlString, Class<T> type) throws JAXBException, SchemaException {
		if (xmlString == null) {
			return null;
		}

		xmlString = xmlString.trim();
		if (!xmlString.startsWith("<") || !xmlString.endsWith(">")) {
			throw new IllegalArgumentException("Provided string is unlikely to be an XML");
		}

		StringReader reader = null;
		try {
			reader = new StringReader(xmlString);
			JAXBElement<T> element = unmarshalElement(reader, type);
			adopt(element);
			return element;
		} finally {
			if (reader != null) {
				IOUtils.closeQuietly(reader);
			}
		}
	}

	public <T> T unmarshalObject(InputStream input, Class<T> type) throws JAXBException, SchemaException {
		Object object = getUnmarshaller().unmarshal(input);
		JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
		adopt(jaxbElement);

		if (jaxbElement == null) {
			return null;
		}
		T value = jaxbElement.getValue();
		// adopt not needed, already adopted in unmarshalElement call above
		return value;

	}

	public <T> T unmarshalObject(InputStream input) throws JAXBException, SchemaException {
		Object object = getUnmarshaller().unmarshal(input);
		JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
		adopt(jaxbElement);

		if (jaxbElement == null) {
			return null;
		}
		T value = jaxbElement.getValue();
		// adopt not needed, already adopted in unmarshalElement call above
		return value;

	}

	public <T> JAXBElement<T> unmarshalElement(Reader reader, Class<T> type) throws JAXBException, SchemaException {
		Object object = getUnmarshaller().unmarshal(reader);
		JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
		adopt(jaxbElement);
		return jaxbElement;
	}

	public <T> T unmarshalToObject(Node node, Class<T> type) throws JAXBException, SchemaException {
		JAXBElement<T> element = unmarshalElement(node, type);
		if (element == null) {
			return null;
		}
		adopt(element);
		return element.getValue();
	}

	public <T> JAXBElement<T> unmarshalElement(Node node, Class<T> type) throws JAXBException, SchemaException {
		Object object = createUnmarshaller().unmarshal(node);
		JAXBElement<T> jaxbElement = (JAXBElement<T>) object;
		adopt(jaxbElement);
		return jaxbElement;
	}

	public <T> T unmarshalObject(File file, Class<T> type) throws JAXBException, SchemaException, FileNotFoundException {
		JAXBElement<T> element = unmarshalElement(file, type);
		if (element == null) {
			return null;
		}
		T value = element.getValue();
		// adopt not needed, already adopted in unmarshalElement call above
		if (!type.isAssignableFrom(value.getClass())) {
			throw new IllegalArgumentException("Unmarshalled "+value.getClass()+" from file "+file+" while "+type+" was expected");
		}
		return value;
	}

	public Object unmarshalObjects(File file) throws JAXBException{
		return createUnmarshaller().unmarshal(file);
	}

	public <T> T unmarshalObject(String stringXml, Class<T> type) throws JAXBException, SchemaException {
		JAXBElement<T> element = unmarshalElement(stringXml, type);
		if (element == null) {
			return null;
		}
		T value = element.getValue();
		adopt(value, type);
		return value;
	}

    // element name must correspond to the name that points to the container definition
    public <T extends Containerable> PrismContainer<T> unmarshalSingleValueContainer(File file, Class<T> type) throws JAXBException, SchemaException, FileNotFoundException {
        return unmarshalSingleValueContainer(unmarshalElement(file, type));
    }

    // element name must correspond to the name that points to the container definition
    public <T extends Containerable> PrismContainer<T> unmarshalSingleValueContainer(String stringXml, Class<T> type) throws JAXBException, SchemaException {
        return unmarshalSingleValueContainer(unmarshalElement(stringXml, type));
    }

    // element name must correspond to the name that points to the container definition
    private <T extends Containerable> PrismContainer<T> unmarshalSingleValueContainer(JAXBElement<T> element) throws JAXBException, SchemaException {
        if (element == null) {
            return null;
        }
        T value = element.getValue();

        // this is a bit tricky - we have to create a container and put the newly obtained value into it
        PrismContainerValue<T> containerValue = value.asPrismContainerValue();
        containerValue.revive(prismContext);
        PrismContainerDefinition<T> definition = prismContext.getSchemaRegistry().findContainerDefinitionByElementName(element.getName());
        if (definition == null) {
            throw new IllegalStateException("There's no container definition for element name " + element.getName());
        }
        containerValue.applyDefinition(definition, false);
        PrismContainer container = definition.instantiate();
        container.add(containerValue);
        return container;
    }


    public <T> T unmarshalObject(Object domOrJaxbElement, Class<T> type) throws SchemaException {
		JAXBElement<T> element;
		if (domOrJaxbElement instanceof JAXBElement<?>) {
			element = (JAXBElement<T>) domOrJaxbElement;
		} else if (domOrJaxbElement instanceof Node) {
			try {
				element = unmarshalElement((Node)domOrJaxbElement, type);
			} catch (JAXBException e) {
				throw new SchemaException(e.getMessage(),e);
			}
		} else {
			throw new IllegalArgumentException("Unknown element type "+domOrJaxbElement);
		}
		if (element == null) {
			return null;
		}
		T value = element.getValue();
		adopt(value, type);
		return value;
	}

	public <T> JAXBElement<T> unmarshalElement(File file, Class<T> type) throws SchemaException, FileNotFoundException, JAXBException {
		if (file == null) {
			throw new IllegalArgumentException("File argument must not be null.");
		}

		InputStream is = null;
		try {
			is = new FileInputStream(file);
			JAXBElement<T> element = (JAXBElement<T>) getUnmarshaller().unmarshal(is);
			adopt(element);
			return element;
		} catch (RuntimeException ex){
			throw new SystemException(ex);
		} finally {
			if (is != null) {
				IOUtils.closeQuietly(is);
			}
		}
	}

	public <T> JAXBElement<T> unmarshalElement(InputStream is, Class<T> type) throws SchemaException, FileNotFoundException, JAXBException {

		try {
			JAXBElement<T> element = (JAXBElement<T>) getUnmarshaller().unmarshal(is);
			adopt(element);
			return element;
		} finally {
			if (is != null) {
				IOUtils.closeQuietly(is);
			}
		}
	}

    public <T> T unmarshalRootObject(File file, Class<T> type) throws JAXBException, FileNotFoundException, SchemaException {
        Validate.notNull(file, "File must not be null.");
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            T object = (T) getUnmarshaller().unmarshal(is);
            adopt(object);
            return object;
        } finally {
            if (is != null) {
                IOUtils.closeQuietly(is);
            }
        }
    }

	public boolean compareAny(List<Object> a, List<Object> b) {
		if (a == b) {
			return true;
		}
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a.size() != b.size()) {
			return false;
		}
		for (int i = 0; i < a.size(); i++) {
			if (!compareElement(a.get(i), b.get(i))) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private boolean compareElement(Object a, Object b) {
		if (a == b) {
			return true;
		}
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		Document doc = null;
		Element ae = null;
		Element be = null;

		if (a instanceof Element) {
			ae = (Element) a;
		} else if (a instanceof JAXBElement) {
			if (doc == null) {
				doc = DOMUtil.getDocument();
			}
			try {
				ae = marshalElementToDom((JAXBElement) a, doc);
			} catch (JAXBException e) {
				throw new IllegalStateException("Failed to marshall element " + a, e);
			}
		} else {
			throw new IllegalArgumentException("Got unexpected type " + a.getClass().getName() + ": " + a);
		}

		if (b instanceof Element) {
			be = (Element) b;
		} else if (a instanceof JAXBElement) {
			if (doc == null) {
				doc = DOMUtil.getDocument();
			}
			try {
				be = marshalElementToDom((JAXBElement) a, doc);
			} catch (JAXBException e) {
				throw new IllegalStateException("Failed to marshall element " + b, e);
			}
		} else {
			throw new IllegalArgumentException("Got unexpected type " + b.getClass().getName() + ": " + b);
		}

		return DOMUtil.compareElement(ae, be, true);
	}


	public <T> T fromElement(Object element, Class<T> type) throws SchemaException {

		if (element == null) {
			return null;
		}

		if (type.isAssignableFrom(element.getClass())) {
			return (T) element;
		}

		if (element instanceof JAXBElement) {
			if (((JAXBElement) element).getValue() == null) {
				return null;
			}
			if (type.isAssignableFrom(((JAXBElement) element).getValue().getClass())) {
				return (T) ((JAXBElement) element).getValue();
			}
		}

		if (element instanceof Element) {
			try {
				JAXBElement<T> unmarshalledElement = unmarshalElement((Element)element, type);
				return unmarshalledElement.getValue();
			} catch (JAXBException e) {
				throw new IllegalArgumentException("Unmarshall failed: " + e.getMessage(),e);
			}
		}

		throw new IllegalArgumentException("Unknown element type "+element.getClass().getName());
	}

	private QName determineElementQName(Objectable objectable) {
		PrismObject<?> prismObject = objectable.asPrismObject();
		if (prismObject.getElementName() != null) {
			return prismObject.getElementName();
		}
		PrismObjectDefinition<?> definition = prismObject.getDefinition();
		if (definition != null) {
			if (definition.getName() != null) {
				return definition.getName();
			}
		}
		throw new IllegalStateException("Cannot determine element name of "+objectable);
	}

    private QName determineElementQName(Containerable containerable) {
        PrismContainerValue prismContainerValue = containerable.asPrismContainerValue();
        PrismContainerDefinition<?> definition = prismContainerValue.getParent() != null ? prismContainerValue.getParent().getDefinition() : null;
        if (definition != null) {
            if (definition.getName() != null) {
                return definition.getName();
            }
        }
        throw new IllegalStateException("Cannot determine element name of " + containerable + " (parent = " + prismContainerValue.getParent() + ", definition = " + definition + ")");
    }

    private boolean isObjectable(Class type) {
		return Objectable.class.isAssignableFrom(type);
	}

	private <T> void adopt(T object, Class<T> type) throws SchemaException {
		if (object instanceof Objectable) {
			getPrismContext().adopt(((Objectable)object));
		}
	}

    private void adopt(Object object) throws SchemaException {
        if (object instanceof JAXBElement) {
            adopt(((JAXBElement)object).getValue());
        } else if (object instanceof Objectable) {
            getPrismContext().adopt(((Objectable)(object)));
        }
    }

    public static String marshalWrap(Object jaxbObject) throws JAXBException {
        JAXBElement<Object> jaxbElement = new JAXBElement<Object>(DEFAULT_ELEMENT_NAME, (Class) jaxbObject.getClass(), jaxbObject);
        return getInstance().marshalElementToString(jaxbElement);
    }

}

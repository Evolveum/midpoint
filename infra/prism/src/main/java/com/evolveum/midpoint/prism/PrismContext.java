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

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.parser.PrismBeanConverter;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class PrismContext {

    private static final Trace LOGGER = TraceManager.getTrace(PrismContext.class);
	private SchemaRegistry schemaRegistry;
	private PrismJaxbProcessor prismJaxbProcessor;
	private PrismDomProcessor prismDomProcessor;
	private PrismBeanConverter beanConverter;
	private SchemaDefinitionFactory definitionFactory;
	private PolyStringNormalizer defaultPolyStringNormalizer;

	private PrismContext() {
		// empty
	}

	public static PrismContext create(SchemaRegistry schemaRegistry) {
		PrismContext prismContext = new PrismContext();
		prismContext.schemaRegistry = schemaRegistry;
		schemaRegistry.setPrismContext(prismContext);

		PrismJaxbProcessor prismJaxbProcessor = new PrismJaxbProcessor(prismContext);
		prismJaxbProcessor.initialize();
		prismContext.prismJaxbProcessor = prismJaxbProcessor;

		PrismDomProcessor prismDomProcessor = new PrismDomProcessor(schemaRegistry);
		prismDomProcessor.setPrismContext(prismContext);
		prismContext.prismDomProcessor = prismDomProcessor;
		
		PrismBeanConverter beanConverter = new PrismBeanConverter(schemaRegistry);
		prismContext.beanConverter = beanConverter;

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

	public SchemaRegistry getSchemaRegistry() {
		return schemaRegistry;
	}

	public void setSchemaRegistry(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}

	public PrismJaxbProcessor getPrismJaxbProcessor() {
		return prismJaxbProcessor;
	}

	public void setPrismJaxbProcessor(PrismJaxbProcessor prismJaxbProcessor) {
		this.prismJaxbProcessor = prismJaxbProcessor;
	}

	public PrismDomProcessor getPrismDomProcessor() {
		return prismDomProcessor;
	}

	public void setPrismDomProcessor(PrismDomProcessor prismDomProcessor) {
		this.prismDomProcessor = prismDomProcessor;
	}

	public PrismBeanConverter getBeanConverter() {
		return beanConverter;
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

	/**
	 * Parses a DOM object and creates a prism from it. It copies data from the original object to the prism.
	 */
	public <T extends Objectable> PrismObject<T> parseObject(Element objectElement) throws SchemaException {
		return prismDomProcessor.parseObject(objectElement);
	}

	/**
	 * Parses a file and creates a prism from it.
	 */
	public <T extends Objectable> PrismObject<T> parseObject(File file) throws SchemaException {
		// Use DOM now. We will switch to StAX later.
		return prismDomProcessor.parseObject(file);
	}

	/**
	 * Parses a string and creates a prism from it.
	 */
	public <T extends Objectable> PrismObject<T> parseObject(String xmlString) throws SchemaException {
		// Use DOM now. We will switch to StAX later.
		return prismDomProcessor.parseObject(xmlString);
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

    /**
     * Method used to marshal objects to xml in debug messages.
     * @param object
     * @return xml as string
     */
    public String silentMarshalObject(Object object, Trace logger) {
        String xml = null;
        try {
            QName fakeQName=new QName(PrismConstants.NS_PREFIX + "debug", "debugPrintObject");
            if (object instanceof Objectable) {
                xml = prismDomProcessor.serializeObjectToString(((Objectable) object).asPrismObject());
            } else if (object instanceof Containerable) {
                Element fakeParent = DOMUtil.createElement(DOMUtil.getDocument(), fakeQName);
                xml = prismDomProcessor.serializeObjectToString(((Containerable) object).asPrismContainerValue(),
                        fakeParent);
            } else {
                xml = prismJaxbProcessor.marshalElementToString(new JAXBElement<Object>(fakeQName, Object.class, object));
            }
        } catch (Exception ex) {
            Trace log = logger != null ? logger : LOGGER;
            LoggingUtils.logException(log, "Couldn't marshal element to string {}", ex, object);
        }
        return xml;
    }

}

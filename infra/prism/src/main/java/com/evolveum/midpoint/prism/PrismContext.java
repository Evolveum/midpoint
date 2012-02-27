/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class PrismContext {
	
    private static final Trace LOGGER = TraceManager.getTrace(PrismContext.class);
	private SchemaRegistry schemaRegistry;
	private PrismJaxbProcessor prismJaxbProcessor;
	private PrismDomProcessor prismDomProcessor;
	private SchemaDefinitionFactory definitionFactory;
	
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
		
		return prismContext;
	}
	
	public void initialize() throws SchemaException, SAXException, IOException {
		schemaRegistry.initialize();
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

	public SchemaDefinitionFactory getDefinitionFactory() {
		if (definitionFactory == null) {
			definitionFactory = new SchemaDefinitionFactory();
		}
		return definitionFactory;
	}

	public void setDefinitionFactory(SchemaDefinitionFactory definitionFactory) {
		this.definitionFactory = definitionFactory;
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
     * Method used to marshal objects to xml in debug messages.
     * @param object
     * @return xml as string
     */
    public String silentMarshalObject(Object object) {
        String xml = null;
        try {
            xml = prismJaxbProcessor.marshalElementToString(object);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't marshal element to string {}", ex, object);
        }
        return xml;
    }
}

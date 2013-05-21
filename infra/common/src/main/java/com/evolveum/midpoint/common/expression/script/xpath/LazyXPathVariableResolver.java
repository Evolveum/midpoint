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

package com.evolveum.midpoint.common.expression.script.xpath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.common.expression.script.ScriptVariables;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 *  XPath variable resolver that stores variables in the map and supports lazy
 *  resolution of objects.
 *
 * @author Igor Farinic
 * @author Radovan Semancik
 */
public class LazyXPathVariableResolver implements XPathVariableResolver {

    private ScriptVariables variables;
    private ObjectResolver objectResolver;
    private String contextDescription;
    private OperationResult result;
    
    private static final Trace LOGGER = TraceManager.getTrace(LazyXPathVariableResolver.class);

    public LazyXPathVariableResolver(ScriptVariables variables, ObjectResolver objectResolver, String contextDescription, OperationResult result) {
    	this.variables = variables;
    	this.objectResolver = objectResolver;
    	this.contextDescription = contextDescription;
    	this.result = result;
    }

    @Override
    public Object resolveVariable(QName name) {
    	if (variables == null) {
    		return null;
    	}
    	
    	if (name != null && (name.getNamespaceURI() == null || name.getNamespaceURI().isEmpty())) {
    		LOGGER.warn("Using variable without a namespace ("+name+"), possible namespace problem (e.g. missing namespace prefix declaration) in "+contextDescription);
    	}
    	
    	// Note: null is a legal variable name here. It corresponds to the root node
        Object variableValue = variables.get(name);
        
        if (variableValue == null) {
        	// TODO: warning ???
        	return null;
        }
        
        QName type = null;
        
        // Attempt to resolve object reference
        if (objectResolver != null && variableValue instanceof ObjectReferenceType) {
        	ObjectReferenceType ref = (ObjectReferenceType)variableValue;
        	if (ref.getOid() == null) {
        		SchemaException newEx = new SchemaException("Null OID in reference in variable "+name+" in "+contextDescription, name);
				throw new TunnelException(newEx);
        	} else {
        		type = ref.getType();
		    	try {
		    		
					variableValue = objectResolver.resolve(ref, ObjectType.class, contextDescription, result);
					
				} catch (ObjectNotFoundException e) {
					ObjectNotFoundException newEx = new ObjectNotFoundException("Object not found during variable "+name+" resolution in "+contextDescription+": "+e.getMessage(),e, ref.getOid());
					// We have no other practical way how to handle the error
					throw new TunnelException(newEx);
				} catch (SchemaException e) {
					ExpressionSyntaxException newEx = new ExpressionSyntaxException("Schema error during variable "+name+" resolution in "+contextDescription+": "+e.getMessage(), e, name);
					throw new TunnelException(newEx);
				}
        	}
        }
     
        try {
        	return convertToXml(variableValue, name, contextDescription);
        } catch (SchemaException e) {
			throw new TunnelException(e);
		}
    }
        
    // May return primitive types or DOM Node
    public static Object convertToXml(Object variableValue, QName variableName, String contextDescription) throws SchemaException {
    	
    	try {
	        if (variableValue instanceof Objectable) {
	        	variableValue = ((Objectable)variableValue).asPrismObject();
	        }
	        
	        if (variableValue instanceof PrismObject) {
	        	PrismObject<?> prismObject = (PrismObject<?>)variableValue;
	        	PrismDomProcessor domProcessor = prismObject.getPrismContext().getPrismDomProcessor();
				variableValue = domProcessor.serializeToDom(prismObject);
				
	        } else if (variableValue instanceof PrismProperty<?>) {
	        	PrismProperty<?> prismProperty = (PrismProperty<?>)variableValue;
	        	PrismDomProcessor domProcessor = prismProperty.getPrismContext().getPrismDomProcessor();
	        	final List<Element> elementList = new ArrayList<Element>();
	        	for (PrismPropertyValue<?> value: prismProperty.getValues()) {
	        		Element valueElement = domProcessor.serializeValueToDom(value, prismProperty.getName());
	        		elementList.add(valueElement);
	        	}
	        	NodeList nodeList = new NodeList() {
					@Override
					public Node item(int index) {
						return elementList.get(index);
					}
					@Override
					public int getLength() {
						return elementList.size();
					}
				};
				variableValue = nodeList;
				
	        } else if (variableValue instanceof PrismValue) {
	        	PrismValue pval = (PrismValue)variableValue;
	        	PrismContext prismContext = pval.getPrismContext();
	        	PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
	        	variableValue = domProcessor.serializeValueToDom(pval, variableName);
	        }
	        
	        if (!((variableValue instanceof Node)||variableValue instanceof NodeList) 
	        		&& !(variableValue.getClass().getPackage().getName().startsWith("java."))) {
	        	throw new SchemaException("Unable to convert value of variable "+variableName+" to XML, still got "+variableValue.getClass().getName()+":"+variableValue+" value at the end");
	        }
	        
	        // DEBUG hack
//	        if (LOGGER.isDebugEnabled()) {
//		        LOGGER.trace("VAR "+variableName+" - "+variableValue.getClass().getName()+":");
//		        if (variableValue instanceof Node) {
//		        	LOGGER.trace(DOMUtil.serializeDOMToString((Node)variableValue));
//		        } else {
//		        	LOGGER.trace(PrettyPrinter.prettyPrint(variableValue));
//		        }
//	        }
	        
	        return variableValue;
	        
    	} catch (SchemaException e) {
    		if (variableValue != null && variableValue instanceof Dumpable) {
    			LOGGER.trace("Value of variable {}:\n{}", variableName, ((Dumpable)variableValue).dump());
    		}
    		throw new SchemaException(e.getMessage() + " while processing variable "+variableName+" with value "+variableValue
    				+" in "+contextDescription, e);
    	} catch (RuntimeException e) {
    		if (variableValue != null && variableValue instanceof Dumpable) {
    			LOGGER.trace("Value of variable {}:\n{}", variableName, ((Dumpable)variableValue).dump());
    		}
    		throw new RuntimeException(e.getClass().getName()+ ": "+e.getMessage() + " while processing variable "+variableName
    				+" with value "+variableValue+" in "+contextDescription, e);
    	}
    }
}
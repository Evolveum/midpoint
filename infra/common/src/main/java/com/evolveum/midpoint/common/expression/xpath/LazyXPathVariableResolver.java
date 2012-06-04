/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.common.expression.xpath;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;

/**
 *  XPath variable resolver that stores variables in the map and supports lazy
 *  resolution of objects.
 *
 * @author Igor Farinic
 * @author Radovan Semancik
 */
public class LazyXPathVariableResolver implements XPathVariableResolver {

    private Map<QName, Object> variables = new HashMap<QName, Object>();
    private ObjectResolver objectResolver;
    private String contextDescription;
    private OperationResult result;

    public LazyXPathVariableResolver(Map<QName, Object> variables, ObjectResolver objectResolver, String contextDescription, OperationResult result) {
    	this.variables = variables;
    	this.objectResolver = objectResolver;
    	this.result = result;
    }

    public void addVariable(QName name, Object value) {
        variables.put(name, value);
    }

    @Override
    public Object resolveVariable(QName name) {
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
					SchemaException newEx = new SchemaException("Schema error during variable "+name+" resolution in "+contextDescription+": "+e.getMessage(), e, name);
					throw new TunnelException(newEx);
				}
        	}
        }
     
        try {
        	return convertToXml(variableValue, name);
        } catch (SchemaException e) {
			throw new TunnelException(e);
		}
    }
        
    // May return primitive types or DOM Node
    public static Object convertToXml(Object variableValue, QName variableName) throws SchemaException {
    	
        if (variableValue instanceof Objectable) {
        	variableValue = ((Objectable)variableValue).asPrismObject();
        }
        
        if (variableValue instanceof PrismObject) {
        	PrismObject<?> prismObject = (PrismObject<?>)variableValue;
        	PrismDomProcessor domProcessor = prismObject.getPrismContext().getPrismDomProcessor();
			variableValue = domProcessor.serializeToDom(prismObject);
			
        } else if (variableValue instanceof PrismValue) {
        	PrismValue pval = (PrismValue)variableValue;
        	PrismContext prismContext = pval.getPrismContext();
        	PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
        	variableValue = domProcessor.serializeValueToDom(pval, variableName);
        }
        
        if (!(variableValue instanceof Node) && !(variableValue.getClass().getPackage().getName().startsWith("java."))) {
        	throw new SchemaException("Unable to convert value of variable "+variableName+" to XML, still got "+variableValue+" value at the end");
        }
        
        // DEBUG hack
//        System.out.println("VAR "+variableName+" - "+variableValue.getClass().getName()+":");
//        if (variableValue instanceof Node) {
//        	System.out.println(DOMUtil.serializeDOMToString((Node)variableValue));
//        } else {
//        	System.out.println(DebugUtil.prettyPrint(variableValue));
//        }
        
        return variableValue;
    }
}
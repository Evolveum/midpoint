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

import org.w3c.dom.Node;

import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.TunnelException;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

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
        Object retval = variables.get(name);
        
        if (retval == null) {
        	// TODO: warning ???
        	return null;
        }
        
        QName type = null;
        
        // Attempt to resolve object reference
        if (objectResolver != null && retval instanceof ObjectReferenceType) {
        	ObjectReferenceType ref = (ObjectReferenceType)retval;
        	if (ref.getOid() == null) {
        		SchemaException newEx = new SchemaException("Null OID in reference in variable "+name+" in "+contextDescription, name);
				throw new TunnelException(newEx);
        	} else {
        		type = ref.getType();
		    	try {
		    		
					retval = objectResolver.resolve(ref, contextDescription, result);
					
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
        
        if (retval instanceof ObjectType) {
        	try {
				retval = JAXBUtil.marshallObjectType((ObjectType)retval);
			} catch (JAXBException e) {
				SchemaException newEx = new SchemaException("Schema error during variable "+name+" resolution in "+contextDescription+": "+e.getMessage(), e, name);
				throw new TunnelException(newEx);
			} catch (IllegalArgumentException e) {
				SchemaException newEx = new SchemaException("Schema error during variable "+name+" resolution in "+contextDescription+": "+e.getMessage(), e, name);
				throw new TunnelException(newEx);
			}
        }
        
        if (retval instanceof MidPointObject) {
        	MidPointObject mObject = (MidPointObject)retval;
        	try {
				Node domNode = mObject.serializeToDom();
				retval = DOMUtil.getFirstChildElement(domNode);
			} catch (SchemaException e) {
				SchemaException newEx = new SchemaException("Schema error during variable "+name+" resolution in "+contextDescription+": "+e.getMessage(), e, name);
				throw new TunnelException(newEx);
			} catch (IllegalArgumentException e) {
				SchemaException newEx = new SchemaException("Schema error during variable "+name+" resolution in "+contextDescription+": "+e.getMessage(), e, name);
				throw new TunnelException(newEx);
			}
        }
        
//        System.out.println("VAR "+name+" - "+retval.getClass().getName()+":\n"+DebugUtil.prettyPrint(retval));
        return retval;
    }
}
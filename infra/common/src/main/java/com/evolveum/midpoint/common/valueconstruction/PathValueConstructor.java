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
package com.evolveum.midpoint.common.valueconstruction;

import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PropertyPathSegment;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AsIsValueConstructorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.prism.xml.ns._public.types_2.XPathType;

/**
 * @author Radovan Semancik
 */
public class PathValueConstructor implements ValueConstructor {
	
	private ObjectResolver objectResolver;
	private PrismContext prismContext;
	
    public PathValueConstructor(ObjectResolver objectResolver, PrismContext prismContext) {
		this.objectResolver = objectResolver;
		this.prismContext = prismContext;
	}

	/* (non-Javadoc)
      * @see com.evolveum.midpoint.common.valueconstruction.ValueConstructor#construct(com.evolveum.midpoint.schema.processor.PropertyDefinition, com.evolveum.midpoint.schema.processor.Property)
      */
    @Override
    public <V extends PrismValue> PrismValueDeltaSetTriple<V> construct(JAXBElement<?> constructorElement, ItemDefinition outputDefinition,
			Item<V> input, ItemDelta<V> inputDelta, Map<QName, Object> variables, 
			boolean conditionResultOld, boolean conditionResultNew,
			String contextDescription, OperationResult result) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {

        Object constructorTypeObject = constructorElement.getValue();
        if (!(constructorTypeObject instanceof Element)) {
            throw new IllegalArgumentException("Path value constructor cannot handle elements of type " + constructorTypeObject.getClass().getName());
        }
        //AsIsValueConstructorType constructorType = (AsIsValueConstructorType)constructorTypeObject;
        
        XPathHolder xpath = new XPathHolder((Element)constructorTypeObject);
        PropertyPath path = xpath.toPropertyPath();
        
        if (path.isEmpty()) {
        	PrismValueDeltaSetTriple<V> outputTriple = ItemDelta.toDeltaSetTriple(input, inputDelta, conditionResultOld, conditionResultNew);
            
            if (outputTriple == null) {
            	return null;
            }
            return outputTriple.clone();
        }
        
        PropertyPathSegment first = path.first();
        
        QName varName = null;
        if (first.isVariable()) {
        	varName = first.getName();
        	path = path.rest();
        }

        if (!variables.containsKey(varName)) {
        	throw new ExpressionEvaluationException("No variable with name "+varName);
        }
        Object baseObject = variables.get(varName);
        ObjectDelta<?> baseDelta = null;
        
        Item<V> referedItem = null;
        ItemDelta<V> referedItemDelta = null;
        
        if (baseObject instanceof ObjectReferenceType) {
        	ObjectType objectType = resolveReference((ObjectReferenceType)baseObject, objectResolver, varName, contextDescription, result);
        	if (objectType != null) {
        		baseObject = objectType.asPrismObject();
        	}
        }
        
        if (baseObject instanceof ObjectDeltaObject<?>) {
        	ObjectDeltaObject<?> odo = (ObjectDeltaObject<?>)baseObject;
        	baseDelta = odo.getDelta();
        	baseObject = odo.getOldObject();
        }
        
        if (baseObject instanceof PrismContainer<?>) {
        	baseObject = ((PrismContainer<?>)baseObject).getValue();
        }
        
        if (baseObject instanceof PrismContainerValue<?>) {
        	PrismContainerValue<?> containerValue = (PrismContainerValue<?>)baseObject;
        	referedItem = (Item<V>) containerValue.findItem(path);
        	if (baseDelta != null) {
        		referedItemDelta = (ItemDelta<V>) baseDelta.findItemDelta(path);
        	}
        }
        
        PrismValueDeltaSetTriple<V> outputTriple = ItemDelta.toDeltaSetTriple(referedItem, referedItemDelta, conditionResultOld, conditionResultNew);
        
        if (outputTriple == null) {
        	return null;
        }
        return ValueConstructorUtil.toOutputTriple(outputTriple, outputDefinition, path.last(), prismContext);
    }
    
    private ObjectType resolveReference(ObjectReferenceType ref, ObjectResolver objectResolver, QName varName, String contextDescription, 
			OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (ref.getOid() == null) {
    		throw new SchemaException("Null OID in reference in variable "+DebugUtil.prettyPrint(varName)+" in "+contextDescription);
    	} else {
	    	try {
	    		
				return objectResolver.resolve(ref, ObjectType.class, contextDescription, result);
				
			} catch (ObjectNotFoundException e) {
				throw new ObjectNotFoundException("Object not found during variable "+DebugUtil.prettyPrint(varName)+" resolution in "+contextDescription+": "+e.getMessage(),e, ref.getOid());
			} catch (SchemaException e) {
				throw new SchemaException("Schema error during variable "+DebugUtil.prettyPrint(varName)+" resolution in "+contextDescription+": "+e.getMessage(), e);
			}
    	}
    }

}

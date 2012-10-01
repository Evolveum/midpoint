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
package com.evolveum.midpoint.common.expression.evaluator;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.common.expression.ExpressionEvaluationParameters;
import com.evolveum.midpoint.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.expression.ExpressionUtil;
import com.evolveum.midpoint.common.expression.Source;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.prism.xml.ns._public.types_2.XPathType;

/**
 * @author Radovan Semancik
 */
public class PathExpressionEvaluator<V extends PrismValue> implements ExpressionEvaluator<V> {
	
	private PropertyPath path;
	private ObjectResolver objectResolver;
	private PrismContext prismContext;
	private ItemDefinition outputDefinition;
	
    public PathExpressionEvaluator(PropertyPath path, ObjectResolver objectResolver, ItemDefinition outputDefinition, PrismContext prismContext) {
    	this.path = path;
		this.objectResolver = objectResolver;
		this.outputDefinition = outputDefinition;
		this.prismContext = prismContext;
	}

    /* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#evaluate(java.util.Collection, java.util.Map, boolean, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationParameters params) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {

		ItemDeltaItem<?> resolveContext = null;
		
		if (params.getSources().size() == 1) {
			Source<?> source = params.getSources().iterator().next();
			if (path.isEmpty()) {
				PrismValueDeltaSetTriple<V> outputTriple = (PrismValueDeltaSetTriple<V>) source.toDeltaSetTriple();
				return outputTriple.clone();
			}
			resolveContext = source;
		}
		        
        Map<QName, Object> variablesAndSources = new HashMap<QName, Object>();
        
        if (params.getVariables() != null) {
	        for (Entry<QName, Object> entry: params.getVariables().entrySet()) {
	        	variablesAndSources.put(entry.getKey(), entry.getValue());
	        }
        }
	        
        for (Source<?> source: params.getSources()) {
        	variablesAndSources.put(source.getName(), source);
        }
        
        PropertyPath resolvePath = path;
        if (path.first().isVariable()) {
        	QName variableName = path.first().getName();
        	if (variablesAndSources.containsKey(variableName)) {
        		resolveContext = ExpressionUtil.toItemDeltaItem(variablesAndSources.get(variableName), objectResolver, 
        				"path expression in "+params.getContextDescription(), params.getResult());
			} else {
				throw new ExpressionEvaluationException("No variable with name "+variableName+" in "+params.getContextDescription());
			}
        	resolvePath = path.rest();
        }
        
        while (!resolvePath.isEmpty()) {
        	if (resolveContext.isContainer()) {
        		resolveContext = resolveContext.findIdi(resolvePath.head());
        		resolvePath = resolvePath.tail();
        		if (resolveContext == null) {
        			throw new ExpressionEvaluationException("Cannot find item using path "+path+" in "+params.getContextDescription());
        		}
        	} else if (resolveContext.isStructuredProperty()) {
        		// The output path does not really matter. The delta will be converted to triple anyway
        		resolveContext = resolveContext.resolveStructuredProperty(resolvePath, (PrismPropertyDefinition) outputDefinition, null);
        		break;
        	} else {
        		throw new ExpressionEvaluationException("Cannot resolve path "+resolvePath+" on "+resolveContext+" in "+params.getContextDescription());
        	}
        }
                
        PrismValueDeltaSetTriple<V> outputTriple = ItemDelta.toDeltaSetTriple((Item<V>)resolveContext.getItemOld(), 
        		(ItemDelta<V>)resolveContext.getDelta());
        
        if (outputTriple == null) {
        	return null;
        }
        
        return ExpressionUtil.toOutputTriple(outputTriple, outputDefinition, path.last(), prismContext);
    }


}

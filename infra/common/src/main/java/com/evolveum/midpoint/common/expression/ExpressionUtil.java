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
package com.evolveum.midpoint.common.expression;

import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.common.expression.functions.BasicExpressionFunctionsXPath;
import com.evolveum.midpoint.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.common.expression.functions.LogExpressionFunctions;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Recomputable;
import com.evolveum.midpoint.prism.Structured;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * @author semancik
 *
 */
public class ExpressionUtil {
	
    public static <V extends PrismValue> PrismValueDeltaSetTriple<V> toOutputTriple(PrismValueDeltaSetTriple<V> resultTriple, 
    		ItemDefinition outputDefinition, final ItemPath residualPath, final PrismContext prismContext) {
    	final Class<?> resultTripleValueClass = resultTriple.getRealValueClass();
    	if (resultTripleValueClass == null) {
    		// triple is empty. type does not matter.
    		return resultTriple;
    	}
    	Class<?> expectedJavaType = XsdTypeMapper.toJavaType(outputDefinition.getTypeName());
    	if (expectedJavaType == null) {
    		expectedJavaType = prismContext.getPrismJaxbProcessor().getCompileTimeClass(outputDefinition.getTypeName());
    	}
    	if (resultTripleValueClass == expectedJavaType) {
    		return resultTriple;
    	}
    	final Class<?> finalExpectedJavaType = expectedJavaType;
    	
    	PrismValueDeltaSetTriple<V> clonedTriple = resultTriple.clone();
    	clonedTriple.accept(new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				if (visitable instanceof PrismPropertyValue<?>) {
					PrismPropertyValue<Object> pval = (PrismPropertyValue<Object>)visitable;
					Object realVal = pval.getValue();
					if (realVal != null) {
						if (Structured.class.isAssignableFrom(resultTripleValueClass)) {
							if (residualPath != null && !residualPath.isEmpty()) {
								realVal = ((Structured)realVal).resolve(residualPath);
							}
						}
						Object convertedVal = realVal;
						if (finalExpectedJavaType != null) {
							convertedVal = JavaTypeConverter.convert(finalExpectedJavaType, realVal);
							
							// HACK, TODO: convert to Recomputable interface
							if (convertedVal != null && convertedVal instanceof PolyString) {
								((PolyString)convertedVal).recompute(prismContext.getDefaultPolyStringNormalizer());
							}
						}
						pval.setValue(convertedVal);
					}
				}
			}
		});
    	return clonedTriple;
    }

	public static Object resolvePath(ItemPath path, Map<QName, Object> variables, Object defaultContext, 
			ObjectResolver objectResolver, String shortDesc, OperationResult result) throws SchemaException, ObjectNotFoundException {
		
		Object root = defaultContext;
		ItemPath relativePath = path;
		ItemPathSegment first = path.first();
		String varDesc = "default context";
		if (first instanceof NameItemPathSegment && ((NameItemPathSegment)first).isVariable()) {
			QName varName = ((NameItemPathSegment)first).getName();
			varDesc = "variable "+PrettyPrinter.prettyPrint(varName);
			relativePath = path.rest();
			if (variables.containsKey(varName)) {
				root = variables.get(varName);
			} else {
				throw new SchemaException("No variable with name "+varName+" in "+shortDesc);
			}
		}
		if (root == null) {
			return null;
		}
		if (relativePath.isEmpty()) {
			return root;
		}
		
		if (root instanceof ObjectReferenceType) {
			root = resolveReference((ObjectReferenceType)root, objectResolver, varDesc, shortDesc, result);
		}
			
		if (root instanceof PrismObject<?>) {
			return ((PrismObject<?>)root).find(relativePath);
		} else if (root instanceof PrismContainer<?>) {
			return ((PrismContainer<?>)root).find(relativePath);
		} else if (root instanceof PrismContainerValue<?>) {
			return ((PrismContainerValue<?>)root).find(relativePath);
		} else if (root instanceof Item<?>) {
			// Except for container (which is handled above)
			throw new SchemaException("Cannot apply path "+relativePath+" to "+root+" in "+shortDesc);
		} else if (root instanceof ObjectDeltaObject<?>) {
			return ((ObjectDeltaObject<?>)root).findIdi(relativePath);
		} else if (root instanceof ItemDeltaItem<?>) {
			return ((ItemDeltaItem<?>)root).findIdi(relativePath);
		} else {
			throw new IllegalArgumentException("Unexpected root "+root+" (relative path:"+relativePath+") in "+shortDesc);
		}
	}
	
	private static PrismObject<?> resolveReference(ObjectReferenceType ref, ObjectResolver objectResolver, String varDesc, String contextDescription, 
			OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (ref.getOid() == null) {
    		throw new SchemaException("Null OID in reference in variable "+varDesc+" in "+contextDescription);
    	} else {
	    	try {
	    		
				ObjectType objectType = objectResolver.resolve(ref, ObjectType.class, contextDescription, result);
				if (objectType == null) {
					throw new IllegalArgumentException("Resolve returned null for "+ref+" in "+contextDescription);
				}
				return objectType.asPrismObject();
				
			} catch (ObjectNotFoundException e) {
				throw new ObjectNotFoundException("Object not found during variable "+varDesc+" resolution in "+contextDescription+": "+e.getMessage(),e, ref.getOid());
			} catch (SchemaException e) {
				throw new SchemaException("Schema error during variable "+varDesc+" resolution in "+contextDescription+": "+e.getMessage(), e);
			}
    	}
    }

	public static ItemDefinition resolveDefinitionPath(ItemPath path, Map<QName, Object> variables,
			PrismObjectDefinition<?> defaultContext, String shortDesc) throws SchemaException {
		while (path!=null && !path.isEmpty() && !(path.first() instanceof NameItemPathSegment)) {
			path = path.rest();
		}
		Object root = defaultContext;
		ItemPath relativePath = path;
		NameItemPathSegment first = (NameItemPathSegment)path.first();
		if (first.isVariable()) {
			relativePath = path.rest();
			if (variables.containsKey(first.getName())) {
				Object varValue = variables.get(first.getName());
				if (root instanceof ItemDeltaItem<?>) {
					root = ((ItemDeltaItem<?>)varValue).getDefinition();
				} else if (root instanceof Item<?>) {
					root = ((Item<?>)varValue).getDefinition();
				} else if (root instanceof ItemDefinition) {
					// This is OK
				} else {
					throw new IllegalStateException("Unexpected content of variable "+first.getName()+": "+varValue);
				}
				if (root == null) {
					throw new IllegalStateException("Null definition in content of variable "+first.getName()+": "+varValue);
				}
			} else {
				throw new SchemaException("No variable with name "+first.getName()+" in "+shortDesc);
			}
		}
		if (root == null) {
			return null;
		}
		if (relativePath.isEmpty()) {
			return (ItemDefinition) root;
		}
		ItemDefinition result = null;
		if (root instanceof PrismObjectDefinition<?>) {
			return ((PrismObjectDefinition<?>)root).findItemDefinition(relativePath);
		} else if (root instanceof PrismContainerDefinition<?>) {
			return ((PrismContainerDefinition<?>)root).findItemDefinition(relativePath);
		} else if (root instanceof ItemDefinition) {
			// Except for container (which is handled above)
			throw new SchemaException("Cannot apply path "+relativePath+" to "+root+" in "+shortDesc);
		} else {
			throw new IllegalArgumentException("Unexpected root "+root+" in "+shortDesc);
		}
	}

	public static <V extends PrismValue> ItemDeltaItem<V> toItemDeltaItem(Object object, ObjectResolver objectResolver,
			String string, OperationResult result) {
		if (object == null) {
			return null;
		}
		
		if (object instanceof ItemDeltaItem<?>) {
			return (ItemDeltaItem<V>) object;
		}
		
        if (object instanceof PrismObject<?>) {
        	return (ItemDeltaItem<V>) new ObjectDeltaObject((PrismObject<?>)object, null, (PrismObject<?>)object);
        } else if (object instanceof Item<?>) {
        	return new ItemDeltaItem<V>((Item<V>)object, null, (Item<V>)object);
        } else if (object instanceof ItemDelta<?>) {
        	return new ItemDeltaItem<V>(null, (ItemDelta<V>)object, null);
        } else {
        	throw new IllegalArgumentException("Unexpected object "+object+" "+object.getClass());
        }
        
	}

	public static FunctionLibrary createBasicFunctionLibrary(PrismContext prismContext) {
		FunctionLibrary lib = new FunctionLibrary();
		lib.setVariableName(MidPointConstants.FUNCTION_LIBRARY_BASIC_VARIABLE_NAME);
		lib.setNamespace(MidPointConstants.NS_FUNC_BASIC);
		BasicExpressionFunctions func = new BasicExpressionFunctions(prismContext);
		lib.setGenericFunctions(func);
		BasicExpressionFunctionsXPath funcXPath = new BasicExpressionFunctionsXPath(func);
		lib.setXmlFunctions(funcXPath);
		return lib;
	}

	public static FunctionLibrary createLogFunctionLibrary(PrismContext prismContext) {
		FunctionLibrary lib = new FunctionLibrary();
		lib.setVariableName(MidPointConstants.FUNCTION_LIBRARY_LOG_VARIABLE_NAME);
		lib.setNamespace(MidPointConstants.NS_FUNC_LOG);
		LogExpressionFunctions func = new LogExpressionFunctions(prismContext);
		lib.setGenericFunctions(func);
		return lib;
	}
}

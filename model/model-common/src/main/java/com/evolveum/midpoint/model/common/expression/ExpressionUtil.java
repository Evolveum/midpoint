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
package com.evolveum.midpoint.model.common.expression;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctionsXPath;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.functions.LogExpressionFunctions;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
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
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

/**
 * @author semancik
 *
 */
public class ExpressionUtil {
	
	private static final Trace LOGGER = TraceManager.getTrace(ExpressionUtil.class);
	
    public static <V extends PrismValue> PrismValueDeltaSetTriple<V> toOutputTriple(PrismValueDeltaSetTriple<V> resultTriple, 
    		ItemDefinition outputDefinition, final ItemPath residualPath, final Protector protector, final PrismContext prismContext) {
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
						if (finalExpectedJavaType != null) {
							Object convertedVal = convertValue(finalExpectedJavaType, realVal, protector, prismContext);
							pval.setValue(convertedVal);
						}
					}
				}
			}
		});
    	return clonedTriple;
    }
    
    /**
     * Slightly more powerful version of "convert" as compared to  JavaTypeConverter.
     * This version can also encrypt/decrypt and also handles polystrings. 
     */
    public static <I,O> O convertValue(Class<O> finalExpectedJavaType, I inputVal, 
    		Protector protector, PrismContext prismContext) {
    	if (inputVal == null) {
    		return null;
    	}
    	if (finalExpectedJavaType.isInstance(inputVal)) {
    		return (O) inputVal;
    	}
    	
    	Object intermediateVal = inputVal;
		if (finalExpectedJavaType == ProtectedStringType.class) {
			String valueToEncrypt;
			if (inputVal instanceof String) {
				valueToEncrypt = (String)inputVal;
			} else {
				valueToEncrypt = JavaTypeConverter.convert(String.class, inputVal);
			}
			try {
				intermediateVal = protector.encryptString(valueToEncrypt);
			} catch (EncryptionException e) {
				throw new SystemException(e.getMessage(),e);
			}
		} else {
		
			if (inputVal instanceof ProtectedStringType) {
				String decryptedString;
				try {
					intermediateVal = protector.decryptString((ProtectedStringType)inputVal);
				} catch (EncryptionException e) {
					throw new SystemException(e.getMessage(),e);
				}
				
			}
		}
		
		O convertedVal = JavaTypeConverter.convert(finalExpectedJavaType, intermediateVal);
		
		PrismUtil.recomputeRealValue(convertedVal, prismContext);

		return convertedVal;
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

	public static FunctionLibrary createBasicFunctionLibrary(PrismContext prismContext, Protector protector) {
		FunctionLibrary lib = new FunctionLibrary();
		lib.setVariableName(MidPointConstants.FUNCTION_LIBRARY_BASIC_VARIABLE_NAME);
		lib.setNamespace(MidPointConstants.NS_FUNC_BASIC);
		BasicExpressionFunctions func = new BasicExpressionFunctions(prismContext, protector);
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
	
	public static void evaluateFilterExpressions(ObjectFilter filter, Map<QName, Object> variables, 
			ExpressionFactory expressionFactory, PrismContext prismContext,
			String shortDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		
		if (filter instanceof LogicalFilter){
			List<ObjectFilter> conditions = ((LogicalFilter) filter).getCondition();
			
			for (ObjectFilter condition : conditions){
				evaluateFilterExpressions(condition, variables, expressionFactory, prismContext, shortDesc, task, result);
			}
			
			return;
		}
		
		Element valueExpressionElement = filter.getExpression();
		if (valueExpressionElement == null
				&& (((PropertyValueFilter) filter).getValues() == null || ((PropertyValueFilter) filter).getValues().isEmpty())) {
			LOGGER.warn("No valueExpression in rule for {}", shortDesc);
			return;
		}
		
		ExpressionType valueExpression = createExpression(valueExpressionElement, prismContext);			
		
		try {
			PrismPropertyValue expressionResult = evaluateExpression(variables, prismContext,
					valueExpression, filter, expressionFactory, shortDesc, task, result);

			if (expressionResult == null || expressionResult.isEmpty()) {
				LOGGER.debug("Result of search filter expression was null or empty. Expression: {}",
						valueExpression);
				return;
			}
			// TODO: log more context
			LOGGER.trace("Search filter expression in the rule for {} evaluated to {}.", new Object[] {
					shortDesc, expressionResult });
			if (filter instanceof EqualsFilter) {
				((EqualsFilter) filter).setValue(expressionResult);
				filter.setExpression(null);
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Transforming filter to:\n{}", filter.dump());
			}
		} catch (RuntimeException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + valueExpression + ".", ex);
			throw new SystemException("Couldn't evaluate expression" + valueExpression + ": "
					+ ex.getMessage(), ex);

		} catch (SchemaException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + valueExpression + ".", ex);
			throw new SchemaException("Couldn't evaluate expression" + valueExpression + ": "
					+ ex.getMessage(), ex);
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + valueExpression + ".", ex);
			throw new ObjectNotFoundException("Couldn't evaluate expression" + valueExpression + ": "
					+ ex.getMessage(), ex);
		} catch (ExpressionEvaluationException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + valueExpression + ".", ex);
			throw new ExpressionEvaluationException("Couldn't evaluate expression" + valueExpression + ": "
					+ ex.getMessage(), ex);
		}

	}
	
	public static ExpressionType createExpression(Element valueExpressionElement, PrismContext prismContext) throws SchemaException {
		ExpressionType valueExpression = null;
		try {
			valueExpression = prismContext.getPrismJaxbProcessor().toJavaValue(
					valueExpressionElement, ExpressionType.class);
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Filter transformed to expression\n{}", valueExpression);
			}
		} catch (JAXBException ex) {
			LoggingUtils.logException(LOGGER, "Expression element couldn't be transformed.", ex);
			throw new SchemaException("Expression element couldn't be transformed: " + ex.getMessage(), ex);
		}
		
		return valueExpression;

	}

	private static PrismPropertyValue evaluateExpression(Map<QName, Object> variables, PrismContext prismContext,
			ExpressionType valueExpression, ObjectFilter filter, ExpressionFactory expressionFactory, 
			String shortDesc, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		
		//TODO rafactor after new query engine is implemented
		ItemDefinition outputDefinition = null;
		if (filter instanceof ValueFilter){
			outputDefinition = ((ValueFilter)filter).getDefinition();
		}
		
		if (outputDefinition == null){
			outputDefinition =  new PrismPropertyDefinition(ExpressionConstants.OUTPUT_ELMENT_NAME, 
					DOMUtil.XSD_STRING, prismContext);
		}
		
		return evaluateExpression(variables, outputDefinition, valueExpression, expressionFactory, shortDesc, task, parentResult);
		
		
//		String expressionResult = expressionHandler.evaluateExpression(currentShadow, valueExpression,
//				shortDesc, result);
   	}
	
	public static PrismPropertyValue evaluateExpression(Map<QName, Object> variables,
			ItemDefinition outputDefinition, ExpressionType valueExpression,
			ExpressionFactory expressionFactory,
			String shortDesc, Task task, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException{
		
		Expression<PrismPropertyValue> expression = expressionFactory.makeExpression(valueExpression,
				outputDefinition, shortDesc, parentResult);

		ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, variables, shortDesc, task, parentResult);
		PrismValueDeltaSetTriple<PrismPropertyValue> outputTriple = expression.evaluate(params);
		
		LOGGER.trace("Result of the expression evaluation: {}", outputTriple);
		
		if (outputTriple == null) {
			return null;
		}
		Collection<PrismPropertyValue> nonNegativeValues = outputTriple.getNonNegativeValues();
		if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
			return null;
		}
        if (nonNegativeValues.size() > 1) {
        	throw new ExpressionEvaluationException("Expression returned more than one value ("+nonNegativeValues.size()+") in "+shortDesc);
        }

        return nonNegativeValues.iterator().next();
	}
	
	public static Map<QName, Object> compileVariablesAndSources(ExpressionEvaluationContext params) {
		Map<QName, Object> variablesAndSources = new HashMap<QName, Object>();
        
        if (params.getVariables() != null) {
	        for (Entry<QName, Object> entry: params.getVariables().entrySet()) {
	        	variablesAndSources.put(entry.getKey(), entry.getValue());
	        }
        }
	        
        if (params.getSources() != null) {
	        for (Source<?> source: params.getSources()) {
	        	variablesAndSources.put(source.getName(), source);
	        }
        }
        
        return variablesAndSources;
	}

}

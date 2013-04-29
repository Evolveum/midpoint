/**
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionVariableDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * @author semancik
 *
 */
public class Expression<V extends PrismValue> {
	
	ExpressionType expressionType;
	ItemDefinition outputDefinition;
	PrismContext prismContext;
	ObjectResolver objectResolver;
	List<ExpressionEvaluator<V>> evaluators = new ArrayList<ExpressionEvaluator<V>>(1);
	
	private static final Trace LOGGER = TraceManager.getTrace(Expression.class);

	public Expression(ExpressionType expressionType, ItemDefinition outputDefinition, ObjectResolver objectResolver, PrismContext prismContext) {
		Validate.notNull(outputDefinition, "null outputDefinition");
		Validate.notNull(objectResolver, "null objectResolver");
		Validate.notNull(prismContext, "null prismContext");
		this.expressionType = expressionType;
		this.outputDefinition = outputDefinition;
		this.objectResolver = objectResolver;
		this.prismContext = prismContext;
	}
	
	public void parse(ExpressionFactory factory, String contextDescription, OperationResult result) 
			throws SchemaException, ObjectNotFoundException {
		if (expressionType == null) {
			evaluators.add(createDefaultEvaluator(factory, contextDescription, result));
			return;
		}
		if (expressionType.getExpressionEvaluator() != null && expressionType.getSequence() != null) {
			throw new SchemaException("Both single evaluator and sequence was specified, ambiguous situation in "+contextDescription);
		}
		if (expressionType.getExpressionEvaluator() == null && expressionType.getSequence() == null) {
			throw new SchemaException("No evaluator was specified in "+contextDescription);
		}
		if (expressionType.getExpressionEvaluator() != null) {
			ExpressionEvaluator evaluator = createEvaluator(expressionType.getExpressionEvaluator(), factory, 
					contextDescription, result);
			evaluators.add(evaluator);
		} else if (expressionType.getSequence() != null) {
			if (expressionType.getSequence().getExpressionEvaluator().isEmpty()) {
				throw new SchemaException("Empty sequence in "+contextDescription);
			}
			QName lastElementName = null;
			Collection<JAXBElement<?>> elements = new ArrayList<JAXBElement<?>>();
			for (JAXBElement<?> expresionEvaluatorElement : expressionType.getSequence().getExpressionEvaluator()) {
				if (lastElementName == null || lastElementName.equals(JAXBUtil.getElementQName(expresionEvaluatorElement))) {
					elements.add(expresionEvaluatorElement);
				} else {
					ExpressionEvaluator evaluator = createEvaluator(elements, factory, contextDescription, result);
					evaluators.add(evaluator);
					elements = new ArrayList<JAXBElement<?>>();
					elements.add(expresionEvaluatorElement);
				}
				lastElementName = JAXBUtil.getElementQName(expresionEvaluatorElement);
			}
			ExpressionEvaluator evaluator = createEvaluator(elements, factory, contextDescription, result);
			evaluators.add(evaluator);
		}
		if (evaluators.isEmpty()) {
			evaluators.add(createDefaultEvaluator(factory, contextDescription, result));
		}
	}

	private ExpressionEvaluator<V> createEvaluator(Collection<JAXBElement<?>> evaluatorElements, ExpressionFactory factory,
			String contextDescription, OperationResult result) 
			throws SchemaException, ObjectNotFoundException {
		if (evaluatorElements.isEmpty()) {
			throw new SchemaException("Empty evaluator list in "+contextDescription);
		}
		JAXBElement<?> fistEvaluatorElement = evaluatorElements.iterator().next();
		ExpressionEvaluatorFactory evaluatorFactory = factory.getEvaluatorFactory(fistEvaluatorElement.getName());
		if (evaluatorFactory == null) {
			throw new SchemaException("Unknown expression evaluator element "+fistEvaluatorElement.getName()+" in "+contextDescription);
		}
		return evaluatorFactory.createEvaluator(evaluatorElements, outputDefinition, contextDescription, result);
	}

	private ExpressionEvaluator<V> createDefaultEvaluator(ExpressionFactory factory, String contextDescription, 
			OperationResult result) throws SchemaException, ObjectNotFoundException {
		ExpressionEvaluatorFactory evaluatorFactory = factory.getDefaultEvaluatorFactory();
		if (evaluatorFactory == null) {
			throw new SystemException("Internal error: No default expression evaluator factory");
		}
		return evaluatorFactory.createEvaluator(null, outputDefinition, contextDescription, result);
	}
	
	public <V extends PrismValue> PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext parameters) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {
		
		try {
		
			Map<QName, Object> processedVariables = processInnerVariables(parameters.getVariables(), parameters.getContextDescription(),
					parameters.getResult());
			
			ExpressionEvaluationContext processedParameters = parameters.shallowClone();
			processedParameters.setVariables(processedVariables);
			
			for (ExpressionEvaluator<?> evaluator: evaluators) {
				PrismValueDeltaSetTriple<V> outputTriple = (PrismValueDeltaSetTriple<V>) evaluator.evaluate(processedParameters);
				if (outputTriple != null) {
					return outputTriple;
				}
			}
			return null;
		} catch (SchemaException ex) {
			LOGGER.trace("Script exception:"+ex.getMessage(), ex);
			throw ex;
		} catch (ExpressionEvaluationException ex) {
			LOGGER.trace("Script exception:"+ex.getMessage(), ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			LOGGER.trace("Script exception:"+ex.getMessage(), ex);
			throw ex;
		} catch (RuntimeException ex) {
			LOGGER.error("Script runtime exception:"+ex.getMessage(), ex);
			throw ex;
		}
	}

	private Map<QName, Object> processInnerVariables(Map<QName, Object> variables, String contextDescription,
			OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (expressionType == null || expressionType.getVariable() == null || expressionType.getVariable().isEmpty()) {
			// shortcut
			return variables;
		}
		Map<QName, Object> newVariables = new HashMap<QName, Object>();
		for(Entry<QName,Object> entry: variables.entrySet()) {
			newVariables.put(entry.getKey(), entry.getValue());
		}
		for (ExpressionVariableDefinitionType variableDefType: expressionType.getVariable()) {
			QName varName = variableDefType.getName();
			if (varName == null) {
				throw new SchemaException("No variable name in expression in "+contextDescription);
			}
			if (variableDefType.getObjectRef() != null) {
				ObjectType varObject = objectResolver.resolve(variableDefType.getObjectRef(), ObjectType.class, "variable "+varName+" in "+contextDescription, result);
				newVariables.put(varName, varObject);
			} else if (variableDefType.getValue() != null) {
				// Only string is supported now
				Object valueObject = variableDefType.getValue();
				if (valueObject instanceof String) {
					newVariables.put(varName, valueObject);
				} else if (valueObject instanceof Element) {
					newVariables.put(varName, ((Element)valueObject).getTextContent());
				} else {
					throw new SchemaException("Unexpected type "+valueObject.getClass()+" in variable definition "+varName+" in "+contextDescription);
				}
			} else {
				throw new SchemaException("No value for variable "+varName+" in "+contextDescription);
			}
		}
		return newVariables;
	}

	
}

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.common.expression.Expression;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ValueConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.VariableDefinitionType;

/**
 * @author Radovan Semancik
 *
 */
public class ValueConstruction<V extends PrismValue> implements Dumpable, DebugDumpable {

	private Map<QName,ValueConstructor> constructors;
	private ExpressionFactory expressionFactory;
	private Map<QName,Object> variables;
	private String shortDesc;
	private ValueConstructionType valueConstructionType;
	private ObjectResolver objectResolver;
	private Item<V> input;
	private ItemDelta<V> inputDelta;
	private PrismValueDeltaSetTriple<V> outputTriple;
	private ItemDefinition outputDefinition;
	private boolean conditionMaskOld = true;
	private boolean conditionMaskNew = true;
	
	private static final Trace LOGGER = TraceManager.getTrace(ValueConstruction.class);
	
	ValueConstruction(ValueConstructionType valueConstructionType, ItemDefinition outputDefinition,
			String shortDesc, Map<QName,ValueConstructor> constructors, ExpressionFactory expressionFactory) {
		this.shortDesc = shortDesc;
		this.valueConstructionType = valueConstructionType;
		this.constructors = constructors;
		this.expressionFactory = expressionFactory;
		this.variables = new HashMap<QName,Object>();
		this.input = null;
		this.outputTriple = null;
		this.outputDefinition = outputDefinition;
		this.objectResolver = null;
	}
	
	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}
	
	public void setInput(Item<V> input) {
		this.input = input;
	}
	
	public ItemDelta<V> getInputDelta() {
		return inputDelta;
	}

	public void setInputDelta(ItemDelta<V> inputDelta) {
		this.inputDelta = inputDelta;
	}
	
	public ItemDefinition getOutputDefinition() {
		return outputDefinition;
	}

	public void setOutputDefinition(ItemDefinition outputDefinition) {
		this.outputDefinition = outputDefinition;
	}
	
	public QName getItemName() {
		if (outputDefinition != null) {
			return outputDefinition.getName();
		}
		return null;
	}

	public void setRootNode(ObjectReferenceType objectRef) {
		addVariableDefinition(null,(Object)objectRef);
	}
	
	public void setRootNode(ObjectDeltaObject<?> odo) {
		addVariableDefinition(null,(Object)odo);
	}
	
	public void setRootNode(ObjectType objectType) {
		addVariableDefinition(null,(Object)objectType);
	}
	
	public void setRootNode(PrismObject<? extends ObjectType> mpObject) {
		addVariableDefinition(null,(Object)mpObject);
	}

	public void addVariableDefinition(VariableDefinitionType varDef) {
		if (varDef.getObjectRef() != null) {
			addVariableDefinition(varDef.getName(),varDef.getObjectRef());
		} else if (varDef.getValue() != null) {
			addVariableDefinition(varDef.getName(),varDef.getValue());
		} else {
			LOGGER.warn("Empty definition of variable {} in expression {}, ignoring it",varDef.getName(),shortDesc);
		}
		
	}
	
	public void addVariableDefinition(QName name, ObjectReferenceType objectRef) {
		addVariableDefinition(name,(Object)objectRef);
	}
	
	public void addVariableDefinition(QName name, ObjectType objectType) {
		addVariableDefinition(name,(Object)objectType);
	}
	
	public void addVariableDefinition(QName name, PrismObject<? extends ObjectType> midpointObject) {
		addVariableDefinition(name,(Object)midpointObject);
	}

	public void addVariableDefinition(QName name, String value) {
		addVariableDefinition(name,(Object)value);
	}

	public void addVariableDefinition(QName name, int value) {
		addVariableDefinition(name,(Object)value);
	}

	public void addVariableDefinition(QName name, Element value) {
		addVariableDefinition(name,(Object)value);
	}
	
	public void addVariableDefinition(QName name, PrismValue value) {
		addVariableDefinition(name,(Object)value);
	}
	
	public void addVariableDefinition(QName name, ObjectDeltaObject<?> value) {
		addVariableDefinition(name,(Object)value);
	}

	public void addVariableDefinitions(Map<QName, Object> extraVariables) {
		for (Entry<QName, Object> entry : extraVariables.entrySet()) {
			variables.put(entry.getKey(), entry.getValue());
		}
	}
	
	private void addVariableDefinition(QName name, Object value) {
		if (variables.containsKey(name)) {
			LOGGER.warn("Duplicate definition of variable {} in expression {}, ignoring it",name,shortDesc);
			return;
		}
		variables.put(name, value);
	}
	
	public boolean hasVariableDefinition(QName varName) {
		return variables.containsKey(varName);
	}
	
	public boolean isInitial() {
		if (valueConstructionType == null) {
			return false;
		}
		Boolean value = valueConstructionType.isInitial();
		if (value == null) {
			value = false;
		}
		return value;
	}

	public boolean isAuthoritative() {
		if (valueConstructionType == null) {
			return false;
		}
		Boolean value = valueConstructionType.isAuthoritative();
		if (value == null) {
			value = false;
		}
		return value;
	}

	public boolean isExclusive() {
		if (valueConstructionType == null) {
			return false;
		}
		Boolean value = valueConstructionType.isExclusive();
		if (value == null) {
			value = false;
		}
		return value;
	}
	
	public boolean isConditionMaskOld() {
		return conditionMaskOld;
	}

	public void setConditionMaskOld(boolean conditionMaskOld) {
		this.conditionMaskOld = conditionMaskOld;
	}

	public boolean isConditionMaskNew() {
		return conditionMaskNew;
	}

	public void setConditionMaskNew(boolean conditionMaskNew) {
		this.conditionMaskNew = conditionMaskNew;
	}
	
	private PrismContext getPrismContext() {
		return outputDefinition.getPrismContext();
	}

	public void evaluate(OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		if (outputDefinition == null) {
			throw new IllegalArgumentException("No output definition, cannot evaluate construction "+shortDesc);
		}
		
		boolean conditionResultOld = evaluateConditionOld(result) && conditionMaskOld;
		boolean conditionResultNew = evaluateConditionNew(result) && conditionMaskNew;
		
		if (!conditionResultOld && !conditionResultNew) {
			// TODO: tracing
			return;
		}
		// TODO: input filter
		evaluateValueConstructors(result, conditionResultOld, conditionResultNew);
		fixDefinition();
		recomputeValues();
		// TODO: output filter
	}
	
	/**
	 * Applies definition to the output if needed.
	 */
	private void fixDefinition() throws SchemaException {
		if (outputTriple == null) {
			return;
		}
		if (outputTriple.isRaw()) {
			outputTriple.applyDefinition(outputDefinition);
		}
	}
	
	private void recomputeValues() {
		if (outputTriple == null) {
			return;
		}
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				if (visitable instanceof PrismValue) {
					((PrismValue)visitable).recompute(getPrismContext());
				}
			}
		};
		outputTriple.accept(visitor);
	}


	private boolean evaluateConditionOld(OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		if (valueConstructionType == null) {
			return true;
		}
		ExpressionType conditionExpressionType = valueConstructionType.getCondition();
		if (conditionExpressionType == null) {
			return true;
		}
		Expression conditionExpression = expressionFactory.createExpression(conditionExpressionType, "condition in "+shortDesc);
		conditionExpression.addVariableDefinitionsOld(variables);
		PrismPropertyValue<Boolean> conditionValue = conditionExpression.evaluateScalar(Boolean.class, result);
		if (conditionValue == null || conditionValue.getValue() == null) {
			return true;
		}
		return conditionValue.getValue();
	}

	private boolean evaluateConditionNew(OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		if (valueConstructionType == null) {
			return true;
		}
		ExpressionType conditionExpressionType = valueConstructionType.getCondition();
		if (conditionExpressionType == null) {
			return true;
		}
		Expression conditionExpression = expressionFactory.createExpression(conditionExpressionType, "condition in "+shortDesc);
		conditionExpression.addVariableDefinitionsNew(variables);
		PrismPropertyValue<Boolean> conditionValue = conditionExpression.evaluateScalar(Boolean.class, result);
		if (conditionValue == null || conditionValue.getValue() == null) {
			return true;
		}
		return conditionValue.getValue();
	}

	
	private void evaluateValueConstructors(OperationResult result, boolean conditionResultOld, boolean conditionResultNew) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		if (valueConstructionType == null) {
			ValueConstructor constructor = getDefaultConstructor();
			outputTriple = constructor.construct(null, outputDefinition, input, inputDelta, variables, 
					conditionResultOld, conditionResultNew, shortDesc, result);
			return;
		}
		
		if (valueConstructionType.getValueConstructor() != null && valueConstructionType.getSequence() != null) {
			throw new SchemaException("Both constructor and sequence was specified, ambiguous situation in "+shortDesc);
		}
		if (valueConstructionType.getValueConstructor() == null && valueConstructionType.getSequence() == null) {
			throw new SchemaException("No constructor was specified in "+shortDesc);
		}
		
		if (valueConstructionType.getValueConstructor() != null) {
			ValueConstructor constructor = determineConstructor(valueConstructionType.getValueConstructor());
			outputTriple = constructor.construct(valueConstructionType.getValueConstructor(), outputDefinition,
						input, inputDelta, variables, conditionResultOld, conditionResultNew,
						shortDesc, result);
			return;
		}
		
		if (valueConstructionType.getSequence() != null) {
			for (JAXBElement<?> valueConstructorElement : valueConstructionType.getSequence().getValueConstructor()) {
				
				ValueConstructor constructor = determineConstructor(valueConstructorElement);
				outputTriple = constructor.construct(valueConstructorElement, outputDefinition, 
									input, inputDelta, variables, conditionResultOld, conditionResultNew, shortDesc, result);
				
				if (outputTriple != null) {
					// we got the value, no need to continue
					break;
				}
			}
		}
	}
	
	private ValueConstructor getDefaultConstructor()  {
		return constructors.get(null);
	}

	private ValueConstructor determineConstructor(JAXBElement<?> valueConstructorElement) throws SchemaException {
		if (!constructors.containsKey(valueConstructorElement.getName())) {
			throw new SchemaException("Unknown value constructor element "+valueConstructorElement.getName()+" in "+shortDesc);
		}
		return constructors.get(valueConstructorElement.getName());
	}

	public PrismValueDeltaSetTriple<V> getOutputTriple() {
		return outputTriple;
	}

	public Item<V> getOutput() throws SchemaException {
		if (outputTriple == null) {
			return null;
		}
		if (outputTriple.hasPlusSet() || outputTriple.hasMinusSet()) {
			throw new IllegalStateException("Cannot create output from "+this+" as it is not zero-only");
		}
		Item<V> output = outputDefinition.instantiate();
		output.addAll(PrismValue.cloneCollection(outputTriple.getZeroSet()));
		return output;
	}
	
	public ItemDelta<V> createEmptyDelta(PropertyPath path) {
		return outputDefinition.createEmptyDelta(path);
	}
	
	public static List<Object> getStaticValueList(ValueConstructionType valueConstruction) throws SchemaException {
		JAXBElement<?> valueConstructor = valueConstruction.getValueConstructor();
		if (valueConstructor == null) {
			return null;
		}
		if (!valueConstructor.getName().equals(SchemaConstants.C_VALUE)) {
			throw new IllegalArgumentException("Expected static value constructor but found "+valueConstructor.getName()+" in value construction");
		}
		Element element = (Element)valueConstructor.getValue();
		return XmlTypeConverter.convertValueElementAsList(element);
	}
	
	/**
	 * Shallow clone. Only the output is cloned deeply.
	 */
	public ValueConstruction<V> clone() {
		ValueConstruction<V> clone = new ValueConstruction<V>(valueConstructionType, outputDefinition, 
				shortDesc, constructors, expressionFactory);
		clone.input = this.input;
		clone.objectResolver = this.objectResolver;
		clone.outputTriple = this.outputTriple.clone();
		clone.outputDefinition = this.outputDefinition;
		clone.variables = this.variables;
		
		return clone;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((input == null) ? 0 : input.hashCode());
		result = prime * result + ((outputTriple == null) ? 0 : outputTriple.hashCode());
		result = prime * result + ((outputDefinition == null) ? 0 : outputDefinition.hashCode());
		result = prime * result + ((variables == null) ? 0 : variables.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValueConstruction other = (ValueConstruction) obj;
		if (input == null) {
			if (other.input != null)
				return false;
		} else if (!input.equals(other.input))
			return false;
		if (outputTriple == null) {
			if (other.outputTriple != null)
				return false;
		} else if (!outputTriple.equals(other.outputTriple))
			return false;
		if (outputDefinition == null) {
			if (other.outputDefinition != null)
				return false;
		} else if (!outputDefinition.equals(other.outputDefinition))
			return false;
		if (variables == null) {
			if (other.variables != null)
				return false;
		} else if (!variables.equals(other.variables))
			return false;
		return true;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<indent;i++) {
			sb.append(INDENT_STRING);
		}
		sb.append(toString());
		return sb.toString();
	}

	@Override
	public String dump() {
		return debugDump();
	}

	@Override
	public String toString() {
		return "VC(" + SchemaDebugUtil.prettyPrint(outputDefinition.getName()) + " = " + outputTriple + ")";
	}
	
}

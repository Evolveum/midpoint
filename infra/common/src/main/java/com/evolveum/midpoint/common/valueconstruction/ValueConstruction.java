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

import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.VariableDefinitionType;

/**
 * @author Radovan Semancik
 *
 */
public class ValueConstruction implements Dumpable, DebugDumpable {

	private Map<QName,ValueConstructor> constructors;
	private Map<QName,Object> variables;
	private String shortDesc;
	private ValueConstructionType valueConstructionType;
	private ObjectResolver objectResolver;
	private PrismProperty input;
	private PrismProperty output;
	private PrismPropertyDefinition outputDefinition;
	
	private static final Trace LOGGER = TraceManager.getTrace(ValueConstruction.class);
	
	ValueConstruction(ValueConstructionType valueConstructionType, PrismPropertyDefinition outputDefinition,
			String shortDesc, Map<QName,ValueConstructor> constructors) {
		this.shortDesc = shortDesc;
		this.valueConstructionType = valueConstructionType;
		this.constructors = constructors;
		this.variables = new HashMap<QName,Object>();
		this.input = null;
		this.output = null;
		this.outputDefinition = outputDefinition;
		this.objectResolver = null;
	}
	
	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}
	
	public void setInput(PrismProperty input) {
		this.input = input;
	}
	
	public void setOutputDefinitionX(PrismPropertyDefinition outputDefinition) {
		this.outputDefinition = outputDefinition;
	}
	
	public void setRootNode(ObjectReferenceType objectRef) {
		addVariableDefinition(null,(Object)objectRef);
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

	public void addVariableDefinition(QName name, Element value) {
		addVariableDefinition(name,(Object)value);
	}
	
	public void addVariableDefinition(QName name, PrismValue value) {
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
	
	public boolean isInitial() {
		Boolean value = valueConstructionType.isInitial();
		if (value == null) {
			value = false;
		}
		return value;
	}

	public boolean isAuthoritative() {
		Boolean value = valueConstructionType.isAuthoritative();
		if (value == null) {
			value = false;
		}
		return value;
	}

	public boolean isExclusive() {
		Boolean value = valueConstructionType.isExclusive();
		if (value == null) {
			value = false;
		}
		return value;
	}
	
	public void evaluate(OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		if (outputDefinition == null) {
			throw new IllegalArgumentException("No output definition, cannot evaluate construction "+shortDesc);
		}
		// TODO: evaluate condition
		// TODO: input filter
		evaluateValueConstructors(result);
		// TODO: output filter
	}
	
	private void evaluateValueConstructors(OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		if (valueConstructionType.getValueConstructor() != null && valueConstructionType.getSequence() != null) {
			throw new SchemaException("Both constructor and sequence was specified, ambiguous situation in "+shortDesc);
		}
		if (valueConstructionType.getValueConstructor() == null && valueConstructionType.getSequence() == null) {
			throw new SchemaException("No constructor was specified in "+shortDesc);
		}
		
		if (valueConstructionType.getValueConstructor() != null) {
			output = determineConstructor(valueConstructionType.getValueConstructor())
				.construct(valueConstructionType.getValueConstructor(), outputDefinition,
						input, variables, shortDesc, result);
		}
		
		if (valueConstructionType.getSequence() != null) {
			for (JAXBElement<?> valueConstructorElement : valueConstructionType.getSequence().getValueConstructor()) {
				
				output = determineConstructor(valueConstructorElement)
							.construct(valueConstructorElement, outputDefinition, 
									input, variables, shortDesc, result);
				
				if (output != null) {
					// we got the value, no need to continue
					break;
				}
			}
		}
	}

	private ValueConstructor determineConstructor(JAXBElement<?> valueConstructorElement) throws SchemaException {
		if (!constructors.containsKey(valueConstructorElement.getName())) {
			throw new SchemaException("Unknown value constructor element "+valueConstructorElement.getName()+" in "+shortDesc);
		}
		return constructors.get(valueConstructorElement.getName());
	}

	public PrismProperty getOutput() {
		return output;
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
	public ValueConstruction clone() {
		ValueConstruction clone = new ValueConstruction(valueConstructionType, outputDefinition, 
				shortDesc, constructors);
		clone.input = this.input;
		clone.objectResolver = this.objectResolver;
		clone.output = this.output.clone();
		clone.outputDefinition = this.outputDefinition;
		clone.variables = this.variables;
		
		return clone;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((input == null) ? 0 : input.hashCode());
		result = prime * result + ((output == null) ? 0 : output.hashCode());
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
		if (output == null) {
			if (other.output != null)
				return false;
		} else if (!output.equals(other.output))
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
		return "VC(" + SchemaDebugUtil.prettyPrint(outputDefinition.getName()) + " = " + output + ")";
	}
	
}

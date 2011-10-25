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
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionXType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.VariableDefinitionType;

/**
 * @author Radovan Semancik
 *
 */
public class ValueConstruction {

	private Map<QName,ValueConstructor> constructors;
	private Map<QName,Object> variables;
	private String shortDesc;
	private ValueConstructionXType valueConstructionType;
	private ObjectResolver objectResolver;
	private Property input;
	private Property output;
	private PropertyDefinition outputDefinition;
	
	private static final Trace LOGGER = TraceManager.getTrace(ValueConstruction.class);
	
	ValueConstruction(ValueConstructionXType valueConstructionType, PropertyDefinition outputDefinition, String shortDesc, Map<QName,ValueConstructor> constructors) {
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
	
	public void setInput(Property input) {
		this.input = input;
	}
	
	public void setOutputDefinition(PropertyDefinition outputDefinition) {
		this.outputDefinition = outputDefinition;
	}
	
	public void setRootNode(ObjectReferenceType objectRef) {
		addVariableDefinition(null,(Object)objectRef);
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

	public void addVariableDefinition(QName name, String value) {
		addVariableDefinition(name,(Object)value);
	}
		
	private void addVariableDefinition(QName name, Object value) {
		if (variables.containsKey(name)) {
			LOGGER.warn("Duplicate definition of variable {} in expression {}, ignoring it",name,shortDesc);
			return;
		}
		variables.put(name, value);
	}
	
	public void evaluate() throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		if (outputDefinition == null) {
			throw new IllegalArgumentException("No output definition, cannot evaluate construction "+shortDesc);
		}
		// TODO: evaluate condition
		// TODO: input filter
		evaluateValueConstructors();
		// TODO: output filter
	}
	
	private void evaluateValueConstructors() throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		for (JAXBElement<?> valueConstructorElement : valueConstructionType.getValue().getValueConstructor()) {
			
			output = determineConstructor(valueConstructorElement)
						.construct(valueConstructorElement, outputDefinition, input, variables, shortDesc);
			
			if (output != null) {
				// we got the value, no need to continue
				break;
			}
		}
	}

	private ValueConstructor determineConstructor(JAXBElement<?> valueConstructorElement) throws SchemaException {
		if (!constructors.containsKey(valueConstructorElement.getName())) {
			throw new SchemaException("Unknown value constructor element "+valueConstructorElement.getName()+" in "+shortDesc);
		}
		return constructors.get(valueConstructorElement.getName());
	}

	public Property getOutput() {
		return output;
	}
}

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
package com.evolveum.midpoint.common.expression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.VariableDefinitionType;

/**
 * @author Radovan Semancik
 *
 */
public class Expression {

	private Element code;
	private Map<QName,Object> variables;
	private String shortDesc;
	private ExpressionEvaluator evaluator;
	private ObjectResolver objectResolver;
	
	private static final Trace LOGGER = TraceManager.getTrace(Expression.class);
	
	Expression(ExpressionEvaluator evaluator, ExpressionType expressionType, String shortDesc) {
		this.code = expressionType.getCode();
		this.shortDesc = shortDesc;
		this.evaluator = evaluator;
		this.variables = new HashMap<QName,Object>();
		if (expressionType.getVariable() != null) {
			for (VariableDefinitionType varDef : expressionType.getVariable()) {
				addVariableDefinition(varDef);
			}
		}
	}
	
	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
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
	
	public void addVariableDefinitions(Map<QName, Object> extraVariables) {
		for (Entry<QName, Object> entry : extraVariables.entrySet()) {
			variables.put(entry.getKey(), entry.getValue());
		}
	}
	
	public void setRootNode(ObjectReferenceType objectRef) {
		addVariableDefinition(null,(Object)objectRef);
	}
	
	private void addVariableDefinition(QName name, Object value) {
		if (variables.containsKey(name)) {
			LOGGER.warn("Duplicate definition of variable {} in expression {}, ignoring it",name,shortDesc);
			return;
		}
		variables.put(name, value);
	}
	
	public <T> T evaluateScalar(Class<T> type) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		return evaluator.evaluateScalar(type, code, variables, objectResolver, shortDesc);
	}

	public <T> List<T> evaluateList(Class<T> type) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		return evaluator.evaluateList(type, code, variables, objectResolver, shortDesc);
	}

}

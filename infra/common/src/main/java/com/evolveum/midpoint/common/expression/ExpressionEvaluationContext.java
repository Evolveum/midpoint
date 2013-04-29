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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression;

import java.util.Collection;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Simple DTO used to contain all the parameters of expression execution.
 * 
 * Designed to allow future compatible changes (addition of optional parameters).
 * 
 * @author semancik
 *
 */
public class ExpressionEvaluationContext {

	private Collection<Source<?>> sources;
	private Map<QName, Object> variables;
	private boolean regress = false;
	private StringPolicyResolver stringPolicyResolver;
	private String contextDescription;
	private OperationResult result;
	
	public ExpressionEvaluationContext(Collection<Source<?>> sources,
			Map<QName, Object> variables, String contextDescription,
			OperationResult result) {
		super();
		this.sources = sources;
		this.variables = variables;
		this.contextDescription = contextDescription;
		this.result = result;
	}

	public Collection<Source<?>> getSources() {
		return sources;
	}
	
	public void setSources(Collection<Source<?>> sources) {
		this.sources = sources;
	}
	
	public Map<QName, Object> getVariables() {
		return variables;
	}
	
	public void setVariables(Map<QName, Object> variables) {
		this.variables = variables;
	}
	
	public boolean isRegress() {
		return regress;
	}
	
	public void setRegress(boolean regress) {
		this.regress = regress;
	}
	
	public StringPolicyResolver getStringPolicyResolver() {
		return stringPolicyResolver;
	}
	
	public void setStringPolicyResolver(StringPolicyResolver stringPolicyResolver) {
		this.stringPolicyResolver = stringPolicyResolver;
	}
	
	public String getContextDescription() {
		return contextDescription;
	}
	
	public void setContextDescription(String contextDescription) {
		this.contextDescription = contextDescription;
	}
	
	public OperationResult getResult() {
		return result;
	}
	
	public void setResult(OperationResult result) {
		this.result = result;
	}
	
	public ExpressionEvaluationContext shallowClone() {
		ExpressionEvaluationContext clone = new ExpressionEvaluationContext(sources, variables, contextDescription, result);
		clone.regress = this.regress;
		clone.stringPolicyResolver = this.stringPolicyResolver;
		return clone;
	}

}

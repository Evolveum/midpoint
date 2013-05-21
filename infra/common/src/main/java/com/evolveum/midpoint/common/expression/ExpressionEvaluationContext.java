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
	private Source<?> defaultSource;
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
	
	public Source<?> getDefaultSource() {
		return defaultSource;
	}

	public void setDefaultSource(Source<?> defaultSource) {
		this.defaultSource = defaultSource;
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
		clone.defaultSource = this.defaultSource;
		return clone;
	}

}

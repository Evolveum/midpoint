/*
 * Copyright (c) 2010-2015 Evolveum
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
import java.util.function.Function;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Simple DTO used to contain all the parameters of expression execution.
 * 
 * Designed to allow future compatible changes (addition of optional parameters).
 * 
 * @author semancik
 *
 */
public class ExpressionEvaluationContext {

	private Collection<Source<?,?>> sources;
	private Source<?,?> defaultSource;
	private ExpressionVariables variables;
	private boolean skipEvaluationPlus = false;
	private boolean skipEvaluationMinus = false;
	private StringPolicyResolver stringPolicyResolver;
	private ExpressionFactory expressionFactory;
	private PrismObjectDefinition<?> defaultTargetContext;
	private RefinedObjectClassDefinition refinedObjectClassDefinition;
	private QName mappingQName;
	private String contextDescription;
	private Task task;
	private OperationResult result;
	private Function<Object, Object> additionalConvertor;
	
	public ExpressionEvaluationContext(Collection<Source<?,?>> sources,
			ExpressionVariables variables, String contextDescription, Task task,
			OperationResult result) {
		super();
		this.sources = sources;
		this.variables = variables;
		this.contextDescription = contextDescription;
		this.task = task;
		this.result = result;
	}

	public Collection<Source<?,?>> getSources() {
		return sources;
	}
	
	public void setSources(Collection<Source<?,?>> sources) {
		this.sources = sources;
	}
	
	public Source<?,?> getDefaultSource() {
		return defaultSource;
	}

	public void setDefaultSource(Source<?,?> defaultSource) {
		this.defaultSource = defaultSource;
	}

	public ExpressionVariables getVariables() {
		return variables;
	}
	
	public void setVariables(ExpressionVariables variables) {
		this.variables = variables;
	}
	
	public boolean isSkipEvaluationPlus() {
		return skipEvaluationPlus;
	}

	public void setSkipEvaluationPlus(boolean skipEvaluationPlus) {
		this.skipEvaluationPlus = skipEvaluationPlus;
	}

	public boolean isSkipEvaluationMinus() {
		return skipEvaluationMinus;
	}

	public void setSkipEvaluationMinus(boolean skipEvaluationMinus) {
		this.skipEvaluationMinus = skipEvaluationMinus;
	}

	public StringPolicyResolver getStringPolicyResolver() {
		return stringPolicyResolver;
	}
	
	public void setStringPolicyResolver(StringPolicyResolver stringPolicyResolver) {
		this.stringPolicyResolver = stringPolicyResolver;
	}
	
	public ExpressionFactory getExpressionFactory() {
		return expressionFactory;
	}

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		this.expressionFactory = expressionFactory;
	}

	public PrismObjectDefinition<?> getDefaultTargetContext() {
		return defaultTargetContext;
	}

	public void setDefaultTargetContext(PrismObjectDefinition<?> defaultTargetContext) {
		this.defaultTargetContext = defaultTargetContext;
	}

	public RefinedObjectClassDefinition getRefinedObjectClassDefinition() {
		return refinedObjectClassDefinition;
	}

	public void setRefinedObjectClassDefinition(RefinedObjectClassDefinition refinedObjectClassDefinition) {
		this.refinedObjectClassDefinition = refinedObjectClassDefinition;
	}

	public QName getMappingQName() {
		return mappingQName;
	}

	public void setMappingQName(QName mappingQName) {
		this.mappingQName = mappingQName;
	}

	public String getContextDescription() {
		return contextDescription;
	}
	
	public void setContextDescription(String contextDescription) {
		this.contextDescription = contextDescription;
	}
	
	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public OperationResult getResult() {
		return result;
	}
	
	public void setResult(OperationResult result) {
		this.result = result;
	}

	public Function<Object, Object> getAdditionalConvertor() {
		return additionalConvertor;
	}

	public void setAdditionalConvertor(Function<Object, Object> additionalConvertor) {
		this.additionalConvertor = additionalConvertor;
	}

	public ExpressionEvaluationContext shallowClone() {
		ExpressionEvaluationContext clone = new ExpressionEvaluationContext(sources, variables, contextDescription, task, result);
		clone.skipEvaluationMinus = this.skipEvaluationMinus;
		clone.skipEvaluationPlus = this.skipEvaluationPlus;
		clone.stringPolicyResolver = this.stringPolicyResolver;
		clone.expressionFactory = this.expressionFactory;
		clone.defaultSource = this.defaultSource;
		clone.refinedObjectClassDefinition = this.refinedObjectClassDefinition;
		clone.mappingQName = this.mappingQName;
		clone.additionalConvertor = this.additionalConvertor;
		return clone;
	}

}

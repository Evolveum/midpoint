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
package com.evolveum.midpoint.common.expression.script;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.evolveum.midpoint.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScriptExpressionEvaluatorType;

/**
 * 
 * @author Radovan Semancik
 *
 */
public class ScriptExpressionFactory {
	
	public static String DEFAULT_LANGUAGE = "http://midpoint.evolveum.com/xml/ns/public/expression/language#Groovy";
	
	private Map<String,ScriptEvaluator> evaluators = new HashMap<String, ScriptEvaluator>();
	private ObjectResolver objectResolver;
	private PrismContext prismContext;
	private Collection<FunctionLibrary> functions;
	
	public ScriptExpressionFactory(ObjectResolver objectResolver, PrismContext prismContext, Collection<FunctionLibrary> functions) {
		this.prismContext = prismContext;
		this.objectResolver = objectResolver;
		this.functions = functions;
	}
	
	/**
	 * Constructor created especially to be used from the Spring context.
	 */
	public ScriptExpressionFactory(ObjectResolver objectResolver, PrismContext prismContext, 
			Collection<FunctionLibrary> functions, Collection<ScriptEvaluator> evaluators) {
		this(objectResolver, prismContext, functions);
		for (ScriptEvaluator evaluator: evaluators) {
			registerEvaluator(evaluator.getLanguageUrl(), evaluator);
		}
	}
	
	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	public Map<String, ScriptEvaluator> getEvaluators() {
		return evaluators;
	}

	public ScriptExpression createScriptExpression(ScriptExpressionEvaluatorType expressionType, ItemDefinition outputDefinition, String shortDesc) throws ExpressionSyntaxException {
		ScriptExpression expression = new ScriptExpression(getEvaluator(getLanguage(expressionType), shortDesc), expressionType);
		expression.setOutputDefinition(outputDefinition);
		expression.setObjectResolver(objectResolver);
		expression.setFunctions(functions);
		return expression;
	}
	
	public void registerEvaluator(String language, ScriptEvaluator evaluator) {
		if (evaluators.containsKey(language)) {
			throw new IllegalArgumentException("Evaluator for language "+language+" already registered");
		}
		evaluators.put(language,evaluator);
	}
	
	private ScriptEvaluator getEvaluator(String language, String shortDesc) throws ExpressionSyntaxException {
		ScriptEvaluator evaluator = evaluators.get(language);
		if (evaluator == null) {
			throw new ExpressionSyntaxException("Unsupported language "+language+" used in script in "+shortDesc);
		}
		return evaluator;
	}

	private String getLanguage(ScriptExpressionEvaluatorType expressionType) {
		if (expressionType.getLanguage() != null) {
			return expressionType.getLanguage();
		}
		return DEFAULT_LANGUAGE;
	}
	
}


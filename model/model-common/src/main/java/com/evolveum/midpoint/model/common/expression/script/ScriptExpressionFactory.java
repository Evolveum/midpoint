/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.common.expression.script;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

/**
 *
 * @author Radovan Semancik
 *
 */
public class ScriptExpressionFactory {

	public static String DEFAULT_LANGUAGE = "http://midpoint.evolveum.com/xml/ns/public/expression/language#Groovy";

	private Map<String,ScriptEvaluator> evaluatorMap = new HashMap<String, ScriptEvaluator>();
	private ObjectResolver objectResolver;
	private PrismContext prismContext;
	private Collection<FunctionLibrary> functions;
	private Protector protector;

	public ScriptExpressionFactory(PrismContext prismContext, Protector protector) {
		this.prismContext = prismContext;
		this.protector = protector;
	}

	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	public void setEvaluators(Collection<ScriptEvaluator> evaluators) {
		for (ScriptEvaluator evaluator: evaluators) {
			registerEvaluator(evaluator.getLanguageUrl(), evaluator);
		}
	}

	public Collection<FunctionLibrary> getFunctions() {
		return functions;
	}

	public void setFunctions(Collection<FunctionLibrary> functions) {
		this.functions = functions;
	}

	public Map<String, ScriptEvaluator> getEvaluators() {
		return evaluatorMap;
	}

	public ScriptExpression createScriptExpression(ScriptExpressionEvaluatorType expressionType, ItemDefinition outputDefinition, String shortDesc) throws ExpressionSyntaxException {
		ScriptExpression expression = new ScriptExpression(getEvaluator(getLanguage(expressionType), shortDesc), expressionType);
		expression.setOutputDefinition(outputDefinition);
		expression.setObjectResolver(objectResolver);
		expression.setFunctions(functions);
		return expression;
	}

	public void registerEvaluator(String language, ScriptEvaluator evaluator) {
		if (evaluatorMap.containsKey(language)) {
			throw new IllegalArgumentException("Evaluator for language "+language+" already registered");
		}
		evaluatorMap.put(language,evaluator);
	}

	private ScriptEvaluator getEvaluator(String language, String shortDesc) throws ExpressionSyntaxException {
		ScriptEvaluator evaluator = evaluatorMap.get(language);
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


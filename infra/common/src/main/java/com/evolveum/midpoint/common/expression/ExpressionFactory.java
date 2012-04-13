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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;

/**
 * 
 * @author Radovan Semancik
 *
 */
public class ExpressionFactory {
	
	public static String DEFAULT_LANGUAGE = "http://www.w3.org/TR/xpath/";
	
	private Map<String,ExpressionEvaluator> evaluators;
	private ObjectResolver objectResolver;
	
	public ExpressionFactory() {
		evaluators = new HashMap<String, ExpressionEvaluator>();
	}
	
	/**
	 * Factory method created especially to be used from the Spring context.
	 */
	@Deprecated
	public static ExpressionFactory createExpressionFactory(Map<String,ExpressionEvaluator> evaluators) {
		ExpressionFactory expressionFactory = new ExpressionFactory();
		for (Entry<String, ExpressionEvaluator> entry: evaluators.entrySet()) {
			expressionFactory.registerEvaluator(entry.getKey(), entry.getValue());
		}
		return expressionFactory;
	}
	
	/**
	 * Factory method created especially to be used from the Spring context.
	 */
	public static ExpressionFactory createExpressionFactory(Collection<ExpressionEvaluator> evaluators) {
		ExpressionFactory expressionFactory = new ExpressionFactory();
		for (ExpressionEvaluator evaluator: evaluators) {
			expressionFactory.registerEvaluator(evaluator.getLanguageUrl(), evaluator);
		}
		return expressionFactory;
	}
	
	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	public Map<String, ExpressionEvaluator> getEvaluators() {
		return evaluators;
	}

	public Expression createExpression(ExpressionType expressionType, String shortDesc) throws ExpressionEvaluationException {
		Expression expression = new Expression(getEvaluator(getLanguage(expressionType), shortDesc), expressionType, shortDesc);
		expression.setObjectResolver(objectResolver);
		expression.setReturnType(expressionType.getReturnType());
		return expression;
	}
	
	public void registerEvaluator(String language, ExpressionEvaluator evaluator) {
		if (evaluators.containsKey(language)) {
			throw new IllegalArgumentException("Evaluator for language "+language+" already registered");
		}
		evaluators.put(language,evaluator);
	}
	
	private ExpressionEvaluator getEvaluator(String language, String shortDesc) throws ExpressionEvaluationException {
		ExpressionEvaluator evaluator = evaluators.get(language);
		if (evaluator == null) {
			throw new ExpressionEvaluationException("Language "+language+" used in expression "+shortDesc+" is not supported");
		}
		return evaluator;
	}

	private String getLanguage(ExpressionType expressionType) {
		if (expressionType.getLanguage() != null) {
			return expressionType.getLanguage();
		}
		return DEFAULT_LANGUAGE;
	}
	
}


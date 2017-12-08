/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.repo.common.expression;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.schema.util.ObjectResolver;

/**
 * This is NOT autowired evaluator. There is special need to manipulate objectResolver.
 * 
 * @author semancik
 */
public abstract class AbstractObjectResolvableExpressionEvaluatorFactory implements ExpressionEvaluatorFactory {
	
	private final ExpressionFactory expressionFactory;
	private ObjectResolver objectResolver;

	public AbstractObjectResolvableExpressionEvaluatorFactory(ExpressionFactory expressionFactory) {
		super();
		this.expressionFactory = expressionFactory;
	}
	
	protected ExpressionFactory getExpressionFactory() {
		return expressionFactory;
	}
	
	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	public LocalizationService getLocalizationService() {
		return expressionFactory.getLocalizationService();
	}

	@PostConstruct
	public void register() {
		getExpressionFactory().registerEvaluatorFactory(this);
	}


}

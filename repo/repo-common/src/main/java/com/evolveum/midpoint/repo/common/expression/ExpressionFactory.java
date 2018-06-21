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
package com.evolveum.midpoint.repo.common.expression;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.CacheRegistry;
import com.evolveum.midpoint.repo.common.Cacheable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * Factory for expressions and registry for expression evaluator factories.
 * 
 * @author semancik
 *
 */
public class ExpressionFactory implements Cacheable {

	private Map<QName,ExpressionEvaluatorFactory> evaluatorFactoriesMap = new HashMap<>();
	private ExpressionEvaluatorFactory defaultEvaluatorFactory;
	private Map<ExpressionIdentifier, Expression<?,?>> cache = new HashMap<>();
	final private PrismContext prismContext;
	private ObjectResolver objectResolver;					// using setter to allow Spring to handle circular references
	final private SecurityContextManager securityContextManager;
	private LocalizationService localizationService;
	private CacheRegistry cacheRegistry;

	public ExpressionFactory(SecurityContextManager securityContextManager, PrismContext prismContext,
			LocalizationService localizationService) {
		this.prismContext = prismContext;
		this.securityContextManager = securityContextManager;
		this.localizationService = localizationService;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public LocalizationService getLocalizationService() {
		return localizationService;
	}

	public void setCacheRegistry(CacheRegistry cacheRegistry) {
		this.cacheRegistry = cacheRegistry;
	}
	
	public CacheRegistry getCacheRegistry() {
		return cacheRegistry;
	}
	
	@PostConstruct
	public void register() {
		cacheRegistry.registerCacheableService(this);
	}
	
	public <V extends PrismValue,D extends ItemDefinition> Expression<V,D> makeExpression(ExpressionType expressionType,
			D outputDefinition, String shortDesc, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException {
		ExpressionIdentifier eid = new ExpressionIdentifier(expressionType, outputDefinition);
		Expression<V,D> expression = (Expression<V,D>) cache.get(eid);
		if (expression == null) {
			expression = createExpression(expressionType, outputDefinition, shortDesc, task, result);
			cache.put(eid, expression);
		}
		return expression;
	}

	private <V extends PrismValue,D extends ItemDefinition> Expression<V,D> createExpression(ExpressionType expressionType,
			D outputDefinition, String shortDesc, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException {
		Expression<V,D> expression = new Expression<>(expressionType, outputDefinition, objectResolver, securityContextManager, prismContext);
		expression.parse(this, shortDesc, task, result);
		return expression;
	}

	public <V extends PrismValue> ExpressionEvaluatorFactory getEvaluatorFactory(QName elementName) {
		return evaluatorFactoriesMap.get(elementName);
	}

	public void registerEvaluatorFactory(ExpressionEvaluatorFactory factory) {
		evaluatorFactoriesMap.put(factory.getElementName(), factory);
	}

	public ExpressionEvaluatorFactory getDefaultEvaluatorFactory() {
		return defaultEvaluatorFactory;
	}

	public void setDefaultEvaluatorFactory(ExpressionEvaluatorFactory defaultEvaluatorFactory) {
		this.defaultEvaluatorFactory = defaultEvaluatorFactory;
	}

	class ExpressionIdentifier {
		private ExpressionType expressionType;
		private ItemDefinition outputDefinition;

		ExpressionIdentifier(ExpressionType expressionType, ItemDefinition outputDefinition) {
			super();
			this.expressionType = expressionType;
			this.outputDefinition = outputDefinition;
		}

		public ExpressionType getExpressionType() {
			return expressionType;
		}

		public void setExpressionType(ExpressionType expressionType) {
			this.expressionType = expressionType;
		}

		public ItemDefinition getOutputDefinition() {
			return outputDefinition;
		}

		public void setOutputDefinition(ItemDefinition outputDefinition) {
			this.outputDefinition = outputDefinition;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((expressionType == null) ? 0 : expressionType.hashCode());
			result = prime * result + ((outputDefinition == null) ? 0 : outputDefinition.hashCode());
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
			ExpressionIdentifier other = (ExpressionIdentifier) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (expressionType == null) {
				if (other.expressionType != null)
					return false;
			} else if (!expressionType.equals(other.expressionType))
				return false;
			if (outputDefinition == null) {
				if (other.outputDefinition != null)
					return false;
			} else if (!outputDefinition.equals(other.outputDefinition))
				return false;
			return true;
		}

		private ExpressionFactory getOuterType() {
			return ExpressionFactory.this;
		}
	}
	
	@Override
	public void clearCache() {
		cache = new HashMap<>();
	}

}

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
package com.evolveum.midpoint.model.common.mapping;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.filter.Filter;
import com.evolveum.midpoint.common.filter.FilterManager;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.evaluator.AsIsExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.evaluator.GenerateExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.evaluator.LiteralExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.evaluator.PathExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsIsExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;

/**
 * @author Radovan Semancik
 *
 */
public class MappingFactory {

	ObjectFactory objectFactory = new ObjectFactory();
	
	private ExpressionFactory expressionFactory;
	private ObjectResolver objectResolver;
	private Protector protector;
	private PrismContext prismContext;
	private FilterManager<Filter> filterManager;
    private SecurityEnforcer securityEnforcer;
	private boolean profiling = false;
		
	public ExpressionFactory getExpressionFactory() {
		return expressionFactory;
	}

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		this.expressionFactory = expressionFactory;
	}
	
	public Protector getProtector() {
		return protector;
	}

	public void setProtector(Protector protector) {
		this.protector = protector;
	}

	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public FilterManager<Filter> getFilterManager() {
		return filterManager;
	}

	public void setFilterManager(FilterManager<Filter> filterManager) {
		this.filterManager = filterManager;
	}

    public SecurityEnforcer getSecurityEnforcer() {
        return securityEnforcer;
    }

    public void setSecurityEnforcer(SecurityEnforcer securityEnforcer) {
        this.securityEnforcer = securityEnforcer;
    }

    public boolean isProfiling() {
		return profiling;
	}

	public void setProfiling(boolean profiling) {
		this.profiling = profiling;
	}

	public <V extends PrismValue, D extends ItemDefinition> Mapping<V, D> createMapping(MappingType mappingType, String shortDesc) {
		Mapping<V,D> mapping = new Mapping<>(mappingType, shortDesc, expressionFactory, securityEnforcer);
		mapping.setFilterManager(filterManager);
		mapping.setProfiling(profiling);
		return mapping;
	}
	
}

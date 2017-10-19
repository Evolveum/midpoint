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
package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.common.filter.Filter;
import com.evolveum.midpoint.common.filter.FilterManager;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * @author Radovan Semancik
 *
 */
public class MappingFactory {

//	ObjectFactory objectFactory = new ObjectFactory();

	private ExpressionFactory expressionFactory;
	private ObjectResolver objectResolver;
	private Protector protector;						// not used for now
	private PrismContext prismContext;
	private FilterManager<Filter> filterManager;
    private SecurityContextManager securityContextManager;
	private boolean profiling = false;

	public ExpressionFactory getExpressionFactory() {
		return expressionFactory;
	}

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		this.expressionFactory = expressionFactory;
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

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public void setFilterManager(FilterManager<Filter> filterManager) {
		this.filterManager = filterManager;
	}

    public SecurityContextManager getSecurityEnforcer() {
        return securityContextManager;
    }

    public void setSecurityContextManager(SecurityContextManager securityContextManager) {
        this.securityContextManager = securityContextManager;
    }

    public boolean isProfiling() {
		return profiling;
	}

	public void setProfiling(boolean profiling) {
		this.profiling = profiling;
	}

	public <V extends PrismValue, D extends ItemDefinition> Mapping.Builder<V, D> createMappingBuilder() {
		return new Mapping.Builder<V, D>()
				.prismContext(prismContext)
				.expressionFactory(expressionFactory)
				.securityContextManager(securityContextManager)
				.variables(new ExpressionVariables())
				.filterManager(filterManager)
				.profiling(profiling);
	}

	public <V extends PrismValue, D extends ItemDefinition> Mapping.Builder<V, D> createMappingBuilder(MappingType mappingType, String shortDesc) {
		return this.<V,D>createMappingBuilder().mappingType(mappingType)
				.contextDescription(shortDesc);
	}

}

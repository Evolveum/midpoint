/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * @author Radovan Semancik
 *
 */
public class MappingFactory {

//    ObjectFactory objectFactory = new ObjectFactory();

    private ExpressionFactory expressionFactory;
    private ObjectResolver objectResolver;
    private Protector protector;                        // not used for now
    private PrismContext prismContext;
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

    public <V extends PrismValue, D extends ItemDefinition> MappingBuilder<V, D> createMappingBuilder() {
        return new MappingBuilder<V, D>()
                .prismContext(prismContext)
                .expressionFactory(expressionFactory)
                .securityContextManager(securityContextManager)
                .objectResolver(objectResolver)
                .profiling(profiling);
    }

    public <V extends PrismValue, D extends ItemDefinition> MappingBuilder<V, D> createMappingBuilder(MappingType mappingType, String shortDesc) {
        return this.<V,D>createMappingBuilder().mappingBean(mappingType)
                .contextDescription(shortDesc)
                .objectResolver(objectResolver);
    }

}

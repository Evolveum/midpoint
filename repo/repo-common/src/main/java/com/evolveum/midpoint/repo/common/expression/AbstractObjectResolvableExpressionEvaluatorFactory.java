/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;

/**
 * This is NOT autowired evaluator. There is special need to manipulate objectResolver.
 *
 * @author semancik
 */
public abstract class AbstractObjectResolvableExpressionEvaluatorFactory implements ExpressionEvaluatorFactory {

    private final ExpressionFactory expressionFactory;
    protected final CacheConfigurationManager cacheConfigurationManager;
    private ObjectResolver objectResolver;

    public AbstractObjectResolvableExpressionEvaluatorFactory(ExpressionFactory expressionFactory,
            CacheConfigurationManager cacheConfigurationManager) {
        super();
        this.expressionFactory = expressionFactory;
        this.cacheConfigurationManager = cacheConfigurationManager;
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

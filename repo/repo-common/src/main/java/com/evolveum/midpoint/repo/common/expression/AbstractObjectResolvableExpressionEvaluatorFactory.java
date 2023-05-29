/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.repo.common.ObjectResolver;

/**
 * This is NOT autowired evaluator. There is special need to manipulate objectResolver.
 *
 * @author semancik
 */
public abstract class AbstractObjectResolvableExpressionEvaluatorFactory extends BaseExpressionEvaluatorFactory {

    private final ExpressionFactory expressionFactory;
    private ObjectResolver objectResolver;

    public AbstractObjectResolvableExpressionEvaluatorFactory(ExpressionFactory expressionFactory) {
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

    @PostConstruct
    public void register() {
        expressionFactory.registerEvaluatorFactory(this);
    }
}

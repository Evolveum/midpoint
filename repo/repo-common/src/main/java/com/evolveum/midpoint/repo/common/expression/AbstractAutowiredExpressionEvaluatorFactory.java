/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.expression;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author semancik
 */
public abstract class AbstractAutowiredExpressionEvaluatorFactory extends BaseExpressionEvaluatorFactory {

    @Autowired private ExpressionFactory expressionFactory;

    @PostConstruct
    public void register() {
        expressionFactory.registerEvaluatorFactory(this);
    }
}

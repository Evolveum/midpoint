/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author semancik
 *
 */
public abstract class AbstractAutowiredExpressionEvaluatorFactory implements ExpressionEvaluatorFactory {

    @Autowired private ExpressionFactory expressionFactory;

    protected ExpressionFactory getExpressionFactory() {
        return expressionFactory;
    }

    @PostConstruct
    public void register() {
        getExpressionFactory().registerEvaluatorFactory(this);
    }

}

/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.security.api.SecurityContextManager;

import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.repo.common.ObjectResolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This is NOT autowired evaluator. There is special need to manipulate objectResolver.
 *
 * @author semancik
 */
public abstract class AbstractObjectResolvableExpressionEvaluatorFactory extends BaseExpressionEvaluatorFactory {

    @NotNull private final ExpressionFactory expressionFactory;
    private ObjectResolver objectResolver;

    public AbstractObjectResolvableExpressionEvaluatorFactory(@NotNull ExpressionFactory expressionFactory) {
        this.expressionFactory = expressionFactory;
    }

    protected @NotNull ExpressionFactory getExpressionFactory() {
        return expressionFactory;
    }

    public @NotNull ObjectResolver getObjectResolver() {
        return Objects.requireNonNull(objectResolver, "no object resolver");
    }

    public void setObjectResolver(ObjectResolver objectResolver) {
        this.objectResolver = objectResolver;
    }

    /** May be null in some low-level tests. */
    public @Nullable SecurityContextManager getSecurityContextManager() {
        return expressionFactory.getSecurityContextManager();
    }

    public @NotNull LocalizationService getLocalizationService() {
        return expressionFactory.getLocalizationService();
    }

    @PostConstruct
    public void register() {
        expressionFactory.registerEvaluatorFactory(this);
    }
}

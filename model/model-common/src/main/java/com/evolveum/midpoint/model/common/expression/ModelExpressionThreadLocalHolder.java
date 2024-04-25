/*
 * Copyright (c) 2013-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression;

import java.util.Objects;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.task.api.ExpressionEnvironment;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * Accesses {@link ModelExpressionEnvironment} via {@link ExpressionEnvironmentThreadLocalHolder}
 * (containing e.g. lens context, projection context, mapping, and task) to be used
 * from withing scripts and methods that are called from scripts.
 *
 * TODO Is the name (still) correct? The "thread local holder" is in a separate class now.
 *
 * @author Radovan Semancik
 */
public class ModelExpressionThreadLocalHolder {

    private static <F extends ObjectType, V extends PrismValue, D extends ItemDefinition<?>>
    ModelExpressionEnvironment<V, D> getModelExpressionEnvironment() {
        ExpressionEnvironment environment = ExpressionEnvironmentThreadLocalHolder.getExpressionEnvironment();
        //noinspection unchecked
        return environment instanceof ModelExpressionEnvironment ?
                (ModelExpressionEnvironment<V, D>) environment : null;
    }

    public static ModelContext<?> getLensContext() {
        ModelExpressionEnvironment<?, ?> env = getModelExpressionEnvironment();
        return env != null ? env.getLensContext() : null;
    }

    @NotNull
    public static ModelContext<?> getLensContextRequired() {
        return Objects.requireNonNull(getLensContext(), "No lens context");
    }

    public static <V extends PrismValue, D extends ItemDefinition<?>> Mapping<V,D> getMapping() {
        ModelExpressionEnvironment<?, ?> env = getModelExpressionEnvironment();
        if (env == null) {
            return null;
        }
        //noinspection unchecked
        return (Mapping<V,D>) env.getMapping();
    }

    public static ModelProjectionContext getProjectionContext() {
        ModelExpressionEnvironment<?, ?> env = getModelExpressionEnvironment();
        if (env == null) {
            return null;
        }
        return env.getProjectionContext();
    }

    public static ModelProjectionContext getProjectionContextRequired() {
        return Objects.requireNonNull(getProjectionContext(), "No projection context");
    }
}

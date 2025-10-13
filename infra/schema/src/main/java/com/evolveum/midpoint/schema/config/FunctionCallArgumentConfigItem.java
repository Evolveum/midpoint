/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionExpressionEvaluatorType;

/**
 * Represents an {@link ExpressionParameterType} that is part of a {@link FunctionExpressionEvaluatorType} i.e. a function call.
 *
 * Intentionally differs from the standard naming convention. The embedded value is used as an argument value specification.
 *
 * TODO reconsider the necessity
 */
public class FunctionCallArgumentConfigItem extends ExpressionParameterConfigItem {

    @SuppressWarnings("unused") // called dynamically
    public FunctionCallArgumentConfigItem(@NotNull ConfigurationItem<ExpressionParameterType> original) {
        super(original);
    }

    private FunctionCallArgumentConfigItem(@NotNull ExpressionParameterType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static FunctionCallArgumentConfigItem of(
            @NotNull ExpressionParameterType bean, @NotNull ConfigurationItemOrigin origin) {
        return new FunctionCallArgumentConfigItem(bean, origin);
    }
}

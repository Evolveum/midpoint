/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionParameterType;

public class ExpressionParameterConfigItem
        extends ConfigurationItem<ExpressionParameterType> {

    @SuppressWarnings("unused") // called dynamically
    public ExpressionParameterConfigItem(@NotNull ConfigurationItem<ExpressionParameterType> original) {
        super(original);
    }

    protected ExpressionParameterConfigItem(@NotNull ExpressionParameterType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static ExpressionParameterConfigItem of(@NotNull ExpressionParameterType bean, @NotNull ConfigurationItemOrigin origin) {
        return new ExpressionParameterConfigItem(bean, origin);
    }

    public @Nullable ExpressionConfigItem getExpression() {
        ExpressionType bean = value().getExpression();
        return bean != null ? ExpressionConfigItem.of(bean, origin()) : null;
    }

    public @NotNull String getName() throws ConfigurationException {
        return MiscUtil.configNonNull(
                value().getName(),
                "No name in %s", fullDescription());
    }

    public @NotNull QName getType() throws ConfigurationException {
        return MiscUtil.configNonNull(value().getType(), () -> "No type of " + fullDescription());
    }
}

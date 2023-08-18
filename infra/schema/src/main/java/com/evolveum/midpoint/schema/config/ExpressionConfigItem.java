/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionPrivilegesSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExpressionConfigItem
        extends ConfigurationItem<ExpressionType>
        implements PrivilegesMixin<ExpressionType> {

    @SuppressWarnings("unused") // called dynamically
    public ExpressionConfigItem(@NotNull ConfigurationItem<ExpressionType> original) {
        super(original);
    }

    protected ExpressionConfigItem(@NotNull ExpressionType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static ExpressionConfigItem embedded(@NotNull ExpressionType bean) {
        return of(bean, ConfigurationItemOrigin.embedded(bean));
    }

    public static ExpressionConfigItem of(@NotNull ExpressionType bean, @NotNull ConfigurationItemOrigin origin) {
        return new ExpressionConfigItem(bean, origin);
    }

    public static ExpressionConfigItem of(
            @NotNull ExpressionType bean,
            @NotNull OriginProvider<? super ExpressionType> originProvider) {
        return new ExpressionConfigItem(bean, originProvider.origin(bean));
    }

    public @Nullable ExecutionPrivilegesSpecificationType getPrivileges() throws ConfigurationException {
        return getPrivileges(
                value().getRunAsRef(),
                value().getPrivileges());
    }

    public boolean isAllowEmptyValues() {
        return Boolean.TRUE.equals(value().isAllowEmptyValues());
    }

    public boolean isTrace() {
        return Boolean.TRUE.equals(value().isTrace());
    }
}

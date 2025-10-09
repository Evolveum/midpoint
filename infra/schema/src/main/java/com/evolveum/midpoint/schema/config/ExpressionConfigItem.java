/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
        super(value, origin, null); // provide parent in the future
    }

    public static ExpressionConfigItem of(@NotNull ExpressionType bean, @NotNull ConfigurationItemOrigin origin) {
        return new ExpressionConfigItem(bean, origin);
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

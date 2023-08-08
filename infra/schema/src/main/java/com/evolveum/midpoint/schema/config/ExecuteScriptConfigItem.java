/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.schema.util.ExecuteScriptUtil;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ValueListType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

public class ExecuteScriptConfigItem
        extends ConfigurationItem<ExecuteScriptType> {

    @SuppressWarnings("unused") // called dynamically
    public ExecuteScriptConfigItem(@NotNull ConfigurationItem<ExecuteScriptType> original) {
        super(original);
    }

    protected ExecuteScriptConfigItem(@NotNull ExecuteScriptType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static ExecuteScriptConfigItem embedded(@NotNull ExecuteScriptType bean) {
        return of(bean, ConfigurationItemOrigin.embedded(bean));
    }

    public static ExecuteScriptConfigItem of(@NotNull ExecuteScriptType bean, @NotNull ConfigurationItemOrigin origin) {
        return new ExecuteScriptConfigItem(bean, origin);
    }

    public static ExecuteScriptConfigItem of(
            @NotNull ExecuteScriptType bean,
            @NotNull OriginProvider<? super ExecuteScriptType> originProvider) {
        return new ExecuteScriptConfigItem(bean, originProvider.origin(bean));
    }

    @Override
    public ExecuteScriptConfigItem clone() {
        return new ExecuteScriptConfigItem(super.clone());
    }

    public @NotNull ExecuteScriptConfigItem implantInput(ValueListType input) {
        return ExecuteScriptConfigItem.of(
                ExecuteScriptUtil.implantInput(value(), input),
                origin());
    }
}

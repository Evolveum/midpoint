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

import org.jetbrains.annotations.Nullable;

public class ExecuteScriptConfigItem
        extends ConfigurationItem<ExecuteScriptType> {

    @SuppressWarnings("unused") // called dynamically
    public ExecuteScriptConfigItem(@NotNull ConfigurationItem<ExecuteScriptType> original) {
        super(original);
    }

    protected ExecuteScriptConfigItem(
            @NotNull ExecuteScriptType value, @NotNull ConfigurationItemOrigin origin, @Nullable ConfigurationItem<?> parent) {
        super(value, origin, parent);
    }

    public static ExecuteScriptConfigItem of(@NotNull ExecuteScriptType bean, @NotNull ConfigurationItemOrigin origin) {
        return new ExecuteScriptConfigItem(bean, origin, null); // hopefully the path is enough
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

    @Override
    public @NotNull String localDescription() {
        return "script execution request (ExecuteScriptType)";
    }
}

/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionPrivilegesSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExecutionPolicyActionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ScriptExecutionPolicyActionConfigItem
        extends PolicyActionConfigItem<ScriptExecutionPolicyActionType>
        implements PrivilegesMixin<ScriptExecutionPolicyActionType> {

    @SuppressWarnings("unused") // called dynamically
    public ScriptExecutionPolicyActionConfigItem(@NotNull ConfigurationItem<ScriptExecutionPolicyActionType> original) {
        super(original);
    }

    private ScriptExecutionPolicyActionConfigItem(@NotNull ScriptExecutionPolicyActionType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static ScriptExecutionPolicyActionConfigItem embedded(@NotNull ScriptExecutionPolicyActionType bean) {
        return of(bean, ConfigurationItemOrigin.embedded(bean));
    }

    public static ScriptExecutionPolicyActionConfigItem of(@NotNull ScriptExecutionPolicyActionType bean, @NotNull ConfigurationItemOrigin origin) {
        return new ScriptExecutionPolicyActionConfigItem(bean, origin);
    }

    public static ScriptExecutionPolicyActionConfigItem of(
            @NotNull ScriptExecutionPolicyActionType bean,
            @NotNull OriginProvider<? super ScriptExecutionPolicyActionType> originProvider) {
        return new ScriptExecutionPolicyActionConfigItem(bean, originProvider.origin(bean));
    }

    public @Nullable ExecutionPrivilegesSpecificationType getPrivileges() throws ConfigurationException {
        return getPrivileges(
                value().getRunAsRef(),
                value().getPrivileges());
    }

    public @NotNull List<ExecuteScriptConfigItem> getExecuteScriptConfigItems() {
        return childrenPlain(
                value().getExecuteScript(),
                ExecuteScriptConfigItem.class,
                ScriptExecutionPolicyActionType.F_EXECUTE_SCRIPT);
    }
}

/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.schema.config.PolicyActionConfigItem;
import com.evolveum.midpoint.schema.config.ScriptExecutionPolicyActionConfigItem;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExecutionPolicyActionType;

import org.jetbrains.annotations.NotNull;

/**
 * Context of execution of specific "script execution" policy action.
 */
class ActionContext {

    @NotNull final ScriptExecutionPolicyActionConfigItem actionCI;
    @NotNull final ScriptExecutionPolicyActionType action;
    @NotNull final EvaluatedPolicyRuleImpl rule;
    @NotNull final LensContext<?> context;
    @NotNull final LensFocusContext<?> focusContext;
    @NotNull final Task task;
    @NotNull final PolicyRuleScriptExecutor beans;

    ActionContext(
            @NotNull PolicyActionConfigItem<ScriptExecutionPolicyActionType> action,
            @NotNull EvaluatedPolicyRuleImpl rule,
            @NotNull LensContext<?> context,
            @NotNull Task task,
            @NotNull PolicyRuleScriptExecutor beans) {
        this.actionCI = action.as(ScriptExecutionPolicyActionConfigItem.class);
        this.action = actionCI.value();
        this.rule = rule;
        this.context = context;
        this.focusContext = context.getFocusContextRequired();
        this.task = task;
        this.beans = beans;
    }

    void putIntoVariables(VariablesMap variables) {
        variables.put(ExpressionConstants.VAR_POLICY_ACTION, action, ScriptExecutionPolicyActionType.class);
        variables.put(ExpressionConstants.VAR_POLICY_RULE, rule, EvaluatedPolicyRule.class);
    }
}

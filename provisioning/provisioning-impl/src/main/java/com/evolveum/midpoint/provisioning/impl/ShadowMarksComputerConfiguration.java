/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.provisioning.impl.shadows.RepoShadowWithState.ShadowState;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.stream.Collectors;

import com.evolveum.midpoint.repo.common.ObjectMarkHelper.MarkPresence;
import com.evolveum.midpoint.repo.common.ObjectMarkHelper.ObjectMarksComputer;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.ShadowMarkingRules;
import com.evolveum.midpoint.schema.processor.ShadowMarkingRules.FilterExpressionEvaluator;
import com.evolveum.midpoint.schema.processor.ShadowMarkingRules.MarkingRule;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkingRuleSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/** Wraps prepared marking rules, ready to be used for shadow marking. */
public class ShadowMarksComputerConfiguration {

    /** Rules with expressions evaluated. */
    @NotNull private final ShadowMarkingRules rules;

    private ShadowMarksComputerConfiguration(@NotNull ShadowMarkingRules rules) {
        this.rules = rules;
    }

    public static ShadowMarksComputerConfiguration create(ProvisioningContext ctx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        FilterExpressionEvaluator evaluator = (rawFilter, lResult) -> {
            var systemConfiguration = CommonBeans.get().systemObjectCache.getSystemConfiguration(lResult);
            var variables = new VariablesMap();
            variables.put(ExpressionConstants.VAR_RESOURCE, ctx.getResource(), ResourceType.class);
            variables.put(ExpressionConstants.VAR_CONFIGURATION, systemConfiguration, SystemConfigurationType.class);
            return ExpressionUtil.evaluateFilterExpressions(
                    rawFilter,
                    variables,
                    MiscSchemaUtil.getExpressionProfile(), // TODO
                    CommonBeans.get().expressionFactory,
                    "protected filter",
                    ctx.getTask(),
                    lResult);
        };
        var rulesWithEvaluatedFilterExpressions = ctx.getObjectDefinitionRequired()
                .getShadowMarkingRules()
                .evaluateExpressions(evaluator, result);
        return new ShadowMarksComputerConfiguration(rulesWithEvaluatedFilterExpressions);
    }

    ObjectMarksComputer computerFor(@NotNull AbstractShadow shadow, @NotNull ShadowState shadowState) {

        return new ObjectMarksComputer() {

            @Override
            public MarkPresence computeObjectMarkPresence(@NotNull String markOid, @NotNull OperationResult result)
                    throws SchemaException {
                stateCheck(rules.areExpressionsEvaluated(), "expressions are not evaluated");
                var rule = rules.getMarkingRulesMap().get(markOid);
                if (rule != null && rule.matches(shadow)) {
                    return MarkPresence.of(
                            markOid,
                            new MarkingRuleSpecificationType()
                                    .ruleId(rule.getRuleId())
                                    .transitional(rule.isTransitional()));
                } else {
                    return null;
                }
            }

            @Override
            public @NotNull Collection<String> getComputableMarksOids() {
                return rules.getMarkingRulesMap().entrySet().stream()
                        .filter(e -> isApplicableInState(e.getValue(), shadowState))
                        .map(e -> e.getKey())
                        .collect(Collectors.toUnmodifiableSet());
            }

            private boolean isApplicableInState(@NotNull MarkingRule rule, @NotNull ShadowState shadowState) {
                return switch (rule.getApplicationTime()) {
                    case ALWAYS -> true;
                    case CLASSIFICATION -> shadowState.isClassified();
                };
            }
        };
    }
}

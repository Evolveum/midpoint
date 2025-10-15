package com.evolveum.midpoint.smart.impl.scoring;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.BulkActionsProfile;
import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;
import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorsProfile;
import com.evolveum.midpoint.schema.expression.ExpressionPermissionProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.FunctionLibrariesProfile;
import com.evolveum.midpoint.schema.expression.ScriptLanguageExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.mappings.ValuesPair;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class MappingsQualityAssessor {
    private final ExpressionFactory expressionFactory;

    public MappingsQualityAssessor(ExpressionFactory expressionFactory) {
        this.expressionFactory = expressionFactory;
    }


    /**
     * Assesses mapping quality by comparing shadow values to focus values (after expression application).
     * Returns the fraction of shadow values that matched any focus value.
     */
    public float assessMappingsQuality(Collection<ValuesPair> valuePairs, ExpressionType suggestedExpression, Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        int totalShadowValues = 0;
        int matchedShadowValues = 0;

        for (final ValuesPair valuePair : valuePairs) {
            final Collection<?> shadowValues = valuePair.shadowValues();
            final Collection<?> focusValues = valuePair.focusValues();

            // Skip if shadow or focus is multivalued
            if (shadowValues == null || focusValues == null ||
                    shadowValues.size() != 1 || focusValues.size() != 1) {
                continue;
            }

            final String shadowValue = shadowValues.iterator().next().toString();
            String focusValue = focusValues.iterator().next().toString();

            if (suggestedExpression != null) {
                focusValue = applyExpression(suggestedExpression, focusValue, task, parentResult);
            }

            totalShadowValues++;
            if (shadowValue.equals(focusValue)) {
                matchedShadowValues++;
            }
        }

        if (totalShadowValues == 0) {
            return -1.0f;
        }
        final float quality = (float) matchedShadowValues / totalShadowValues;
        return Math.round(quality * 100.0f) / 100.0f;
    }

    @Nullable
    private String applyExpression(ExpressionType expressionType, String input, Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        final String description = "Inbound mapping expression";
        final VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_INPUT, input, String.class);
        final ExpressionProfile profile = restrictedProfile();
        final Collection<String> transformedInput = ExpressionUtil.evaluateStringExpression(variables, expressionType,
                profile, this.expressionFactory, description, task, parentResult);

        if (transformedInput == null || transformedInput.isEmpty()) {
            return null;
        }
        return transformedInput.iterator().next();
    }

    private static ExpressionProfile restrictedProfile() {
        // Permission profile (allow-list):
        // - Default decision: ALLOW (safe baseline; explicit denies still win).
        // - Explicitly allow java.lang.String (basic safe transformations).
        // - Explicitly deny a placeholder 'execute' method (defensive guardrail).
        final ExpressionPermissionProfile permissionsProfile = ExpressionPermissionProfile.closed(
                "LLM scripts profile", AccessDecision.ALLOW, Collections.emptyList(),
                List.of(new ExpressionPermissionClassProfileType()
                        .decision(AuthorizationDecisionType.ALLOW)
                        .name("java.lang.String")
                        .method(new ExpressionPermissionMethodProfileType()
                                .name("execute")
                                .decision(AuthorizationDecisionType.DENY))));
        // Script evaluator profile:
        // - Enable script evaluator: ALLOW.
        // - Only Groovy is enabled and bound to the above permission profile.
        // - Type checking enabled to enforce class/method restrictions at runtime.
        final ExpressionEvaluatorProfile evaluatorProfile = new ExpressionEvaluatorProfile(
                SchemaConstantsGenerated.C_SCRIPT, AccessDecision.ALLOW,
                List.of(new ScriptLanguageExpressionProfile("groovy", AccessDecision.ALLOW, true, permissionsProfile)));
        // Expression profile assembly:
        // - Evaluators default: ALLOW (so script evaluator is usable).
        // - Only Groovy is configured; other languages remain unavailable by omission.
        // - Bulk actions: none; Function libraries: none; Privilege elevation: DENY.
        return new ExpressionProfile("LLM scripts profile",
                new ExpressionEvaluatorsProfile(AccessDecision.ALLOW, List.of(evaluatorProfile)),
                BulkActionsProfile.none(), FunctionLibrariesProfile.none(), AccessDecision.DENY);
    }

}

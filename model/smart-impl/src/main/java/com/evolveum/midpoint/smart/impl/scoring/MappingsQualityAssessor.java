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

    private static final String GROOVY_LANGUAGE =
            "http://midpoint.evolveum.com/xml/ns/public/expression/language#Groovy";
    private final ExpressionFactory expressionFactory;

    public MappingsQualityAssessor(ExpressionFactory expressionFactory) {
        this.expressionFactory = expressionFactory;
    }


    /**
     * Assesses mapping quality by comparing shadow values to focus values.
     *
     * The quality is computed as the fraction of sampled shadow values that equal the (optionally
     * transformed) focus value. Only pairs where both sides are single-valued are considered; multi-valued
     * pairs are skipped.
     *
     * @param valuePairs Sampled value pairs (shadow vs focus) used to estimate the mapping quality.
     * @param suggestedExpression Optional expression (i.e. Groovy script) to transform the focus value
     *                            before comparison. If {@code null}, raw focus values are used.
     *
     * @return Quality in the range {@code [0.0, 1.0]} rounded to two decimals;
     *         returns {@code -1.0f} if expression evaluation failed.
     *         returns {@code -2.0f} if no comparable samples were found.
     *
     */
    public AssessmentResult assessMappingsQualityDetailed(
            Collection<ValuesPair> valuePairs,
            @Nullable ExpressionType suggestedExpression,
            Task task,
            OperationResult parentResult) {
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
                try {
                    focusValue = applyExpression(suggestedExpression, focusValue, task, parentResult);
                } catch (Exception e) {
                    return AssessmentResult.evalFailed("Failed to evaluate suggested expression: " + e.getMessage());
                }
            }

            totalShadowValues++;
            if (shadowValue.equals(focusValue)) {
                matchedShadowValues++;
            }
        }

        if (totalShadowValues == 0) {
            return AssessmentResult.noSamples();
        }
        final float quality = Math.round(((float) matchedShadowValues / totalShadowValues) * 100.0f) / 100.0f;
        return AssessmentResult.ok(quality);
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
        // Permission profile for Groovy language (allow-list):
        // - Default decision: ALLOW.
        // - Explicitly deny a placeholder String's 'execute' method.
        final ExpressionPermissionProfile permissionsProfile = ExpressionPermissionProfile.closed(
                "LLM scripts profile", AccessDecision.ALLOW, Collections.emptyList(),
                List.of(new ExpressionPermissionClassProfileType()
                        .decision(AuthorizationDecisionType.ALLOW)
                        .name("java.lang.String")
                        .method(new ExpressionPermissionMethodProfileType()
                                .name("execute")
                                .decision(AuthorizationDecisionType.DENY))));
        // Script evaluator profile:
        // - Default script evaluator decision: DENY (only explicitly listed script type is usable).
        // - Only Groovy is explicitly enabled.
        final ExpressionEvaluatorProfile evaluatorProfile = new ExpressionEvaluatorProfile(
                SchemaConstantsGenerated.C_SCRIPT, AccessDecision.DENY,
                List.of(new ScriptLanguageExpressionProfile(GROOVY_LANGUAGE, AccessDecision.ALLOW, true,
                        permissionsProfile)));
        // Expression profile assembly:
        // - Evaluators default: DENY (only explicitly listed script evaluator is usable).
        // - Only script evaluator is configured; other evaluators remain unavailable by omission.
        return new ExpressionProfile("LLM scripts profile",
                new ExpressionEvaluatorsProfile(AccessDecision.DENY, List.of(evaluatorProfile)),
                BulkActionsProfile.none(), FunctionLibrariesProfile.none(), AccessDecision.DENY);
    }

    public static class AssessmentResult {
        public final float quality;
        public final AssessmentStatus status;
        public final @Nullable String errorLog;

        private AssessmentResult(float quality, AssessmentStatus status, @Nullable String errorLog) {
            this.quality = quality;
            this.status = status;
            this.errorLog = errorLog;
        }

        public static AssessmentResult ok(float quality) {
            return new AssessmentResult(quality, AssessmentStatus.OK, null);
        }

        public static AssessmentResult evalFailed(String errorLog) {
            return new AssessmentResult(-1.0f, AssessmentStatus.EVAL_FAILED, errorLog);
        }

        public static AssessmentResult noSamples() {
            return new AssessmentResult(-2.0f, AssessmentStatus.NO_SAMPLES,
                    "No comparable samples were found for assessing mapping quality");
        }
    }

    public enum AssessmentStatus { OK, NO_SAMPLES, EVAL_FAILED }

}

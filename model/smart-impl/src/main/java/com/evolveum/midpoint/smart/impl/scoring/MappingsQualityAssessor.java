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
import com.evolveum.midpoint.smart.impl.mappings.MappingDirection;
import com.evolveum.midpoint.smart.impl.mappings.ValuesPair;
import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
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
     * Assesses mapping quality by comparing the source side to the target side.
     * Direction semantics:
     * - Inbound (inboundMapping = true): source = shadow, target = focus (shadow -> focus)
     * - Outbound (inboundMapping = false): source = focus, target = shadow (focus -> shadow)
     * The quality is computed as the fraction of sampled single-valued pairs where the (optionally transformed)
     * source equals the target. Multi-valued pairs are skipped.
     */
    public AssessmentResult assessMappingsQuality(
            ValuesPairSample<?, ?> testingSample,
            @Nullable ExpressionType suggestedExpression,
            Task task,
            OperationResult parentResult) throws ExpressionEvaluationException, SecurityViolationException {
        int totalSamples = 0;
        int matchedSamples = 0;
        boolean inboundMapping = testingSample.direction() == MappingDirection.INBOUND;

        for (final ValuesPair<?, ?> valuePair : testingSample.pairs()) {
            final Collection<?> shadowValues = valuePair.shadowValues();
            final Collection<?> focusValues = valuePair.focusValues();

            // Skip if shadow or focus is multivalued
            if (shadowValues == null || focusValues == null ||
                    shadowValues.size() != 1 || focusValues.size() != 1) {
                continue;
            }

            final String rawShadow = shadowValues.iterator().next().toString();
            final String rawFocus = focusValues.iterator().next().toString();

            final String sourceValue = inboundMapping ? rawShadow : rawFocus;
            final String target = inboundMapping ? rawFocus : rawShadow;
            final String variableName = inboundMapping
                    ? ExpressionConstants.VAR_INPUT
                    : testingSample.focusPropertyPath().lastName().getLocalPart();

            final String transformedSource = suggestedExpression == null
                    ? sourceValue
                    : applyExpression(suggestedExpression, sourceValue, variableName, task, parentResult);
            if (target.equals(transformedSource)) {
                matchedSamples++;
            }

            totalSamples++;
        }

        if (totalSamples == 0) {
            return AssessmentResult.noSamples();
        }
        final float quality = Math.round(((float) matchedSamples / totalSamples) * 100.0f) / 100.0f;
        return AssessmentResult.ok(quality);
    }

    @Nullable
    private String applyExpression(ExpressionType expressionType, @Nullable String input, String variableName,
            Task task, OperationResult parentResult)
            throws ExpressionEvaluationException, SecurityViolationException {
        final String description = "Mapping expression";
        final VariablesMap variables = new VariablesMap();
        variables.put(variableName, input, String.class);
        final ExpressionProfile profile = restrictedProfile();
        try {
            final Collection<String> transformedInput = ExpressionUtil.evaluateStringExpression(variables,
                    expressionType, profile, this.expressionFactory, description, task, parentResult);

            if (transformedInput == null || transformedInput.isEmpty()) {
                return null;
            }
            return transformedInput.iterator().next();
        } catch (SchemaException | CommunicationException | ConfigurationException | ObjectNotFoundException e) {
            throw new MappingEvaluationException("Failed to evaluate suggested expression.", e);
        }
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

    public record AssessmentResult(float quality, AssessmentStatus status, @Nullable String errorLog) {
        public static AssessmentResult ok(float quality) {
            return new AssessmentResult(quality, AssessmentStatus.OK, null);
        }

        public static AssessmentResult noSamples() {
            return new AssessmentResult(0.0f, AssessmentStatus.NO_SAMPLES,
                    "No comparable samples were found for assessing mapping quality");
        }
    }

    public enum AssessmentStatus { OK, NO_SAMPLES }

    public static class MappingEvaluationException extends RuntimeException {
        public MappingEvaluationException(String message, Exception cause) {
            super(message, cause);
        }
    }

}

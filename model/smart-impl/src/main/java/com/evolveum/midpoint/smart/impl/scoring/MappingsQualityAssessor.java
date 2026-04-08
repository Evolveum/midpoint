package com.evolveum.midpoint.smart.impl.scoring;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.constants.ExpressionConstants;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

@Component
public class MappingsQualityAssessor {

    private final MappingScriptValidator scriptValidator;

    public MappingsQualityAssessor(MappingScriptValidator scriptValidator) {
        this.scriptValidator = scriptValidator;
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
        try {
            final Collection<String> transformedInput = scriptValidator.evaluateExpression(
                    expressionType, variableName, input, task, parentResult);

            if (transformedInput == null || transformedInput.isEmpty()) {
                return null;
            }
            return transformedInput.iterator().next();
        } catch (SchemaException | CommunicationException | ConfigurationException | ObjectNotFoundException e) {
            throw new MappingEvaluationException("Failed to evaluate suggested expression.", e);
        }
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

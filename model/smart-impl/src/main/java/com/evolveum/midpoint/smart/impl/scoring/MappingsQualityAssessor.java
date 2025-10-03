package com.evolveum.midpoint.smart.impl.scoring;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.evolveum.midpoint.smart.impl.mappings.ValuesPair;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class MappingsQualityAssessor {

    /**
     * Assesses mapping quality by comparing shadow values to focus values (after expression application).
     * Returns the fraction of shadow values that matched any focus value.
     */
    public float assessMappingsQuality(Collection<ValuesPair> valuePairs, ExpressionType suggestedExpression) {
        int totalShadowValues = 0;
        int matchedShadowValues = 0;

        for (final ValuesPair valuePair : valuePairs) {
            Collection<?> shadowValues = valuePair.shadowValues();
            Collection<?> focusValues = valuePair.focusValues();

            // Skip if shadow or focus is multivalued
            if (shadowValues == null || focusValues == null ||
                    shadowValues.size() != 1 || focusValues.size() != 1) {
                continue;
            }

            String shadowValue = shadowValues.iterator().next().toString();
            String focusValue = focusValues.iterator().next().toString();

            String result = applyExpression(suggestedExpression, focusValue);

            totalShadowValues++;
            if (shadowValue.equals(result)) {
                matchedShadowValues++;
            }
        }

        if (totalShadowValues == 0) {
            return -1.0f;
        }
        float quality = (float) matchedShadowValues / totalShadowValues;
        return Math.round(quality * 100.0f) / 100.0f;
    }

    private String applyExpression(ExpressionType expressionType, String input) {
        return "";
    }
}

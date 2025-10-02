package com.evolveum.midpoint.smart.impl.scoring;

import java.util.Collection;

import com.evolveum.midpoint.smart.impl.mappings.ValuesPair;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class MappingsQualityAssessor {
    public float assessMappingsQuality(Collection<ValuesPair> valuePairs, ExpressionType suggestedExpression) {
        for (final ValuesPair valuePair : valuePairs) {
            // TODO
            return 0;
        }
        return 0;
    }

    private String applyExpression(ExpressionType expressionType, String input) {
        return "";
    }
}
